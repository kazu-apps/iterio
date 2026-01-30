package com.iterio.app.service.audio

import java.util.Random
import kotlin.math.exp
import kotlin.math.sin

/**
 * Realistic rain sound generator with 3 layers:
 *
 * Layer 1 - Steady rain base: pink noise -> bandpass (1k-10kHz) for "shhhh" sound
 * Layer 2 - Raindrop impulses: Poisson-distributed random short bursts (2-5ms),
 *           frequency band 3-8kHz, each drop randomly panned L/R
 * Layer 3 - Distant thunder: 20-80Hz low noise, random intervals 30-60s,
 *           1s attack / 3-5s decay envelope
 * Time variation: 20-40s cycle smoothly modulates rain intensity
 */
class RainGenerator(private val sampleRate: Int) : AudioGenerator {
    private val random = Random()
    @Volatile private var volume: Float = 0.5f

    // --- Layer 1: Steady rain (bandpass pink noise) ---
    private val numRows = 16
    private val rows = FloatArray(numRows) { random.nextFloat() * 2f - 1f }
    private var runningSum: Float = rows.sum()
    private var counter: Int = 0
    private val bpfL = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 3000f, 0.8f)
    private val bpfR = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 3500f, 0.8f)

    // --- Layer 2: Raindrop impulses ---
    // Probability of a raindrop per sample: ~60 drops/sec
    private val dropProbPerSample = 60.0f / sampleRate
    private val dropFilterL = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 5000f, 1.5f)
    private val dropFilterR = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 5500f, 1.5f)
    private var dropDecayL: Float = 0f
    private var dropDecayR: Float = 0f
    private val dropDecayRate = exp(-1.0 / (sampleRate * 0.003)).toFloat() // ~3ms decay

    // --- Layer 3: Distant thunder ---
    private var thunderEnvelope: Float = 0f
    private var thunderPhase: Float = 0f
    private var thunderFreq: Float = 40f
    private var thunderCountdown: Int = nextThunderInterval()
    private var thunderDecayRate: Float = 0f
    private var thunderActive: Boolean = false
    private val thunderAttackRate = 1f / (sampleRate * 1.0f)  // 1s attack

    // --- Time variation ---
    private var sampleIndex: Long = 0
    private val intensityPeriodSamples = sampleRate * 30.0  // ~30s cycle

    private fun nextThunderInterval(): Int {
        return sampleRate * (30 + random.nextInt(30))  // 30-60 seconds
    }

    override fun generate(buffer: ShortArray): Int {
        val amp = volume * 0.35f * Short.MAX_VALUE
        val frames = buffer.size / 2
        val normPink = 1f / numRows

        for (i in 0 until frames) {
            // Intensity modulation (20-40s cycle)
            val intensityPhase = 2.0 * Math.PI * sampleIndex / intensityPeriodSamples
            val intensity = 0.7f + 0.3f * sin(intensityPhase).toFloat()

            // --- Layer 1: Pink noise base -> bandpass ---
            counter++
            val tz = Integer.numberOfTrailingZeros(counter).coerceAtMost(numRows - 1)
            runningSum -= rows[tz]
            val newVal = random.nextFloat() * 2f - 1f
            rows[tz] = newVal
            runningSum += newVal
            val pinkSample = runningSum * normPink

            val rainL = bpfL.process(pinkSample) * intensity
            val rainR = bpfR.process(pinkSample + (random.nextFloat() - 0.5f) * 0.1f) * intensity

            // --- Layer 2: Raindrop impulses ---
            if (random.nextFloat() < dropProbPerSample * intensity) {
                val dropAmp = 0.3f + random.nextFloat() * 0.7f
                val pan = random.nextFloat()  // 0=left, 1=right
                dropDecayL += dropAmp * (1f - pan)
                dropDecayR += dropAmp * pan
            }
            val dropL = dropFilterL.process(dropDecayL * (random.nextFloat() * 2f - 1f)) * 0.4f
            val dropR = dropFilterR.process(dropDecayR * (random.nextFloat() * 2f - 1f)) * 0.4f
            dropDecayL *= dropDecayRate
            dropDecayR *= dropDecayRate

            // --- Layer 3: Distant thunder ---
            thunderCountdown--
            if (thunderCountdown <= 0 && !thunderActive) {
                thunderActive = true
                thunderEnvelope = 0f
                thunderFreq = 20f + random.nextFloat() * 60f
                thunderDecayRate = exp(-1.0 / (sampleRate * (3.0 + random.nextDouble() * 2.0))).toFloat()
                thunderCountdown = nextThunderInterval()
            }
            var thunderSample = 0f
            if (thunderActive) {
                if (thunderEnvelope < 1f) {
                    thunderEnvelope = (thunderEnvelope + thunderAttackRate).coerceAtMost(1f)
                } else {
                    thunderEnvelope *= thunderDecayRate
                    if (thunderEnvelope < 0.001f) {
                        thunderActive = false
                        thunderEnvelope = 0f
                    }
                }
                thunderPhase += thunderFreq / sampleRate
                if (thunderPhase > 1f) thunderPhase -= 1f
                val thunderNoise = (random.nextFloat() * 2f - 1f) * 0.5f
                thunderSample = thunderEnvelope * (
                    sin(2.0 * Math.PI * thunderPhase.toDouble()).toFloat() * 0.5f + thunderNoise * 0.5f
                ) * 0.6f
            }

            // --- Mix ---
            val mixL = softClip(rainL * 0.6f + dropL + thunderSample)
            val mixR = softClip(rainR * 0.6f + dropR + thunderSample)

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
