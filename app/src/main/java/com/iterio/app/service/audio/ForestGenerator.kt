package com.iterio.app.service.audio

import java.util.Random
import kotlin.math.exp
import kotlin.math.sin

/**
 * Forest ambience generator with 4 layers:
 *
 * Layer 1 - Wind: pink noise -> swept bandpass (1-3kHz, 10-15s cycle),
 *           L/R center frequencies offset by 5-10%
 * Layer 2 - Bird calls: FM synthesis pseudo-birdsong, 3 voice variants (2-5kHz),
 *           2-6 calls per 10s, each panned randomly
 * Layer 3 - Leaf rustling: 10-30ms high-frequency bursts (4-12kHz),
 *           irregular intervals, very low volume
 * Layer 4 - Stream: very low level high-frequency noise (3-8kHz),
 *           4-6s amplitude modulation
 */
class ForestGenerator(private val sampleRate: Int) : AudioGenerator {
    private val random = Random()
    @Volatile private var volume: Float = 0.5f
    private var sampleIndex: Long = 0

    // --- Layer 1: Wind (swept bandpass pink noise) ---
    private val numRows = 16
    private val rows = FloatArray(numRows) { random.nextFloat() * 2f - 1f }
    private var runningSum: Float = rows.sum()
    private var pinkCounter: Int = 0
    private val windBpfL = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 2000f, 0.6f)
    private val windBpfR = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 2100f, 0.6f)
    private val windSweepPeriod = sampleRate * 12.0  // 12s sweep

    // --- Layer 2: Bird calls (FM synthesis) ---
    private data class BirdVoice(
        val baseFreq: Float,    // carrier frequency
        val modRatio: Float,    // modulator / carrier ratio
        val modIndex: Float,    // FM modulation index
        val duration: Int       // call duration in samples
    )

    private val birdVariants = listOf(
        BirdVoice(3200f, 1.5f, 2.0f, (sampleRate * 0.15).toInt()),   // short chirp
        BirdVoice(2400f, 2.0f, 3.0f, (sampleRate * 0.3).toInt()),    // warble
        BirdVoice(4200f, 1.2f, 1.5f, (sampleRate * 0.08).toInt())    // pip
    )

    private var birdActive = false
    private var birdSamplePos = 0
    private var birdCurrentVoice: BirdVoice = birdVariants[0]
    private var birdPan = 0.5f
    private var birdCountdown = nextBirdInterval()
    private var birdCallsInBurst = 0
    private var birdBurstRemaining = 0
    private var birdBurstGap = 0

    private fun nextBirdInterval(): Int {
        return sampleRate * (2 + random.nextInt(4))  // 2-5 seconds between bursts
    }

    // --- Layer 3: Leaf rustling ---
    private var leafActive = false
    private var leafDecay = 0f
    private val leafDecayRate = exp(-1.0 / (sampleRate * 0.015)).toFloat()  // ~15ms decay
    private var leafCountdown = nextLeafInterval()
    private val leafBpf = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 7000f, 1.0f)
    private var leafPan = 0.5f

    private fun nextLeafInterval(): Int {
        return (sampleRate * (0.1 + random.nextDouble() * 0.5)).toInt()  // 0.1-0.6s
    }

    // --- Layer 4: Stream ---
    private val streamBpfL = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 5000f, 0.5f)
    private val streamBpfR = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 5500f, 0.5f)
    private val streamModPeriod = sampleRate * 5.0  // 5s modulation

    override fun generate(buffer: ShortArray): Int {
        val amp = volume * 0.35f * Short.MAX_VALUE
        val frames = buffer.size / 2
        val normPink = 1f / numRows

        for (i in 0 until frames) {
            // --- Layer 1: Wind ---
            pinkCounter++
            val tz = Integer.numberOfTrailingZeros(pinkCounter).coerceAtMost(numRows - 1)
            runningSum -= rows[tz]
            val newVal = random.nextFloat() * 2f - 1f
            rows[tz] = newVal
            runningSum += newVal
            val pink = runningSum * normPink

            // Sweep center frequency 1kHz - 3kHz
            val sweepPhase = 2.0 * Math.PI * sampleIndex / windSweepPeriod
            val sweepCenter = 2000f + 1000f * sin(sweepPhase).toFloat()
            windBpfL.setCutoff(sweepCenter * 0.95f, 0.6f)
            windBpfR.setCutoff(sweepCenter * 1.05f, 0.6f)

            val windL = windBpfL.process(pink) * 0.5f
            val windR = windBpfR.process(pink + (random.nextFloat() - 0.5f) * 0.05f) * 0.5f

            // --- Layer 2: Bird calls ---
            var birdL = 0f
            var birdR = 0f

            birdCountdown--
            if (birdCountdown <= 0 && !birdActive && birdBurstRemaining <= 0) {
                // Start a burst of 1-3 calls
                birdBurstRemaining = 1 + random.nextInt(3)
                birdCountdown = 0
            }

            if (birdBurstRemaining > 0 && !birdActive && birdBurstGap <= 0) {
                birdActive = true
                birdSamplePos = 0
                birdCurrentVoice = birdVariants[random.nextInt(birdVariants.size)]
                birdPan = 0.1f + random.nextFloat() * 0.8f
                birdBurstRemaining--
            }

            if (birdBurstGap > 0) birdBurstGap--

            if (birdActive) {
                val voice = birdCurrentVoice
                val t = birdSamplePos.toFloat() / sampleRate
                val envT = birdSamplePos.toFloat() / voice.duration
                // Bell-shaped envelope
                val env = (4f * envT * (1f - envT)).coerceAtLeast(0f)

                val modFreq = voice.baseFreq * voice.modRatio
                val modSignal = sin(2.0 * Math.PI * modFreq * t).toFloat()
                val carrierFreq = voice.baseFreq + voice.modIndex * voice.baseFreq * modSignal
                val birdSample = sin(2.0 * Math.PI * carrierFreq * t).toFloat() * env * 0.15f

                birdL = birdSample * (1f - birdPan)
                birdR = birdSample * birdPan

                birdSamplePos++
                if (birdSamplePos >= voice.duration) {
                    birdActive = false
                    birdBurstGap = (sampleRate * (0.05 + random.nextDouble() * 0.15)).toInt()
                    if (birdBurstRemaining <= 0) {
                        birdCountdown = nextBirdInterval()
                    }
                }
            }

            // --- Layer 3: Leaf rustling ---
            leafCountdown--
            if (leafCountdown <= 0 && !leafActive) {
                leafActive = true
                leafDecay = 0.3f + random.nextFloat() * 0.4f
                leafPan = random.nextFloat()
                leafCountdown = nextLeafInterval()
            }

            var leafL = 0f
            var leafR = 0f
            if (leafActive) {
                val leafNoise = (random.nextFloat() * 2f - 1f) * leafDecay
                val leafFiltered = leafBpf.process(leafNoise) * 0.08f
                leafL = leafFiltered * (1f - leafPan)
                leafR = leafFiltered * leafPan
                leafDecay *= leafDecayRate
                if (leafDecay < 0.001f) leafActive = false
            }

            // --- Layer 4: Stream ---
            val streamNoise = random.nextFloat() * 2f - 1f
            val streamModPhase = 2.0 * Math.PI * sampleIndex / streamModPeriod
            val streamMod = 0.7f + 0.3f * sin(streamModPhase).toFloat()
            val streamL = streamBpfL.process(streamNoise) * 0.06f * streamMod
            val streamR = streamBpfR.process(streamNoise + (random.nextFloat() - 0.5f) * 0.1f) * 0.06f * streamMod

            // --- Mix ---
            val mixL = softClip(windL + birdL + leafL + streamL)
            val mixR = softClip(windR + birdR + leafR + streamR)

            buffer[i * 2] = (mixL * amp).toInt().coerceIn(
                Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
            ).toShort()
            buffer[i * 2 + 1] = (mixR * amp).toInt().coerceIn(
                Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
            ).toShort()

            sampleIndex++
        }
        return buffer.size
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }
}
