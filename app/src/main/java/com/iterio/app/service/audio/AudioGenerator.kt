package com.iterio.app.service.audio

/**
 * Procedural audio sample generator interface.
 *
 * All implementations produce **stereo interleaved** PCM 16-bit samples:
 * [L0, R0, L1, R1, ...]. The buffer size passed to [generate] is always
 * an even number representing total frames * 2 channels.
 */
interface AudioGenerator {
    /** Fill [buffer] with stereo interleaved PCM 16-bit samples. Returns the number of samples written. */
    fun generate(buffer: ShortArray): Int
    /** Set playback volume (0.0f to 1.0f). */
    fun setVolume(volume: Float)
}
