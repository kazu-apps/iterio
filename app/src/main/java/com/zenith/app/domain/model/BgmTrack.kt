package com.zenith.app.domain.model

import androidx.annotation.RawRes
import com.zenith.app.R

/**
 * BGMトラックを表すデータクラス
 */
data class BgmTrack(
    val id: String,
    val nameJa: String,
    val category: BgmCategory,
    @RawRes val resourceId: Int
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
            resourceId = R.raw.bgm_rain
        ),
        BgmTrack(
            id = "forest",
            nameJa = "森の音",
            category = BgmCategory.NATURE,
            resourceId = R.raw.bgm_forest
        ),
        BgmTrack(
            id = "waves",
            nameJa = "波の音",
            category = BgmCategory.NATURE,
            resourceId = R.raw.bgm_waves
        ),
        BgmTrack(
            id = "lofi_study",
            nameJa = "Lo-Fi Study",
            category = BgmCategory.LOFI,
            resourceId = R.raw.bgm_lofi_study
        ),
        BgmTrack(
            id = "white_noise",
            nameJa = "ホワイトノイズ",
            category = BgmCategory.WHITE_NOISE,
            resourceId = R.raw.bgm_white_noise
        )
    )

    fun getById(id: String): BgmTrack? = all.find { it.id == id }

    fun getByCategory(category: BgmCategory): List<BgmTrack> = all.filter { it.category == category }
}
