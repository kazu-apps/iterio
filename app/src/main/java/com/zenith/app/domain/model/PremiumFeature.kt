package com.zenith.app.domain.model

enum class PremiumFeature(
    val titleJa: String,
    val descriptionJa: String
) {
    COMPLETE_LOCK_MODE(
        titleJa = "完全ロックモード",
        descriptionJa = "タイマー終了まで他のアプリを使用できなくなり、集中力を最大限に高めます。"
    ),
    FULL_REVIEW_INTERVALS(
        titleJa = "忘却曲線（全6回）",
        descriptionJa = "1日後、3日後、7日後、14日後、30日後、60日後の復習で長期記憶を定着させます。"
    ),
    CALENDAR_HEATMAP(
        titleJa = "ヒートマップカレンダー",
        descriptionJa = "学習量をヒートマップで可視化し、継続のモチベーションを高めます。"
    ),
    DETAILED_STATS(
        titleJa = "詳細統計",
        descriptionJa = "週間・月間の学習データや詳細な分析で、学習効率を改善できます。"
    ),
    BGM(
        titleJa = "BGM機能",
        descriptionJa = "集中力を高めるBGMを再生しながら学習できます。"
    ),
    BACKUP(
        titleJa = "バックアップ",
        descriptionJa = "学習データをバックアップし、端末変更時も安心です。"
    ),
    WIDGET(
        titleJa = "ウィジェット",
        descriptionJa = "ホーム画面から学習状況を確認し、すぐにタイマーを開始できます。"
    )
}
