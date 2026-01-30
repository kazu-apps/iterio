package com.iterio.app.service.audio

import java.util.Random
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Ocean waves generator using a state machine approach:
 *
 * - Asymmetric envelope: fast rise (1.5-2s) + slow decay (4-6s), 7-12s per wave (randomized)
 * - Time-varying LPF: 500->6000Hz on rise (bright crash), 6000->500Hz on decay (muffled recede)
 * - Foam/spray layer: 3-10kHz noise activated during rise and early decay,
 *   with 20-40Hz fine amplitude modulation
 * - Stereo movement: each wave slowly pans L->R or R->L
 * - Set waves: 15% chance of a larger wave
 */
class WavesGenerator(private val sampleRate: Int) : AudioGenerator {
    private val random = Random()
    @Volatile private var volume: Float = 0.5f
    private var sampleIndex: Long = 0

    // Wave state machine
    private var wavePhase: Float = 0f      // 0.0 = start, 1.0 = end of wave
    private var waveDuration: Int = 0       // total wave duration in samples
    private var waveRiseDuration: Int = 0   // rise portion in samples
    private var waveDecayDuration: Int = 0  // decay portion in samples
    private var waveSamplePos: Int = 0
    private var waveAmplitude: Float = 1f   // set wave = larger
    private var wavePanDirection: Float = 1f  // +1 = L->R, -1 = R->L
    private var wavePanStart: Float = 0.3f

    // Noise source for wave body
    private var brownSample: Float = 0f
    private val leakFactor = 0.998f
    private val step = 0.025f

    // Time-varying LPF for wave body
    private val waveLpfL = BiquadFilter(sampleRate, BiquadFilter.FilterType.LOWPASS, 500f, 0.7f)
    private val waveLpfR = BiquadFilter(sampleRate, BiquadFilter.FilterType.LOWPASS, 500f, 0.7f)

    // Foam/spray layer
    private val foamBpfL = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 6000f, 0.5f)
    private val foamBpfR = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 7000f, 0.5f)

    init {
        initNewWave()
    }

    private fun initNewWave() {
        val riseSec = 1.5f + random.nextFloat() * 0.5f   // 1.5-2s
        val decaySec = 4f + random.nextFloat() * 2f       // 4-6s
        waveRiseDuration = (riseSec * sampleRate).toInt()
        waveDecayDuration = (decaySec * sampleRate).toInt()
        waveDuration = waveRiseDuration + waveDecayDuration
        waveSamplePos = 0
        wavePhase = 0f

        // 15% chance of set wave (larger)
        waveAmplitude = if (random.nextFloat() < 0.15f) 1.4f else 1f

        // Alternate pan direction with some randomness
        wavePanDirection = if (random.nextBoolean()) 1f else -1f
        wavePanStart = 0.3f + random.nextFloat() * 0.1f

        waveLpfL.setCutoff(500f, 0.7f)
        waveLpfR.setCutoff(500f, 0.7f)
    }

    override fun generate(buffer: ShortArray): Int {
        val amp = volume * 0.4f * Short.MAX_VALUE
        val frames = buffer.size / 2

        for (i in 0 until frames) {
            // Update wave phase
            wavePhase = waveSamplePos.toFloat() / waveDuration
            val inRise = waveSamplePos < waveRiseDuration
            val risePhase = if (inRise) waveSamplePos.toFloat() / waveRiseDuration else 1f
            val decayPhase = if (!inRise) (waveSamplePos - waveRiseDuration).toFloat() / waveDecayDuration else 0f

            // Asymmetric envelope
            val envelope = if (inRise) {
                // Fast concave rise
                risePhase.pow(0.6f)
            } else {
                // Slow convex decay
                (1f - decayPhase).pow(1.5f)
            }

            // Time-varying LPF cutoff
            val lpfCutoff = if (inRise) {
                lerp(500f, 6000f, risePhase)
            } else {
                lerp(6000f, 500f, decayPhase)
            }
            waveLpfL.setCutoff(lpfCutoff, 0.7f)
            waveLpfR.setCutoff(lpfCutoff, 0.7f)

            // Brown noise for wave body
            brownSample = brownSample * leakFactor + (random.nextFloat() * 2f - 1f) * step
            brownSample = brownSample.coerceIn(-1f, 1f)

            val bodyL = waveLpfL.process(brownSample + (random.nextFloat() - 0.5f) * 0.02f)
            val bodyR = waveLpfR.process(brownSample + (random.nextFloat() - 0.5f) * 0.02f)

            // Foam/spray layer - active during rise and early decay
            val foamEnv = if (inRise) {
                risePhase
            } else if (decayPhase < 0.3f) {
                1f - decayPhase / 0.3f
            } else {
                0f
            }
            val foamNoise = random.nextFloat() * 2f - 1f
            // 20-40Hz micro-modulation on foam
            val foamModFreq = 20f + random.nextFloat() * 20f
            val foamMod = 0.7f + 0.3f * sin(
                2.0 * Math.PI * foamModFreq * sampleIndex / sampleRate
            ).toFloat()
            val foamL = foamBpfL.process(foamNoise) * foamEnv * foamMod * 0.25f
            val foamR = foamBpfR.process(foamNoise + (random.nextFloat() - 0.5f) * 0.1f) * foamEnv * foamMod * 0.25f

            // Stereo pan movement
            val panPos = wavePanStart + wavePanDirection * wavePhase * 0.4f
            val panL = (1f - panPos).coerceIn(0.2f, 0.8f)
            val panR = panPos.coerceIn(0.2f, 0.8f)

            // Mix
            val totalEnv = envelope * waveAmplitude
            val mixL = softClip((bodyL * 0.7f + foamL) * totalEnv * panL)
            val mixR = softClip((bodyR * 0.7f + foamR) * totalEnv * panR)

            buffer[i * 2] = (mixL * amp).toInt().coerceIn(
                Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
            ).toShort()
            buffer[i * 2 + 1] = (mixR * amp).toInt().coerceIn(
                Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
            ).toShort()

            waveSamplePos++
            sampleIndex++

            if (waveSamplePos >= waveDuration) {
                initNewWave()
            }
        }
        return buffer.size
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }
}
