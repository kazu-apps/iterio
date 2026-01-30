package com.iterio.app.domain.model

/**
 * 音声生成アルゴリズムの種類
 */
enum class AudioGeneratorType {
    WHITE_NOISE,
    RAIN,
    FOREST,
    WAVES,
    LOFI_STUDY
}

/**
 * BGMトラックを表すデータクラス
 */
data class BgmTrack(
    val id: String,
    val nameJa: String,
    val category: BgmCategory,
    val generatorType: AudioGeneratorType
)

/**
 * BGMカテゴリ
 */
enum class BgmCategory(val nameJa: String) {
    NATURE("自然音"),
    AMBIENT("アンビエント"),
    LOFI("Lo-Fi"),
    WHITE_NOISE("ホワイトノイズ")
}

/**
 * 利用可能なBGMトラック一覧
 */
object BgmTracks {
    val all = listOf(
        BgmTrack(
            id = "rain",
            nameJa = "雨音",
            category = BgmCategory.NATURE,
            generatorType = AudioGeneratorType.RAIN
        ),
        BgmTrack(
            id = "forest",
            nameJa = "森の音",
            category = BgmCategory.NATURE,
            generatorType = AudioGeneratorType.FOREST
        ),
        BgmTrack(
            id = "waves",
            nameJa = "波の音",
            category = BgmCategory.NATURE,
            generatorType = AudioGeneratorType.WAVES
        ),
        BgmTrack(
            id = "lofi_study",
            nameJa = "Lo-Fi Study",
            category = BgmCategory.LOFI,
            generatorType = AudioGeneratorType.LOFI_STUDY
        ),
        BgmTrack(
            id = "white_noise",
            nameJa = "ホワイトノイズ",
            category = BgmCategory.WHITE_NOISE,
            generatorType = AudioGeneratorType.WHITE_NOISE
        )
    )

    fun getById(id: String): BgmTrack? = all.find { it.id == id }

    fun getByCategory(category: BgmCategory): List<BgmTrack> = all.filter { it.category == category }
}
