# HANDOVER.md

## Session Status: Completed (修正9完了)

## 次セッションのバグ修正ロードマップ（9件）

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
