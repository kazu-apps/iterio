package com.iterio.app.service.audio

import java.util.Random
import kotlin.math.sin

/**
 * Refined white noise generator with stereo decorrelation, high-frequency rolloff,
 * and slow breathing modulation for a smooth, non-fatiguing sound.
 *
 * - L/R use independent random seeds for spatial width
 * - OnePoleFilter (coeff 0.85) rolls off frequencies above ~8kHz
 * - 45-second LFO modulates amplitude by 5% for subtle "breathing"
 * - tanh soft clipping prevents harsh peaks
 */
class WhiteNoiseGenerator(private val sampleRate: Int) : AudioGenerator {
    private val randomL = Random(System.nanoTime())
    private val randomR = Random(System.nanoTime() + 12345L)
    @Volatile private var volume: Float = 0.5f

    private val filterL = OnePoleFilter(0.85f)
    private val filterR = OnePoleFilter(0.85f)

    private var sampleIndex: Long = 0
    private val breathPeriodSamples = sampleRate * 45.0  // 45 second breathing cycle

    override fun generate(buffer: ShortArray): Int {
        val amp = volume * 0.3f * Short.MAX_VALUE
        val frames = buffer.size / 2

        for (i in 0 until frames) {
            // Breathing LFO: 5% depth, 45s period
            val breathPhase = 2.0 * Math.PI * sampleIndex / breathPeriodSamples
            val breathMod = 1f - 0.05f + 0.05f * sin(breathPhase).toFloat()

            // Independent Gaussian noise per channel
            val rawL = randomL.nextGaussian().toFloat()
            val rawR = randomR.nextGaussian().toFloat()

            // One-pole lowpass for high-frequency rolloff
            val filteredL = filterL.process(rawL)
            val filteredR = filterR.process(rawR)

            // Soft clip and scale
            val sampleL = softClip(filteredL * breathMod) * amp
            val sampleR = softClip(filteredR * breathMod) * amp

            buffer[i * 2] = sampleL.toInt().coerceIn(
                Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
            ).toShort()
            buffer[i * 2 + 1] = sampleR.toInt().coerceIn(
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
