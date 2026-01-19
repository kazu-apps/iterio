# ZENITH - アプリ仕様書

## 1. アプリ概要

### アプリ名
**ZENITH** (ゼニス)

### コンセプト
ポモドーロテクニックとエビングハウス忘却曲線を組み合わせた、集中学習支援アプリ。フォーカスモード（アプリブロック機能）により、スマートフォン依存を防ぎながら効率的な学習を実現する。

### ターゲットユーザー
- 受験生（中学・高校・大学受験）
- 資格試験受験者
- 集中力を高めたい学習者
- スマートフォン依存から脱却したい人

---

## 2. 技術スタック

### 言語・SDK
| 項目 | 値 |
|------|-----|
| 言語 | Kotlin |
| Minimum SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Compile SDK | 35 |
| Java Version | 17 |

### アーキテクチャ
**MVVM + Repository Pattern + Clean Architecture**

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  (Composable Screens + ViewModels)                  │
├─────────────────────────────────────────────────────┤
│                  Domain Layer                        │
│  (Repository Interfaces + Domain Models)            │
├─────────────────────────────────────────────────────┤
│                   Data Layer                         │
│  (Repository Impl + DAO + Room Database)            │
├─────────────────────────────────────────────────────┤
│                  Service Layer                       │
│  (TimerService + FocusModeService + LockOverlay)    │
└─────────────────────────────────────────────────────┘
```

### 使用ライブラリ

| カテゴリ | ライブラリ |
|---------|-----------|
| UI | Jetpack Compose, Material Design 3 |
| Navigation | Navigation Compose |
| Database | Room |
| DI | Hilt |
| 非同期処理 | Kotlin Coroutines, Flow |
| バックグラウンド | WorkManager |
| データ保存 | DataStore Preferences |
| シリアライズ | Gson, Kotlinx Serialization |
| Widget | Glance |

---

## 3. プロジェクト構成

### ディレクトリ構造

```
app/src/main/java/com/zenith/app/
├── data/
│   ├── local/
│   │   ├── entity/          # Roomエンティティ
│   │   ├── dao/             # Data Access Objects
│   │   ├── converter/       # 型コンバーター
│   │   └── ZenithDatabase.kt
│   └── repository/          # リポジトリ実装
├── domain/
│   ├── model/               # ドメインモデル
│   └── repository/          # リポジトリインターフェース
├── di/                      # Hilt DIモジュール
├── ui/
│   ├── screens/             # 各画面のComposable
│   │   ├── home/
│   │   ├── tasks/
│   │   ├── timer/
│   │   ├── stats/
│   │   ├── calendar/
│   │   └── settings/
│   ├── components/          # 共通UIコンポーネント
│   ├── theme/               # テーマ・カラー定義
│   ├── navigation/          # ナビゲーション設定
│   └── MainActivity.kt
├── service/                 # サービス群
│   ├── TimerService.kt
│   ├── FocusModeService.kt
│   └── LockOverlayService.kt
├── worker/                  # WorkManager
├── util/                    # ユーティリティ
├── widget/                  # ホームウィジェット
└── ZenithApplication.kt
```

### 主要ファイルの役割

| ファイル | 役割 |
|---------|------|
| `ZenithDatabase.kt` | Room Database定義（version 3） |
| `TimerService.kt` | ポモドーロタイマーのForeground Service |
| `FocusModeService.kt` | アプリブロックのAccessibility Service |
| `LockOverlayService.kt` | 完全ロックモードのオーバーレイ表示 |
| `ReviewReminderWorker.kt` | 復習リマインダーのWorkManager |

---

## 4. データベース設計

### Roomバージョン
**Version 3** (fallbackToDestructiveMigration使用)

### エンティティ一覧

#### SubjectGroupEntity（科目グループ）
```kotlin
@Entity(tableName = "subject_groups")
data class SubjectGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#00838F",
    val displayOrder: Int,
    val createdAt: LocalDateTime
)
```

#### TaskEntity（タスク）
```kotlin
@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = SubjectGroupEntity::class,
        parentColumns = ["id"],
        childColumns = ["groupId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val progressNote: String? = null,
    val progressPercent: Int? = null,
    val nextGoal: String? = null,
    val workDurationMinutes: Int? = null,  // タスク固有の作業時間
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```

#### StudySessionEntity（学習セッション）
```kotlin
@Entity(
    tableName = "study_sessions",
    foreignKeys = [ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime? = null,
    val workDurationMinutes: Int = 0,
    val plannedDurationMinutes: Int = 25,
    val cyclesCompleted: Int = 0,
    val wasInterrupted: Boolean = false,
    val notes: String? = null
)
```

#### ReviewTaskEntity（復習タスク）
```kotlin
@Entity(tableName = "review_tasks")
data class ReviewTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studySessionId: Long,
    val taskId: Long,
    val scheduledDate: LocalDate,
    val reviewNumber: Int,  // 1-6
    val isCompleted: Boolean = false,
    val completedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime
)
```

#### SettingsEntity（設定）
```kotlin
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)
```

#### DailyStatsEntity（日別統計）
```kotlin
@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val date: LocalDate,
    val totalStudyMinutes: Int,
    val sessionCount: Int,
    val subjectBreakdownJson: String
)
```

### テーブル関連図

```
subject_groups (1) ──< tasks (N)
                          │
                          │ (1)
                          ▼
                    study_sessions (N)
                          │
                          │ (1)
                          ▼
                    review_tasks (N)
```

---

## 5. 機能一覧

### ポモドーロタイマー
| 項目 | 状況 | 詳細 |
|------|------|------|
| 基本タイマー | ✅ 完了 | 作業・短休憩・長休憩のサイクル |
| カスタム作業時間 | ✅ 完了 | 1-180分（タスク単位・設定単位） |
| カスタム休憩時間 | ✅ 完了 | 短休憩3-15分、長休憩10-30分 |
| サイクル数設定 | ✅ 完了 | 2-10サイクル（セッション開始時に変更可） |
| 通知表示 | ✅ 完了 | Foreground Service通知 |
| 一時停止/再開 | ✅ 完了 | |
| スキップ | ✅ 完了 | フォーカスモード時は非表示 |

### フォーカスモード（アプリブロック）
| 項目 | 状況 | 詳細 |
|------|------|------|
| Accessibility Service | ✅ 完了 | アプリ切り替え検出 |
| アプリへの強制復帰 | ✅ 完了 | 他アプリ起動時にZENITHに戻る |
| システムアプリ許可 | ✅ 完了 | 電話・設定・ランチャーは許可 |
| 通常モード | ✅ 完了 | 緊急時は解除可能 |

### 完全ロックモード
| 項目 | 状況 | 詳細 |
|------|------|------|
| オーバーレイ表示 | ✅ 完了 | 半透明のフルスクリーンオーバーレイ |
| タイマー表示 | ✅ 完了 | オーバーレイ上に残り時間表示 |
| 停止ボタン無効化 | ✅ 完了 | タイマー終了まで解除不可 |
| 権限リクエスト | ✅ 完了 | SYSTEM_ALERT_WINDOW権限 |
| セッション単位切替 | ✅ 完了 | 開始時にON/OFF選択可能 |

### 学習記録・タスク管理
| 項目 | 状況 | 詳細 |
|------|------|------|
| 科目グループ作成 | ✅ 完了 | カラー選択（8色）可能 |
| タスク作成 | ✅ 完了 | タスク固有の作業時間設定可 |
| 進捗管理 | ✅ 完了 | パーセンテージ・メモ・次の目標 |
| セッション履歴 | ✅ 完了 | 学習時間・サイクル数記録 |

### 忘却曲線（復習スケジュール）
| 項目 | 状況 | 詳細 |
|------|------|------|
| 自動スケジュール生成 | ✅ 完了 | セッション完了時に生成 |
| 復習間隔 | ✅ 完了 | 1, 3, 7, 14, 30, 60日後 |
| 完了マーク | ✅ 完了 | |
| リスケジュール | ✅ 完了 | |
| リマインダー通知 | ✅ 完了 | WorkManagerで毎朝9時 |

### カレンダー・統計
| 項目 | 状況 | 詳細 |
|------|------|------|
| 月間カレンダー | ✅ 完了 | ヒートマップ表示（5段階） |
| 連続学習日数 | ✅ 完了 | ストリーク表示 |
| 週間統計 | ✅ 完了 | 棒グラフ表示 |
| 月間合計 | ✅ 完了 | |

### ウィジェット
| 項目 | 状況 | 詳細 |
|------|------|------|
| Glanceウィジェット | ✅ 完了 | 2x2サイズ |
| 学習時間表示 | ✅ 完了 | 本日の学習時間 |
| ストリーク表示 | ✅ 完了 | 連続日数 |
| タイマー状態表示 | ✅ 完了 | 実行中のタイマー表示 |

### BGM機能
| 項目 | 状況 | 詳細 |
|------|------|------|
| バックグラウンド再生 | ❌ 未着手 | MediaPlayer/ExoPlayer |
| 曲選択 | ❌ 未着手 | プリセットBGM |
| 音量調整 | ❌ 未着手 | |

### バックアップ
| 項目 | 状況 | 詳細 |
|------|------|------|
| ローカルバックアップ | ❌ 未着手 | JSONエクスポート |
| クラウド同期 | ❌ 未着手 | Google Drive連携 |

---

## 6. 画面構成

### 画面一覧

| 画面 | ルート | 役割 |
|------|--------|------|
| ホーム | `home` | 今日の学習状況・クイックスタート |
| タスク | `tasks` | 科目グループ・タスク管理 |
| タイマー | `timer/{taskId}` | ポモドーロタイマー |
| 統計 | `stats` | 学習統計・グラフ |
| カレンダー | `calendar` | 月間学習記録 |
| 設定 | `settings` | アプリ設定 |

### ナビゲーション

```
BottomNavigationBar
├── ホーム (Home)
├── タスク (Tasks)
├── 統計 (Stats)
├── カレンダー (Calendar)
└── 設定 (Settings)

タスク画面 → タイマー画面（taskIdで遷移）
```

### 主要UIコンポーネント

| コンポーネント | 用途 |
|---------------|------|
| `ZenithTopBar` | カスタムトップバー |
| `ZenithCard` | テーマ適用カード |
| `CircularTimer` | 円形タイマー表示 |
| `PhaseIndicator` | フェーズ・サイクル表示 |
| `TimerControls` | タイマー操作ボタン |
| `LoadingIndicator` | ローディング表示 |

---

## 7. 権限・サービス

### Android権限一覧

| 権限 | 用途 | 状況 |
|------|------|------|
| `FOREGROUND_SERVICE` | タイマーサービス | ✅ |
| `FOREGROUND_SERVICE_SPECIAL_USE` | 特殊用途サービス | ✅ |
| `POST_NOTIFICATIONS` | 通知表示 | ✅ |
| `VIBRATE` | バイブレーション | ✅ |
| `WAKE_LOCK` | 画面ON維持 | ✅ |
| `SYSTEM_ALERT_WINDOW` | オーバーレイ表示 | ✅ |
| `BIND_ACCESSIBILITY_SERVICE` | アプリブロック | ✅ |

### サービス実装状況

| サービス | 種別 | 状況 |
|---------|------|------|
| `TimerService` | Foreground Service | ✅ 完了 |
| `FocusModeService` | Accessibility Service | ✅ 完了 |
| `LockOverlayService` | 通常Service | ✅ 完了 |
| `ReviewReminderWorker` | WorkManager | ✅ 完了 |

---

## 8. デザイン

### カラーテーマ

| 用途 | カラーコード | 説明 |
|------|-------------|------|
| Primary | `#00838F` | Teal 700 |
| Accent | `#4DD0E1` | Cyan 300 |
| Background | `#121212` | ダークグレー |
| Surface | `#1E1E1E` | カード背景 |
| TextPrimary | `#DEFFFFFF` | 白87% |
| TextSecondary | `#99FFFFFF` | 白60% |
| Success | `#4CAF50` | 緑 |
| Warning | `#FF9800` | オレンジ |
| Error | `#E53935` | 赤 |

### ヒートマップカラー（5段階）

| レベル | 条件 | カラー |
|--------|------|--------|
| 0 | 0分 | `#2D2D2D` |
| 1 | 1-29分 | `#004D40` |
| 2 | 30-59分 | `#00695C` |
| 3 | 60-119分 | `#00838F` |
| 4 | 120分以上 | `#4DD0E1` |

### UIの方針
- **ダークテーマ**: 目に優しい暗色基調
- **Material Design 3**: 最新のデザインガイドライン
- **日本語UI**: 全テキスト日本語対応
- **シンプル**: 機能を直感的に使えるUI

---

## 9. マネタイズ設計

### 現在の状況
**未実装** - 現在は全機能無料

### 将来の計画（案）

| 機能 | 無料版 | Premium版 |
|------|--------|-----------|
| 基本タイマー | ✅ | ✅ |
| フォーカスモード | ✅ | ✅ |
| 完全ロックモード | - | ✅ |
| BGM | - | ✅ |
| クラウドバックアップ | - | ✅ |
| 広告 | あり | なし |
| 統計詳細 | 基本 | 詳細 |

---

## 10. 当初の計画から変更した点

### 変更1: 厳格モード → 完全ロックモード
- **変更内容**: 名称を「厳格モード」から「完全ロックモード」に変更
- **理由**: より直感的でわかりやすい名前にするため
- **追加実装**: フルスクリーンオーバーレイによる物理的なアプリブロック

### 変更2: タスク固有の作業時間
- **変更内容**: グローバル設定のみ → タスクごとに作業時間設定可能
- **理由**: 科目や内容によって最適な学習時間が異なるため

### 変更3: セッション開始時のカスタマイズ
- **変更内容**: 設定画面のみ → セッション開始ダイアログでも変更可能
- **理由**: 状況に応じて柔軟に変更したいニーズ
- **対象**: サイクル数、完全ロックモードのON/OFF

### 変更4: 作業時間スライダーの範囲
- **変更内容**: 15-60分 → 1-180分
- **理由**: 短時間の集中や長時間の作業にも対応するため
- **追加**: +/−ボタンによる微調整機能

---

## 11. 今後の開発フェーズ

### Phase 2（次期）

| タスク | 優先度 | 見積もり |
|--------|--------|----------|
| BGM機能実装 | 高 | - |
| ローカルバックアップ | 中 | - |
| レビュー画面の改善 | 中 | - |
| 通知のカスタマイズ | 低 | - |

### Phase 3（将来）

| タスク | 優先度 |
|--------|--------|
| クラウド同期（Google Drive） | 中 |
| Premium課金機能 | 中 |
| 多言語対応（英語） | 低 |
| 学習グループ機能 | 低 |

### 現在のブロッカー・課題

| 課題 | 詳細 | 対応案 |
|------|------|--------|
| Accessibility Service有効化 | ユーザーが手動で有効化する必要あり | 初回起動時のガイド表示 |
| オーバーレイ権限 | 完全ロックモード使用時に権限必要 | ダイアログで案内実装済み |
| BGM著作権 | 使用可能なBGMの調達 | フリー音源またはオリジナル作成 |

---

## 12. ファイル総数

| カテゴリ | 数 |
|---------|-----|
| Kotlinファイル | 73 |
| XMLリソース | 多数 |
| 画面数 | 6 |
| サービス | 3 |
| Worker | 1 |
| Entity | 6 |
| DAO | 6 |
| Repository | 6 |

---

*最終更新: 2026年1月18日*
*バージョン: 1.0.0 (Phase 1)*
