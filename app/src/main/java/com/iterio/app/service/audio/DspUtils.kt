package com.iterio.app.service.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Biquad filter (Robert Bristow-Johnson Audio EQ Cookbook).
 * Supports lowpass, highpass, and bandpass with dynamic cutoff changes.
 */
class BiquadFilter(
    private val sampleRate: Int,
    private var type: FilterType = FilterType.LOWPASS,
    cutoffHz: Float = 1000f,
    q: Float = 0.707f
) {
    enum class FilterType { LOWPASS, HIGHPASS, BANDPASS }

    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    init {
        computeCoefficients(cutoffHz, q)
    }

    fun setCutoff(cutoffHz: Float, q: Float = 0.707f) {
        computeCoefficients(cutoffHz, q)
    }

    private fun computeCoefficients(cutoffHz: Float, q: Float) {
        val clampedFreq = cutoffHz.coerceIn(20f, sampleRate / 2f - 1f)
        val w0 = 2.0 * PI * clampedFreq / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)

        val a0: Double
        when (type) {
            FilterType.LOWPASS -> {
                b0 = (1.0 - cosW0) / 2.0
                b1 = 1.0 - cosW0
                b2 = (1.0 - cosW0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            FilterType.HIGHPASS -> {
                b0 = (1.0 + cosW0) / 2.0
                b1 = -(1.0 + cosW0)
                b2 = (1.0 + cosW0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            FilterType.BANDPASS -> {
                b0 = alpha
                b1 = 0.0
                b2 = -alpha
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
        }

        b0 /= a0
        b1 /= a0
        b2 /= a0
        a1 /= a0
        a2 /= a0
    }

    fun process(input: Float): Float {
        val x0 = input.toDouble()
        val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x0
        y2 = y1
        y1 = y0
        return y0.toFloat()
    }

    fun reset() {
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }
}

/**
 * Lightweight one-pole lowpass filter.
 * coeff closer to 1.0 = more smoothing (lower cutoff).
 */
class OnePoleFilter(private var coeff: Float = 0.85f) {
    private var prev: Float = 0f

    fun process(input: Float): Float {
        prev = prev * coeff + input * (1f - coeff)
        return prev
    }

    fun setCoeff(c: Float) {
        coeff = c.coerceIn(0f, 0.999f)
    }

    fun reset() {
        prev = 0f
    }
}

/**
 * tanh-based soft clipper to prevent harsh digital clipping.
 */
fun softClip(sample: Float): Float {
    return tanh(sample.toDouble()).toFloat()
}

/**
 * Linear interpolation between a and b.
 */
fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}
