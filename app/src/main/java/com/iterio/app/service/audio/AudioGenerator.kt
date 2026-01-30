package com.iterio.app.service.audio

import com.iterio.app.domain.model.AudioGeneratorType
import java.util.Random
import kotlin.math.sin

/**
 * 音声サンプルをプロシージャルに生成するインターフェース
 */
interface AudioGenerator {
    /** バッファにPCM 16bitサンプルを書き込み、書き込んだサンプル数を返す */
    fun generate(buffer: ShortArray): Int
    /** 音量を設定（0.0f〜1.0f） */
    fun setVolume(volume: Float)
}

/**
 * ホワイトノイズ生成: ガウシアンランダムサンプル
 */
class WhiteNoiseGenerator(private val sampleRate: Int) : AudioGenerator {
    private val random = Random()
    @Volatile private var volume: Float = 0.5f

    override fun generate(buffer: ShortArray): Int {
        val amplitude = volume * 0.3f * Short.MAX_VALUE
        for (i in buffer.indices) {
            buffer[i] = (random.nextGaussian() * amplitude).toInt().coerceIn(
                Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
            ).toShort()
        }
        return buffer.size
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }
}

/**
 * 雨音生成: ブラウンノイズ（ランダムウォーク + リークファクター）
 */
class RainGenerator(private val sampleRate: Int) : AudioGenerator {
    private val random = Random()
    @Volatile private var volume: Float = 0.5f
    private var lastSample: Float = 0f

    override fun generate(buffer: ShortArray): Int {
        val amplitude = volume * 0.4f * Short.MAX_VALUE
        val leakFactor = 0.999f
        val step = 0.02f

        for (i in buffer.indices) {
            lastSample = lastSample * leakFactor + (random.nextFloat() * 2f - 1f) * step
            lastSample = lastSample.coerceIn(-1f, 1f)
            buffer[i] = (lastSample * amplitude).toInt().coerceIn(
                Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
            ).toShort()
        }
        return buffer.size
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }
}

/**
 * 森の音生成: ピンクノイズ（Voss-McCartney アルゴリズム）
 */
class ForestGenerator(private val sampleRate: Int) : AudioGenerator {
    private val random = Random()
    @Volatile private var volume: Float = 0.5f
    private val numRows = 16
    private val rows = FloatArray(numRows) { random.nextFloat() * 2f - 1f }
    private var runningSum: Float = rows.sum()
    private var counter: Int = 0

    override fun generate(buffer: ShortArray): Int {
        val amplitude = volume * 0.35f * Short.MAX_VALUE
        val normalization = 1f / numRows

        for (i in buffer.indices) {
            counter++
            val trailingZeros = Integer.numberOfTrailingZeros(counter).coerceAtMost(numRows - 1)

            runningSum -= rows[trailingZeros]
            val newValue = random.nextFloat() * 2f - 1f
            rows[trailingZeros] = newValue
            runningSum += newValue

            val sample = runningSum * normalization
            buffer[i] = (sample * amplitude).toInt().coerceIn(
                Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
            ).toShort()
        }
        return buffer.size
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }
}

/**
 * 波の音生成: ブラウンノイズ + 正弦波振幅変調（周期 ~8秒）
 */
class WavesGenerator(private val sampleRate: Int) : AudioGenerator {
    private val random = Random()
    @Volatile private var volume: Float = 0.5f
    private var lastSample: Float = 0f
    private var sampleIndex: Long = 0

    override fun generate(buffer: ShortArray): Int {
        val amplitude = volume * 0.4f * Short.MAX_VALUE
        val leakFactor = 0.999f
        val step = 0.02f
        val wavePeriod = sampleRate * 8.0 // ~8秒周期

        for (i in buffer.indices) {
            lastSample = lastSample * leakFactor + (random.nextFloat() * 2f - 1f) * step
            lastSample = lastSample.coerceIn(-1f, 1f)

            // 正弦波エンベロープ: 0.3〜1.0 の範囲で変調
            val envelope = 0.3f + 0.7f * ((sin(2.0 * Math.PI * sampleIndex / wavePeriod) + 1.0) / 2.0).toFloat()
            buffer[i] = (lastSample * amplitude * envelope).toInt().coerceIn(
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

/**
 * Lo-Fi Study 生成: 正弦波コード進行 + ローパスフィルタ + 微小ノイズ
 * コード: C→Am→F→Dm（各4秒）
 */
class LoFiGenerator(private val sampleRate: Int) : AudioGenerator {
    private val random = Random()
    @Volatile private var volume: Float = 0.5f
    private var sampleIndex: Long = 0
    private var filteredSample: Float = 0f

    // コード進行の周波数（Hz）: C, Am, F, Dm
    private val chords = arrayOf(
        floatArrayOf(261.63f, 329.63f, 392.00f), // C major: C4, E4, G4
        floatArrayOf(220.00f, 261.63f, 329.63f), // A minor: A3, C4, E4
        floatArrayOf(174.61f, 220.00f, 261.63f), // F major: F3, A3, C4
        floatArrayOf(146.83f, 174.61f, 220.00f)  // D minor: D3, F3, A3
    )

    override fun generate(buffer: ShortArray): Int {
        val amplitude = volume * 0.25f * Short.MAX_VALUE
        val chordDurationSamples = sampleRate * 4 // 4秒/コード
        val totalCycleSamples = chordDurationSamples * chords.size
        val filterCoeff = 0.1f // ローパスフィルタ係数（強め）
        val noiseLevel = 0.05f

        for (i in buffer.indices) {
            val positionInCycle = (sampleIndex % totalCycleSamples).toInt()
            val chordIndex = positionInCycle / chordDurationSamples
            val chord = chords[chordIndex]

            // 3音の正弦波を合成
            var sample = 0f
            for (freq in chord) {
                sample += sin(2.0 * Math.PI * freq * sampleIndex / sampleRate).toFloat()
            }
            sample /= chord.size

            // 微小ノイズ追加
            sample += (random.nextFloat() * 2f - 1f) * noiseLevel

            // ローパスフィルタ
            filteredSample = filteredSample + filterCoeff * (sample - filteredSample)

            buffer[i] = (filteredSample * amplitude).toInt().coerceIn(
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

/**
 * AudioGeneratorType に対応するジェネレータインスタンスを生成するファクトリ
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
