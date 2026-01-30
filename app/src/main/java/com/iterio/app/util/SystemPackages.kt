package com.iterio.app.util

/**
 * システムパッケージの定数を管理するオブジェクト
 * フォーカスモード中に常に許可されるアプリを定義
 */
object SystemPackages {

    /**
     * ランチャーアプリ（ホーム画面）
     */
    val LAUNCHERS = setOf(
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",  // Google Pixel launcher
        "com.sec.android.app.launcher",            // Samsung launcher
        "com.mi.android.globallauncher",           // Xiaomi launcher
        "com.huawei.android.launcher",             // Huawei launcher
        "com.oppo.launcher"                        // OPPO launcher
    )

    /**
     * システムUI
     */
    val SYSTEM_UI = setOf(
        "com.android.systemui"
    )

    /**
     * 緊急・電話関連アプリ（常に許可すべき）
     */
    val EMERGENCY = setOf(
        "com.android.settings",       // 設定
        "com.android.phone",          // 電話
        "com.android.dialer",         // ダイヤラー
        "com.google.android.dialer",  // Googleダイヤラー
        "com.android.emergency",      // 緊急情報
        "com.android.contacts"        // 連絡先
    )

    /**
     * ソフトモードで許可されるパッケージ
     * ランチャー + SystemUI のみ（停止ボタンで解除可能なため緊急アプリは不要）
     */
    val SOFT_MODE_ALLOWED = LAUNCHERS + SYSTEM_UI

    /**
     * 常に許可されるべきすべてのパッケージ
     */
    val ALWAYS_ALLOWED = LAUNCHERS + SYSTEM_UI + EMERGENCY

    /**
     * 完全ロックモード（strict mode）で許可されるパッケージ
     * ランチャーを除外して、ホーム画面への移動を防ぐ
     */
    val STRICT_MODE_ALLOWED = SYSTEM_UI + EMERGENCY
}
