package com.iterio.app.service.audio

import com.iterio.app.domain.model.AudioGeneratorType

/**
 * Factory that creates the appropriate [AudioGenerator] for a given [AudioGeneratorType].
 */
object AudioGeneratorFactory {
    fun create(type: AudioGeneratorType, sampleRate: Int): AudioGenerator {
        return when (type) {
            AudioGeneratorType.WHITE_NOISE -> WhiteNoiseGenerator(sampleRate)
            AudioGeneratorType.RAIN -> RainGenerator(sampleRate)
            AudioGeneratorType.FOREST -> ForestGenerator(sampleRate)
            AudioGeneratorType.WAVES -> WavesGenerator(sampleRate)
            AudioGeneratorType.LOFI_STUDY -> LoFiGenerator(sampleRate)
        }
    }
}
