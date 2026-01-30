# HANDOVER.md

## Session Status: 完了（BGM音質改善 - 5種類のBGMをリアルな音響に全面書き直し）

## 次セッションのタスクロードマップ

1. 手動テスト: 各BGM再生→5種類が明確に異なる音に聞こえること
2. TDDテスト作成: 新規BGMジェネレータ（DspUtils, 5ジェネレータ, Factory）のユニットテスト
   - DspUtilsTest: BiquadFilter, OnePoleFilter, softClip, lerp
   - AudioGeneratorFactoryTest: 全タイプ生成確認
   - 各ジェネレータ共通テスト: stereo出力、volume制御、バッファサイズ、無音なし、クリッピングなし

### feat: BGM音質改善 - 全5種類のBGMを複数レイヤー合成+ステレオ化 ✅完了

**問題:** 全5種類のBGMが「ノイズの音量を変えただけ」に聞こえる。全てノイズベース、モノラル出力、LoFiのフィルタが壊れている、時間変化なし、スペクトル差なし。

**修正方針:** 各ジェネレータを複数レイヤーの合成で書き直し、ステレオ化。

**変更ファイル:**

| # | ファイル | 変更内容 |
|---|---------|---------|
| 1 | `service/audio/DspUtils.kt` | **新規** BiquadFilter（LP/HP/BP、動的カットオフ）、OnePoleFilter、softClip(tanh)、lerp |
| 2 | `service/audio/AudioGenerator.kt` | インターフェースのみに縮小、KDoc更新（ステレオインターリーブ仕様） |
| 3 | `service/audio/WhiteNoiseGenerator.kt` | **新規** ステレオ非相関L/R + OnePole高域ロールオフ + 45秒呼吸LFO + softClip |
| 4 | `service/audio/RainGenerator.kt` | **新規** 3層: バンドパスピンクノイズ + ポアソン雨粒インパルス(40-80/秒) + 遠雷(30-60秒間隔) |
| 5 | `service/audio/ForestGenerator.kt` | **新規** 4層: 掃引バンドパス風 + FM合成鳥声(3バリエーション) + 葉擦れバースト + 小川 |
| 6 | `service/audio/WavesGenerator.kt` | **新規** 非対称エンベロープ(1.5-2s rise/4-6s decay) + 時変LPF(500-6000Hz) + 泡層 + ステレオパン + セットウェーブ(15%) |
| 7 | `service/audio/LoFiGenerator.kt` | **新規** 三角波コード(C3+E4G4C5ボイシング) + 75BPMドラム(kick/snare/hat) + ビニルクラックル + テープ揺れLFO |
| 8 | `service/audio/AudioGeneratorFactory.kt` | **新規** ファクトリを独立ファイルに抽出 |
| 9 | `service/BgmService.kt` | `CHANNEL_OUT_MONO` → `CHANNEL_OUT_STEREO`（3箇所） |

**スペクトル差別化:**

| トラック | 主要帯域 | 固有要素 |
|---------|---------|---------|
| White Noise | 20Hz-16kHz（フラット） | 広帯域、45秒呼吸 |
| Rain | 1kHz-10kHz（高域寄り） | 雨粒インパルス、遠雷 |
| Forest | 200Hz-5kHz（中域） | FM鳥声、掃引フィルタ |
| Waves | 40Hz-6kHz（時変） | 非対称エンベロープ、時変LPF |
| Lo-Fi | 50Hz-2kHz（ローパス） | ドラム、コード進行、クラックル |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL
**テスト結果:** BgmManagerTest 全テスト PASS

**手動検証チェックリスト:**

| テスト項目 | 期待動作 |
|-----------|---------|
| Rain 再生 | 雨粒の「パチパチ」が聞こえる、時々遠雷 |
| Forest 再生 | 鳥の声が散発的に聞こえる、風が揺れる |
| Waves 再生 | 波の寄せ・引きのリズムが聞こえる |
| Lo-Fi 再生 | ドラムビートとコード進行が聞こえる |
| White Noise 再生 | 滑らかで耳障りでない、微細な呼吸 |
| ステレオ確認 | イヤホンでL/Rの違いが聞こえる |

---

## 前セッション: ソフトモードでシステムアプリがブロックされない修正

### 前セッションのタスクロードマップ

手動テストによる本修正の動作検証

### fix: ソフトモードでシステムアプリ（電話・設定等）がブロックされない ✅完了

**問題:** ソフトモード（アプリ制限モード）で電話・設定・ダイヤラー・連絡先などの緊急アプリがブロックされない。`SystemPackages.ALWAYS_ALLOWED` に `EMERGENCY` セットが含まれており、ソフトモードがこれを使用していたため。

**根本原因:** ソフトモードは停止ボタンで解除可能なため、緊急アプリの許可は不要。ストリクトモード（解除不可）でのみ緊急アプリを許可すべき。

**修正内容:**

| # | ファイル | 変更内容 |
|---|---------|---------|
| 1 | `util/SystemPackages.kt` | `SOFT_MODE_ALLOWED` 定数追加（`LAUNCHERS + SYSTEM_UI`、EMERGENCYなし） |
| 2 | `service/FocusModeService.kt` | ソフトモードで `SOFT_MODE_ALLOWED` を使用（`ALWAYS_ALLOWED` → `SOFT_MODE_ALLOWED`） |
| 3 | `test/service/FocusModeServiceTest.kt` | テスト更新: ソフトモードで `SOFT_MODE_ALLOWED` を検証、`SOFT_MODE_ALLOWED` が EMERGENCY を除外するテスト追加 |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL
**テスト結果:** FocusModeServiceTest 全テスト PASS

**手動検証チェックリスト:**

| テスト項目 | 期待動作 |
|-----------|---------|
| ソフトモード → 電話アプリ起動 | Toast + Iterioに戻される |
| ソフトモード → 設定アプリ起動 | Toast + Iterioに戻される |
| ソフトモード → ダイヤラー起動 | Toast + Iterioに戻される |
| ストリクトモード → 電話アプリ | 許可される（緊急用） |
| ストリクトモード → 設定アプリ | 許可される（緊急用） |

---

### fix: フォーカスモード（ソフト）でアプリブロックが機能しない ✅完了

**問題:** ユーザーがアプリ内設定でフォーカスモードをONにし、開始ダイアログも表示されるが、Androidのユーザー補助サービスが有効でない場合、`TimerService.startTimerInternal()` がフォーカスモードをサイレントにスキップし、実際には何もブロックされない。

**根本原因:** `FocusModeService.isServiceRunning.value` が `false` の場合、`Timber.w` でログ出力のみ行い、ユーザーに一切通知しなかった。

**修正内容:**

| # | ファイル | 変更内容 |
|---|---------|---------|
| 1 | `FocusModeDialog.kt` | `isAccessibilityServiceRunning` パラメータ追加、`AccessibilityServiceWarning` composable 追加（エラーコンテナ表示 + 「設定を開く」ボタン → `ACTION_ACCESSIBILITY_SETTINGS` へ遷移） |
| 2 | `TimerScreen.kt` | `FocusModeService.isServiceRunning` を `collectAsStateWithLifecycle` で取得し `FocusModeWarningDialog` に渡す |
| 3 | `strings.xml` / `strings-en.xml` | 警告文字列3件追加（`focus_mode_accessibility_warning_title`, `_desc`, `_open_settings`） |
| 4 | `TimerService.kt` | `focusModeEnabled && !isServiceRunning` の場合に `Toast.LENGTH_LONG` で既存の `focus_mode_enable_accessibility` 文字列を表示 |
| 5 | `FocusModeService.kt` | `startFocusMode()`: 許可パッケージセットをログ出力、`onAccessibilityEvent()`: パッケージ名と判定結果をログ出力、`showBlockedWarning()`: `Handler.post` 除去（main thread 上で直接 Toast 表示） |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL

**手動検証チェックリスト:**

| テスト項目 | 期待動作 |
|-----------|---------|
| ユーザー補助サービス無効 → フォーカスモード開始ダイアログ表示 | 警告バナー「ユーザー補助サービスが無効です」が表示される |
| 警告の「設定を開く」をタップ | Android のユーザー補助設定画面に遷移 |
| ユーザー補助サービス有効 → フォーカスモード開始ダイアログ表示 | 警告バナーが表示されない |
| ユーザー補助サービス無効のままタイマー開始 | Toast「フォーカスモードを使用するには…」が表示される |
| ソフトモード開始（サービス有効） → 非許可アプリ起動 | Toast + Iterioに戻される |
| Logcat で「Focus mode started」「Focus mode event」が出力される | 診断ログが確認できる |

---

### バグ9: 許可アプリのチェック解除が保存されない / アプリ制限が機能しない ✅完了

**問題:** タイマー画面のボトムシートで許可アプリのチェックを変更しても、DBに保存されずローカル状態のみ更新。画面遷移で元に戻る。全選択/全解除がフィルタ中に正しく動作しない。

**根本原因3つ:**
1. `TimerScreen` のボトムシートで `onSelectionChanged` が `sessionAllowedApps` ローカル状態のみ更新し、`settingsRepository.setAllowedApps()` を呼ばない
2. 全アプリがDB上で許可済みの場合、`FocusModeService` で全アプリが `isPackageAllowed()` = true となり制限が効かない
3. 全選択が `filteredApps.map{}.toSet()` でフィルタ外の既存選択を上書き、全解除が `emptySet()` でフィルタ外の選択も消える

**修正内容:**

| ファイル | 変更内容 |
|---------|---------|
| `ui/screens/timer/TimerViewModel.kt` | `SettingsRepository` をコンストラクタに注入、`updateAllowedApps()` メソッド追加（UiState同期更新 + DB非同期保存） |
| `ui/screens/timer/components/AllowedAppsSelectorBottomSheet.kt` | 個別トグル・全選択・全解除で `onSelectionChanged` を即座に呼出。全選択を union に修正、全解除をフィルタ分のみ除去に修正 |
| `ui/screens/timer/TimerScreen.kt` | `onSelectionChanged` で `viewModel.updateAllowedApps()` を呼出し（DB永続化） |
| `test/.../TimerViewModelTest.kt` | `settingsRepository` mock 追加、`createViewModel()` 引数追加、`updateAllowedApps` テスト2件追加 |
| `service/TimerService.kt` | 既存ビルドエラー修正（`Timber.w(error, msg)` → `Timber.w("msg: $error")`、DomainError は Throwable ではないため） |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL
**テスト結果:** TimerViewModelTest 全51テスト PASS（新規2件含む、0 failures）

**手動検証チェックリスト:**

| テスト項目 | 期待動作 |
|-----------|---------|
| タイマー画面 → 許可アプリ → チェック外す → シート閉じる → 再度開く | チェック状態が維持されている |
| タイマー画面でチェック変更 → 設定画面に戻る | 「X個選択中」が変更後の値を表示 |
| 許可アプリでアプリを外す → フォーカスモード開始 → そのアプリを開く | Toast警告 + Iterioに戻される（ソフトモード） |
| 検索フィルタ中に「全選択」 | フィルタ外の既存選択が維持される |
| 検索フィルタ中に「全解除」 | フィルタ外の既存選択が維持される |

---

### セキュリティレビュー Advisory Notes 対応 ✅完了

**概要:** バグ8修正のセッション管理マイグレーションに対するセキュリティレビューで指摘された3件の LOW priority Advisory Notes を対応。

| # | Advisory Note | 対応内容 |
|---|--------------|---------|
| 1 | `sessionIdDeferred` 再代入時の前回 Deferred 孤立リスク | `createSession()` にコメント追加: UI層が単一アクティブタイマーを保証する前提を文書化 |
| 2 | セッション作成失敗時のサイレント動作 | `Timber.e` → `Timber.w` に変更し、ユーザーに影響するメッセージ（「Timer is running without a session record」）を追加 |
| 3 | `cancelTimer(interrupted: Boolean)` の未使用パラメータ | `interrupted` パラメータを削除、全呼び出し元（TimerScreen, TimerViewModelTest）を更新 |

**変更ファイル:**

| ファイル | 変更内容 |
|---------|---------|
| `service/TimerService.kt` | Advisory #1: createSession() にコメント追加、Advisory #2: ログレベルとメッセージ改善 |
| `ui/screens/timer/TimerViewModel.kt` | Advisory #3: `cancelTimer(interrupted: Boolean)` → `cancelTimer()` |
| `ui/screens/timer/TimerScreen.kt` | Advisory #3: `cancelTimer(interrupted = true)` → `cancelTimer()` |
| `test/.../TimerViewModelTest.kt` | Advisory #3: テスト名とパラメータ更新 |

---

### バグ8: ロック画面から戻った後、セッション完了がどこにも反映されない ✅完了

**問題:** 完全ロックモード中にタイマーが全サイクル完了し、「Tap to return to app」でアプリに戻ると、学習時間・統計・復習タスク生成が一切反映されない。

**根本原因:** セッション保存処理がUI層（`TimerViewModel.finishSession()`）に依存。ロック画面から戻ると ViewModel が再生成され `currentSessionId` が null になるため、`finishSession()` 内の `currentSessionId ?: state.sessionId ?: return` で早期リターンし、DB保存が実行されなかった。

**修正内容: セッション作成・完了処理を TimerService（サービス層）に移動**

| ファイル | 変更内容 |
|---------|---------|
| `service/TimerService.kt` | `@Inject` で UseCase/Repository を注入、`serviceScope` + `sessionIdDeferred` 追加、`createSession()` / `saveCompletedSession()` / `saveInterruptedSession()` / `saveSession()` 追加、`NonCancellable` でDB保存保護、`onDestroy()` で `serviceScope.cancel()` |
| `service/TimerService.kt` (TimerState) | `sessionId: Long?` フィールド追加 |
| `ui/screens/timer/TimerViewModel.kt` | `createSession()` / `finishSession()` / `currentSessionId` / `sessionStartTime` 削除、`StartTimerSessionUseCase` / `FinishTimerSessionUseCase` コンストラクタ引数削除、`updateUiFromServiceState()` から `finishSession()` 呼び出し削除 |
| `test/.../TimerViewModelTest.kt` | `startTimerSessionUseCase` / `finishTimerSessionUseCase` のmock・verify・テスト2件削除、`createViewModel()` 引数更新 |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL
**テスト結果:** TimerViewModelTest 全テスト PASS

**検証方法（手動テスト）:**

| テスト項目 | 期待動作 |
|-----------|---------|
| 完全ロック→全サイクル完了→Tap to return | 学習時間・統計が反映されている |
| 完全ロック→途中停止（通知のStop） | 中断セッションが記録される |
| ソフトモード→全サイクル完了 | FinishDialog表示、統計反映（従来動作維持） |
| ロックなし→全サイクル完了 | FinishDialog表示、統計反映（従来動作維持） |
| ロックなし→キャンセル | 中断セッションが記録される |

---

### 機能要望: フォーカスモードに「アプリ制限モード」（ソフトロック）追加 ✅完了

**概要:** フォーカスモードのロックレベルを2段階（ソフト/ハード）のラジオボタン選択に変更。

**実装内容:**
- 設定画面・タイマー開始ダイアログの「完全ロックモード」チェックボックスをラジオボタン2択に変更
  - **アプリ制限モード（ソフト）**: デフォルト。許可外アプリ起動時にToast警告→Iterioに戻す。オーバーレイなし
  - **完全ロックモード（ハード）**: 既存動作。Premium機能。オーバーレイで画面ロック
- ソフトモード時、`FocusModeService` で非許可アプリ検知時にToast警告を表示してから `returnToApp()` を実行
- 許可アプリ選択を両モードで常時表示（ハードモード時も許可アプリを選択可能に）
- `TimerScreen` で `allowedApps` を常にパス（ハードモード時に空にしない）

**変更ファイル（6ファイル）:**

| ファイル | 変更内容 |
|---------|---------|
| `res/values/strings.xml` | 5文字列追加 + 1文字列更新 |
| `res/values-en/strings.xml` | 5文字列追加 + 1文字列更新 |
| `service/FocusModeService.kt` | `showBlockedWarning()` 追加、soft mode でToast表示 |
| `ui/screens/settings/components/FocusModeSettingsSection.kt` | `SettingsPremiumSwitchItem` → `FocusModeLevelSelector` ラジオボタン |
| `ui/screens/timer/components/FocusModeDialog.kt` | `LockModeOption` → `FocusModeLevelDialogSelector` ラジオボタン、許可アプリ常時表示 |
| `ui/screens/timer/TimerScreen.kt` | `allowedApps` を常にパス（1行変更） |

**データモデル変更:** なし。`focusModeStrict: Boolean` をそのまま使用（`false`=ソフト、`true`=ハード）

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL

### バグ7: 完了ダイアログのサイクル数が常に設定値（3サイクル）と表示される ✅完了

**問題:** タイマーで1サイクルだけ実行して完了しても、「お疲れ様でした！」ダイアログに「3サイクル完了しました。」と表示される。設定画面の「長休憩までのサイクル数: 3サイクル」の設定値をそのまま表示してしまっている。

**根本原因:** `TimerScreen.kt:261` で `FinishDialog` に `uiState.totalCycles`（設定の目標サイクル数）を渡していた。本来は `uiState.currentCycle`（実際に完了したサイクル数）を渡すべきだった。

**修正内容（2ファイル3行）:**
- `FocusModeDialog.kt`: `FinishDialog` のパラメータ名 `totalCycles` → `completedCycles` にリネーム（意味の明確化）
- `TimerScreen.kt`: `FinishDialog` に渡す値を `uiState.totalCycles` → `uiState.currentCycle` に変更

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL

### バグ8: ロック画面から「Tap to return to app」でアプリに戻った後、セッション完了が反映されない ✅完了

**修正済み:** セッション作成・完了処理を `TimerService`（サービス層）に移動。詳細は上部のバグ8セクション参照。

---

## 前セッション: バグ5・6完了

## バグ修正ロードマップ（完了済み）

| # | 修正内容 | ステータス | 概要 |
|---|---------|-----------|------|
| 5 | ロック画面オーバーレイの上端・下端がタッチ可能 | **完了** | 2層防御: Immersive Mode + AccessibilityService パネル閉鎖 |
| 6 | 許可アプリ画面にアプリが表示されない | **完了** | システムアプリフィルタ削除 + `<queries>` 追加 + API 33+ 対応 |

### バグ5: ロック画面オーバーレイの上端・下端がまだタッチ可能 ✅完了

**問題:** バグ4修正後もステータスバー領域（上端）とナビゲーションバー領域（下端）がタッチできる。`TYPE_APPLICATION_OVERLAY` はシステムバーの下に描画されるため、フラグ変更では解決不可能。

**実装: 2層防御アプローチ**

| レイヤー | 手法 | 効果 |
|---------|------|------|
| Layer 1 (主) | Immersive Sticky Mode on MainActivity | システムバーを物理的に非表示 → タッチ対象がなくなる |
| Layer 2 (補助) | AccessibilityService でシステムパネル即閉鎖 | エッジスワイプで一時表示された場合のフォールバック |

**実装内容:**
- `MainActivity.kt`: `enterImmersiveMode()`, `exitImmersiveMode()`, `observeStrictMode()`, `onWindowFocusChanged()` 追加。`FocusModeService.isStrictMode` を監視し、ストリクトモード ON で Immersive Sticky Mode に入り、OFF で復帰 + `enableEdgeToEdge()` 再適用
- `FocusModeService.kt`: `SYSTEM_UI_PACKAGE` 定数追加、`onAccessibilityEvent()` でストリクトモード中に `com.android.systemui` パネルが開いたら `GLOBAL_ACTION_BACK` で即閉鎖

**対象ファイル:**

| ファイル | 変更内容 |
|---------|---------|
| `ui/MainActivity.kt` | Immersive Mode 追加 (enter/exit/observe/onWindowFocusChanged) |
| `service/FocusModeService.kt` | システムパネル閉鎖ロジック追加 |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL

### バグ6: 許可アプリ画面にインストール済みアプリが1つも表示されない ✅完了

**問題:** 設定 > 許可アプリ画面で「インストール済みアプリがありません」と表示。「0個選択中 / 0アプリ」。パッケージ一覧の取得自体が失敗。

**根本原因2つ:**
1. `.filter { !isSystemApp(it) }` が全システムアプリを除外 → エミュレータ/実機でプリインストールアプリ（Chrome, Gmail等）が全て `FLAG_SYSTEM` を持つため、リスト0件
2. `<queries>` 未宣言 → Android 11+ のパッケージ可視性制限で `queryIntentActivities` が空リストを返す可能性

**実装内容:**
- `InstalledAppsHelper.kt`: システムアプリフィルタ `.filter { !isSystemApp(it) }` 削除（`CATEGORY_LAUNCHER` intent が既にユーザー向けアプリのみを返すため冗長）
- `InstalledAppsHelper.kt`: `AllowedApp.isSystemApp` フィールドに `FLAG_SYSTEM` に基づく正確な値を設定（`false` 固定値から修正）
- `InstalledAppsHelper.kt`: `getLaunchableApps()` で API 33+ の `ResolveInfoFlags.of()` API を使用（deprecated API 対応）
- `InstalledAppsHelper.kt`: 未使用の `private fun isSystemApp(ApplicationInfo)` オーバーロード削除
- `InstalledAppsHelper.kt`: 診断ログ追加（launchable apps 件数）
- `AndroidManifest.xml`: `<queries>` ブロック追加（`ACTION_MAIN + CATEGORY_LAUNCHER`）

**対象ファイル:**

| ファイル | 変更内容 |
|---------|---------|
| `util/InstalledAppsHelper.kt` | システムアプリフィルタ削除、isSystemApp正確値設定、API 33+対応、デッドコード削除、診断ログ |
| `AndroidManifest.xml` | `<queries>` ブロック追加 |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL

---

## 今回のセッション: バグ4 - 集中ロック画面が全画面を覆っていない

### 完了タスク

`LockOverlayService.kt` の4つの根本原因を修正:

| # | 問題 | 修正内容 |
|---|------|---------|
| A | 横向きで右端に隙間 | `FLAG_LAYOUT_NO_LIMITS` 追加、gravity を `TOP or START` に変更 |
| B | 通知バーをスワイプダウンできる | `FLAG_NOT_FOCUSABLE` 削除（フォーカス取得でブロック） |
| C | 画面回転でレイアウト再構築されない | `onConfigurationChanged()` オーバーライド追加 |
| D | タッチが背後のアプリに透過 | `FLAG_NOT_TOUCH_MODAL` 削除 + `setOnTouchListener` で全イベント消費 |

### 変更ファイル

| ファイル | 変更内容 |
|---------|---------|
| `service/LockOverlayService.kt` | `createLayoutParams()` 新設、`setupTouchHandler()` 新設、`onConfigurationChanged()` 追加、`launchApp()` 抽出、`currentLayoutParams` メンバ追加 |

### 追加設定

- API 30+ (`Build.VERSION_CODES.R`): `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`
- API 28+ (`Build.VERSION_CODES.P`): `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`

### ビルド結果

- `compileDebugKotlin` BUILD SUCCESSFUL

### 検証方法（手動テスト）

| テスト項目 | 期待動作 |
|-----------|---------|
| 縦画面でオーバーレイ表示 | 画面全体を隙間なくカバー |
| 横画面でオーバーレイ表示 | 右端含め全画面カバー、landscape レイアウト使用 |
| 縦→横回転 | レイアウト再構築、タイマー文字保持 |
| 横→縦回転 | 同上 |
| 上端からスワイプダウン | 通知バー開かない |
| オーバーレイをタップ | アプリに復帰 |
| オーバーレイをスワイプ | 何も起きない（消費される） |
| ノッチ付きデバイス | ノッチ領域もカバー |

---

## 前回セッション: 修正9件完了

## バグ修正ロードマップ（9件）

| # | 修正内容 | ステータス | 概要 |
|---|---------|-----------|------|
| 1 | 休憩中のロックモード解除 | **完了** | 休憩→ロック解除、作業復帰→自動再ロック |
| 2 | ループモードの改善 | **完了** | 無限ループ廃止→今日の残りタスク順次実行方式 |
| 3 | 横画面レイアウト対応 | **完了** | タイマー・ホーム・オーバーレイの横画面対応 |
| 4 | ホーム画面に現在のタイマー状態を表示 | **完了** | タイマー動作中にミニタイマーバー表示 |
| 5 | 今日の学習時間・サイクルが更新されない | **完了** | Flow ベースのリアクティブ更新に切替 |
| 6 | BGMが再生されない | **完了** | MediaPlayer→AudioTrack+プロシージャル音声生成 |
| 7 | タイマー完了ダイアログの表示タイミング | **完了** | セッション完了時に自動遷移してダイアログ即表示 |
| 8 | 統計画面の学習時間・連続学習が更新されない | **完了** | FinishTimerSessionUseCase に DailyStatsRepository 注入、updateStats() 呼び出し追加 |
| 9 | 集中ロックモード中にアプリがホーム画面に戻る | **完了** | ロック中にアプリ外遷移、復帰時にタイマー画面に戻らない |

### 修正1: 休憩中のロックモード解除 ✅完了

**問題:** `TimerService.onPhaseComplete()` で WORK→SHORT_BREAK/LONG_BREAK 遷移時、`FocusModeService` と `LockOverlayService` のロック解除処理がない。休憩中もスマホ操作がブロックされたままになる。

**実装内容:**
- **WORK → SHORT_BREAK** (line 417-419): `FocusModeService.stopFocusMode()` + `LockOverlayService.hideOverlay()` を追加
- **SHORT_BREAK → WORK** (line 434-441): `FocusModeService.startFocusMode()` + `LockOverlayService.showOverlay()` + `updateOverlayTime()` を追加
- **LONG_BREAK → WORK** (line 456-463): 同上
- **セッション完了**: autoLoop有効でもセッション完了方式に統一（次タスクナビゲーションはUI層で処理）
- **テスト更新**: `TimerServiceTest` の autoLoop テストを新しい動作に合わせて更新

**対象ファイル:** `TimerService.kt`, `TimerServiceTest.kt`

### 修正2: ループモードの改善 ✅完了

**問題:** autoLoop有効時、`onPhaseComplete()` で同じタスクのサイクル1にリセットして永久ループする。ロックモードと組み合わせるとスマホが永久に操作不能になる。

**実装内容:**
- `TimerUiState` に `isAutoLoopSession`, `allTasksCompleted` フィールド追加
- `startTimer()` で `isAutoLoopSession` をセット
- `loadNextTask()` で `allTasksCompleted` を判定（autoLoop有効 + 次タスクなし → true）
- `loadNextTask()` を `internal` に変更（テスト用）
- `FinishDialog` に `allTasksCompleted` パラメータ追加、`stringResource(R.string.timer_all_tasks_completed)` で「全タスク完了！」メッセージ表示
- `TimerScreen` で `allTasksCompleted` を `FinishDialog` に伝播
- `strings.xml` / `strings-en.xml` に文字列リソース追加
- `TimerViewModelTest` に6つの新規テスト追加（loadNextTask + allTasksCompleted）

**対象ファイル:** `TimerViewModel.kt`, `FocusModeDialog.kt`, `TimerScreen.kt`, `strings.xml`, `strings-en.xml`, `TimerViewModelTest.kt`

**テスト結果:** TimerViewModelTest 全テスト PASS（新規6件含む）

### 修正3: 横画面レイアウト対応 ✅完了

**問題:** 横画面（ランドスケープ）で表示領域が足りず、タイマー画面のUIが見切れている。

**実装内容:**
- `CircularTimer` に `timerSize: Dp = 280.dp` パラメータ追加（後方互換、フォントサイズもスケーリング）
- `TimerScreen` に `LocalConfiguration` でランドスケープ検出追加
  - 横画面: `Row` レイアウト（左: CircularTimer 200dp, 右: Column with verticalScroll でコントロール類）
  - 縦画面: 既存の `Column` レイアウトをそのまま維持
  - 共有ラムダ (`onStartTimer`, `onBgmClick`) を抽出して重複排除
- `layout-land/overlay_lock_screen.xml` 新規作成（横並びレイアウト、同一IDでコード変更不要）
- `HomeScreen` は既に `verticalScroll` 済みのため変更不要

**対象ファイル:** `TimerDisplay.kt`, `TimerScreen.kt`, `layout-land/overlay_lock_screen.xml`（新規）

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL

### 修正4: ホーム画面に現在のタイマー状態を表示 ✅完了

**問題:** タイマーが動作中にホーム画面に戻ると、タイマーの存在がわからない。休憩中など特に現在の状態が見えない。

**実装内容:**
- `TimerService` companion に `_activeTimerState: MutableStateFlow<TimerState?>` を追加、全 `_timerState.value = ...` を `updateTimerState()` ヘルパーで置換（running/paused 時のみ公開、IDLE/完了で null）
- `HomeUiState` に `activeTimerState: TimerState?` フィールド追加
- `HomeViewModel` で `TimerService.activeTimerState` を collect
- `ActiveTimerBar.kt` 新規作成: AnimatedVisibility でフェーズアイコン・タスク名・残り時間・進捗バーを表示、タップでタイマー画面遷移
- 文字列リソース6件追加（日本語・英語）
- テスト4件追加

**対象ファイル:**
| ファイル | 変更 |
|---------|------|
| `service/TimerService.kt` | companion に static StateFlow + `updateTimerState()` + 全9箇所置換 + onDestroy cleanup |
| `ui/screens/home/HomeViewModel.kt` | +import, +activeTimerState field, +collectActiveTimerState() |
| `ui/screens/home/HomeScreen.kt` | +ActiveTimerBar import & 呼び出し |
| `ui/screens/home/components/ActiveTimerBar.kt` | **新規** ミニタイマーバー composable |
| `res/values/strings.xml` | +6文字列（日本語） |
| `res/values-en/strings.xml` | +6文字列（英語） |
| `test/ui/screens/home/HomeViewModelTest.kt` | +3 imports, +resetActiveTimerState in setup, +4テスト |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL
**テスト結果:** HomeViewModelTest 全テスト PASS（新規4件含む）

### 修正5: 今日の学習時間・サイクルが更新されない ✅完了

**問題:** `HomeViewModel.loadHomeData()` は `init {}` で1回だけ実行され、タイマー完了後にホーム画面へ戻ると同じ ViewModel インスタンスが再利用されるため、最新のセッションデータが画面に反映されない。`getTotalMinutesForDay()` / `getTotalCyclesForDay()` は `suspend fun`（ワンショット）で Room の `Flow` ではないため DB 変更を自動検知できなかった。

**実装内容:**
- `StudySessionDao` に `observeTotalMinutesForDay()` / `observeTotalCyclesForDay()` Flow クエリ2件追加
- `StudySessionRepository` インターフェースに Flow メソッド2件追加
- `StudySessionRepositoryImpl` に Flow 実装2件追加
- `HomeViewModel.loadHomeData()` でワンショット `getTotalMinutesForDay/getCycles` → `combine(observeMinutes, observeCycles).collect` に置換
- `loadJob: Job?` フィールド追加、再呼び出し時に前回の Job をキャンセル
- `FakeStudySessionRepository` に Flow メソッド2件追加
- `HomeViewModelTest` の mock を `coEvery { ...getTotalMinutesForDay... }` → `every { ...observeTotalMinutesForDay... } returns flowOf(...)` に全件更新
- リアクティブ更新テスト追加（MutableStateFlow で値変更→即座に UI 反映を検証）

**対象ファイル:**
| ファイル | 変更 |
|---------|------|
| `data/local/dao/StudySessionDao.kt` | +2 Flow クエリ |
| `domain/repository/StudySessionRepository.kt` | +2 Flow メソッドシグネチャ |
| `data/repository/StudySessionRepositoryImpl.kt` | +2 Flow メソッド実装 |
| `ui/screens/home/HomeViewModel.kt` | loadJob + combine/collect |
| `test/fakes/FakeStudySessionRepository.kt` | +2 Flow メソッド |
| `test/ui/screens/home/HomeViewModelTest.kt` | mock 更新 + リアクティブテスト追加 |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL
**テスト結果:** HomeViewModelTest 全テスト PASS（リアクティブ更新テスト含む）

### 修正6: BGMが再生されない ✅完了

**問題:** 全5つの BGM ファイル (`res/raw/bgm_*.mp3`) が 1 バイトのプレースホルダーだったため `MediaPlayer.create()` が null を返し、音が出なかった。副次的に `setAudioAttributes()` を `MediaPlayer.create()` 後に呼んでいたため `IllegalStateException` の可能性もあった。

**実装内容: MediaPlayer → AudioTrack + プロシージャル音声生成**
- `BgmTrack` モデルから `@RawRes resourceId` を削除、`generatorType: AudioGeneratorType` に置換
- `AudioGeneratorType` enum 追加 (WHITE_NOISE, RAIN, FOREST, WAVES, LOFI_STUDY)
- `AudioGenerator` インターフェース + 5つの実装クラス新規作成:
  - `WhiteNoiseGenerator`: ガウシアンランダムサンプル
  - `RainGenerator`: ブラウンノイズ（ランダムウォーク + リークファクター）
  - `ForestGenerator`: ピンクノイズ（Voss-McCartney アルゴリズム）
  - `WavesGenerator`: ブラウンノイズ + 正弦波振幅変調（~8秒周期）
  - `LoFiGenerator`: 正弦波コード進行(C→Am→F→Dm) + ローパスフィルタ + 微小ノイズ
- `AudioGeneratorFactory`: AudioGeneratorType → AudioGenerator インスタンス生成
- `BgmService`: MediaPlayer を完全削除、AudioTrack (44100Hz, Mono, PCM_16BIT, MODE_STREAM) + 生成スレッドに置換
- AudioFocus 追加 (GAIN/LOSS/TRANSIENT/DUCK)
- `BgmManager.play()`: `track.resourceId` → `track.generatorType.name` に変更
- 1バイトプレースホルダー MP3 ファイル5件削除
- `BgmManagerTest`: mock シグネチャ更新 (`any<Int>()` → `any<String>()`)

**対象ファイル:**
| ファイル | 変更 |
|---------|------|
| `domain/model/BgmTrack.kt` | resourceId → generatorType, AudioGeneratorType enum 追加 |
| `service/audio/AudioGenerator.kt` | **新規** インターフェース + 5実装 + Factory |
| `service/BgmService.kt` | MediaPlayer → AudioTrack + AudioGenerator + AudioFocus |
| `ui/bgm/BgmManager.kt` | play() 引数変更 |
| `res/raw/bgm_*.mp3` (5件) | **削除** |
| `test/ui/bgm/BgmManagerTest.kt` | mock シグネチャ更新 |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL
**テスト結果:** BgmManagerTest 全テスト PASS (10件)

### 修正7: タイマー完了ダイアログの表示タイミング ✅完了

**問題:** タイマーでポモドーロセッションを完了した際、「お疲れ様でした！」ダイアログが完了した瞬間に表示されない。ユーザーが TimerScreen から離脱すると ViewModel が破棄され、セッション完了時にダイアログを表示する相手がいないため、再度タイマー画面を開いた時に初めてダイアログが表示されていた。

**実装内容: TimerService companion に sessionCompletedTaskId StateFlow を追加し、IterioNavHost で監視→自動遷移**
- `TimerService` companion に `_sessionCompletedTaskId: MutableStateFlow<Long?>` + `sessionCompletedTaskId: StateFlow<Long?>` + `consumeSessionCompletedEvent()` を追加
- `onPhaseComplete()` の最終サイクル完了ブロックで `_sessionCompletedTaskId.value = state.taskId` を設定
- `stopTimerInternal()` と `onDestroy()` で `_sessionCompletedTaskId.value = null` をクリア
- `IterioNavHost` に `LaunchedEffect` を追加し、`sessionCompletedTaskId` が non-null になったら `navController.navigate(Screen.Timer.createRoute(taskId)) { launchSingleTop = true }` で自動遷移後、`consumeSessionCompletedEvent()` でイベントを消費

**動作フロー:**
- ユーザーが TimerScreen に居る場合 → ViewModel が直接 collect → ダイアログ即表示。`launchSingleTop` により重複ナビゲーションは no-op
- ユーザーが別画面に居る場合 → IterioNavHost が検知して TimerScreen に自動遷移 → ダイアログ表示

**対象ファイル:**
| ファイル | 変更 |
|---------|------|
| `service/TimerService.kt` | companion に sessionCompletedTaskId StateFlow + consume + onPhaseComplete/stopTimerInternal/onDestroy 更新 |
| `ui/navigation/IterioNavHost.kt` | LaunchedEffect で sessionCompletedTaskId 監視 → 自動遷移 |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL
**テスト結果:** TimerViewModelTest 全テスト PASS、TimerServiceTest 全テスト PASS

### 修正8: 統計画面の学習時間・連続学習が更新されない ✅完了

**問題:** `FinishTimerSessionUseCase` がセッション完了時に `study_sessions` テーブルのみ更新し、`daily_stats` テーブルを一切更新していなかった。`StatsViewModel` は週間・月間学習時間、連続学習日数、週間チャートデータをすべて `daily_stats` テーブルから読み取るため、常に 0 が返されていた。

**根本原因:** `FinishTimerSessionUseCase` に `DailyStatsRepository` が注入されておらず、`updateStats()` が呼ばれていなかった。

**実装内容:**
- `FinishTimerSessionUseCase` に `DailyStatsRepository` を注入
- `invoke()` 内で `taskRepository.updateLastStudiedAt()` の後に `dailyStatsRepository.updateStats()` を呼び出し
- テストに `dailyStatsRepository` mock 追加 + 新規テスト3件追加

**対象ファイル:**
| ファイル | 変更 |
|---------|------|
| `domain/usecase/FinishTimerSessionUseCase.kt` | `DailyStatsRepository` 注入 + `updateStats()` 呼び出し |
| `test/domain/usecase/FinishTimerSessionUseCaseTest.kt` | mock 追加 + 新規テスト3件 |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL
**テスト結果:** FinishTimerSessionUseCaseTest 全テスト PASS（既存10件 + 新規3件）、StatsViewModelTest 全テスト PASS

### 修正9: 集中ロックモード中にアプリがホーム画面に戻ってしまう ✅完了

**問題:** タイマーの集中ロックモード（完全ロック）中に、スマホのホーム画面に遷移してしまう。休憩時間になってアプリを再度開くと、タイマー画面ではなくアプリのホーム画面（ホームタブ）が表示される。

**根本原因3つ:**
1. `SystemPackages.ALWAYS_ALLOWED` にランチャーが含まれ、ストリクトモードでもHome押下がブロックされない
2. `returnToApp()` が MainActivity を起動するが、NavController が最後の画面（ホームタブ）を復元
3. 休憩→作業遷移時に画面遷移しない

**実装内容:**
- `SystemPackages.kt`: `STRICT_MODE_ALLOWED` 定数追加（`SYSTEM_UI + EMERGENCY`、ランチャー除外）
- `FocusModeService.startFocusMode()`: `strictMode` 時に `STRICT_MODE_ALLOWED` を使用
- `IterioNavHost.kt`: `repeatOnLifecycle(RESUMED)` + `combine(isStrictMode, activeTimerState)` で、ストリクトモード中にアプリ復帰 or 休憩→作業遷移時にタイマー画面へ自動ナビゲーション
- `FocusModeServiceTest.kt`: 新規テスト8件

**対象ファイル:**
| ファイル | 変更 |
|---------|------|
| `util/SystemPackages.kt` | `STRICT_MODE_ALLOWED` 追加 |
| `service/FocusModeService.kt` | `startFocusMode()` でストリクトモード時にランチャー除外 |
| `ui/navigation/IterioNavHost.kt` | ストリクトモード中の自動タイマー画面遷移 |
| `test/service/FocusModeServiceTest.kt` | **新規** 8テスト |

**ビルド結果:** `compileDebugKotlin` BUILD SUCCESSFUL
**テスト結果:** FocusModeServiceTest 全テスト PASS（8件）

---

## 前回セッション: バグ修正3件（完了）

### バグ修正ロードマップ（3件）

| # | 修正内容 | ステータス | 対象ファイル |
|---|---------|-----------|-------------|
| 1 | ロックオーバーレイ背景の完全不透明化 | **完了** | `overlay_lock_screen.xml` |
| 2 | 休憩中のロックモード解除・作業復帰時の自動再ロック | **完了** | `TimerService.kt` |
| 3 | ループモード改善（今日のタスク順次実行方式） | **完了** | `TimerService.kt`, `TimerViewModel.kt`, `TimerScreen.kt`, `FocusModeDialog.kt`, `IterioNavHost.kt` |

### 変更ファイル一覧

| ファイル | 変更内容 |
|---------|---------|
| `res/layout/overlay_lock_screen.xml` | 背景色 #E6→#FF |
| `service/TimerService.kt` | 休憩中ロック解除、autoLoop無限ループ削除 |
| `ui/screens/timer/TimerViewModel.kt` | nextTask state追加、TaskRepository inject、loadNextTask() |
| `ui/screens/timer/TimerScreen.kt` | onNavigateToNextTask コールバック追加 |
| `ui/screens/timer/components/FocusModeDialog.kt` | FinishDialog に次タスクボタン追加 |
| `ui/navigation/IterioNavHost.kt` | 次タスクナビゲーション配線 |
| `test/.../TimerViewModelTest.kt` | taskRepository mock追加 |

### テスト結果
- TimerViewModelTest: **ALL PASSED**
- SettingsViewModelTest: 64 failures（既存の問題、本変更とは無関係）

### 既知の問題
- `SettingsViewModelTest` で64テスト失敗（line 80 NullPointerException、既存問題）

---

## Previous Session: Completed

## Completed Tasks

### feat: グループ期限追加 + ホーム画面期限セクション改善

**概要:** 2つの機能を実装:
1. SubjectGroup に期限フィールド追加（ON/OFF トグル + 日付ピッカー）
2. ホーム画面「期限が近いタスク」セクション改善（タスク+グループ統合、最大3件、専用一覧画面）

**実装内容:**

#### Phase 1: データレイヤー
- DB Migration 7→8: `hasDeadline`, `deadlineDate` カラム追加
- `SubjectGroupEntity` / `SubjectGroup` ドメインモデルに2フィールド追加
- `SubjectGroupMapper` 更新
- `SubjectGroupDao.getUpcomingDeadlineGroups()` クエリ追加
- `SubjectGroupRepository` / `SubjectGroupRepositoryImpl` にメソッド追加
- `IterioDatabase` バージョン8、`DatabaseModule` に migration 登録
- `BackupData` / `BackupRepositoryImpl` に期限フィールド追加

#### Phase 2: グループダイアログ UI
- `GroupDialogs.kt`: AddGroupDialog / EditGroupDialog に期限トグル + DatePicker 追加
- `TasksViewModel.addGroup()` に `hasDeadline`, `deadlineDate` 引数追加
- `TasksEvent.AddGroup` に引数追加
- `TasksScreen` のコールバック更新

#### Phase 3: DeadlineItem sealed class
- `DeadlineItem` sealed class 新規作成
  - `TaskDeadline`: タスクの期限（再生ボタン付き）
  - `GroupDeadline`: グループの期限（フォルダアイコン）

#### Phase 4: ホーム画面の期限セクション改善
- `HomeUiState`: `upcomingDeadlineTasks` → `upcomingDeadlineItems` + `totalDeadlineCount`
- `HomeViewModel`: `SubjectGroupRepository` inject、`combine` で統合ロード、`take(3)`
- `UpcomingDeadlinesSection`: `DeadlineItem` ベースにリファクタ、「すべて見る」リンク追加
- `HomeScreen`: `onNavigateToDeadlineList` コールバック追加

#### Phase 5: 期限一覧画面
- `Screen.DeadlineList` ルート追加
- `DeadlineListViewModel`: combine + filter (ALL/TASKS/GROUPS)
- `DeadlineListScreen`: TopAppBar, SummaryRow, FilterChip, LazyColumn
- `IterioNavHost` にルート登録 + HomeScreen コールバック接続

#### Phase 6: テスト
- `FakeSubjectGroupRepository`: `getUpcomingDeadlineGroups` 実装追加
- `HomeViewModelTest`: `SubjectGroupRepository` mock 追加、deadline テスト3件追加/更新
- `DeadlineListViewModelTest`: 新規8テスト（初期状態、ロード、フィルタ、ソート、空状態）
- `TestAppModule`: `getUpcomingDeadlineGroups` mock 追加

## Changed Files

### New Files (4)
| File | Content |
|------|---------|
| `data/local/migration/Migration_7_8.kt` | DB migration (hasDeadline, deadlineDate) |
| `domain/model/DeadlineItem.kt` | 統一 sealed class |
| `ui/screens/deadline/DeadlineListViewModel.kt` | 期限一覧 ViewModel |
| `ui/screens/deadline/DeadlineListScreen.kt` | 期限一覧画面 |
| `test/ui/screens/deadline/DeadlineListViewModelTest.kt` | テスト (8件) |

### Modified Files (22)
| File | Change |
|------|--------|
| `data/local/entity/SubjectGroupEntity.kt` | +hasDeadline, +deadlineDate |
| `domain/model/SubjectGroup.kt` | +hasDeadline, +deadlineDate |
| `data/mapper/SubjectGroupMapper.kt` | マッピング追加 |
| `data/local/dao/SubjectGroupDao.kt` | +getUpcomingDeadlineGroups クエリ |
| `domain/repository/SubjectGroupRepository.kt` | +getUpcomingDeadlineGroups |
| `data/repository/SubjectGroupRepositoryImpl.kt` | +実装 |
| `data/local/IterioDatabase.kt` | version 7→8 |
| `di/DatabaseModule.kt` | +MIGRATION_7_8 |
| `domain/model/BackupData.kt` | +SubjectGroupBackup フィールド |
| `data/repository/BackupRepositoryImpl.kt` | +backup マッピング |
| `ui/screens/tasks/components/GroupDialogs.kt` | 期限トグル+DatePicker |
| `ui/screens/tasks/TasksViewModel.kt` | addGroup 引数追加 |
| `ui/screens/tasks/TasksEvent.kt` | AddGroup 引数追加 |
| `ui/screens/tasks/TasksScreen.kt` | コールバック更新 |
| `ui/screens/home/HomeViewModel.kt` | combine + DeadlineItem |
| `ui/screens/home/HomeScreen.kt` | +onNavigateToDeadlineList |
| `ui/screens/home/components/UpcomingDeadlinesSection.kt` | DeadlineItem ベースにリファクタ |
| `ui/navigation/Screen.kt` | +DeadlineList |
| `ui/navigation/IterioNavHost.kt` | +ルート登録 |
| `res/values/strings.xml` | +19文字列 |
| `res/values-en/strings.xml` | +19文字列 |
| `test/fakes/FakeSubjectGroupRepository.kt` | +getUpcomingDeadlineGroups |
| `test/ui/screens/home/HomeViewModelTest.kt` | +SubjectGroupRepository mock, +3テスト |
| `androidTest/di/TestAppModule.kt` | +getUpcomingDeadlineGroups mock |

## Test Results

- Unit Tests: **1055 PASSED**, 1 failed (pre-existing `SettingsRepositoryImplTest` failure, unrelated)
- New HomeViewModelTest deadline tests: **ALL PASSED** (3 new tests)
- DeadlineListViewModelTest: **ALL PASSED** (8 tests)

## Architecture Changes

- `DeadlineItem` sealed class を導入し、タスクの期限とグループの期限を統一的に扱えるようにした
- `HomeViewModel` で `combine` を使用して2つの Flow を統合
- 専用の `DeadlineListScreen` + `DeadlineListViewModel` で一覧表示

## Next Actions

1. 手動確認: グループ追加ダイアログで期限トグルON → 日付選択 → 保存 → 再表示で値保持
2. ホーム画面: 期限付きタスク+グループが日付順に混在表示されること
3. 3件超の場合「すべて見る」が表示され、タップで期限一覧画面に遷移
4. フィルタ (すべて/タスク/グループ) が正しく動作
5. バックアップ: エクスポート/インポートで期限データ保持

## Known Issues

- `SettingsRepositoryImplTest.getPomodoroSettings returns default values when no settings exist` が失敗（本変更とは無関係、既存の問題）
