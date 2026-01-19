# ZENITH 開発申し送り書

**作成日時:** 2026-01-19
**現在のステータス:** ビルド成功 ✅
**ビルド時間:** BUILD SUCCESSFUL in 1m 25s

---

## 今回のセッションで完了したタスク

### 1. セキュリティ強化

#### 1.1 SignatureVerifier.kt 改善
- **セキュリティホール修正**: `return true` → `return false`（公開鍵未設定時）
- **署名アルゴリズム強化**: `SHA1withRSA` → `SHA256withRSA`
- **BuildConfig対応**: 公開鍵をgradle.propertiesから取得
- **ログ機能追加**: エラー/警告の詳細ログ出力
- **テスト対応**: ログラッパー関数でユニットテスト環境対応

#### 1.2 バックアップ暗号化機能【新規】
**ファイル:** `data/encryption/EncryptionManager.kt`

- **暗号化方式**: AES-256-GCM
- **キー管理**: Android KeyStoreでセキュア保存
- **データ形式**: NONCE(12bytes) + 暗号化データ + 認証タグ
- **後方互換性**: 平文JSONバックアップも読み込み可能

### 2. Billing機能改善

#### 2.1 BillingClientWrapper.kt
- 詳細な購入状態ハンドリング（when式）
- ユーザーキャンセル、既に所有などの状態分離
- ログ出力強化

#### 2.2 BillingUseCase.kt
- **失効日計算改善**: `expiryTimeMillis`から正確な期限取得
- フォールバック処理を`calculateExpiryFromPurchaseTime()`に分離

### 3. バックアップ機能改善

#### 3.1 GoogleDriveManager.kt
- EncryptionManager統合
- アップロード時に自動暗号化
- ダウンロード時に暗号化判定＆復号化
- 後方互換性対応（平文JSONサポート継続）

#### 3.2 BackupRepositoryImpl.kt
- ローカルバックアップも暗号化対応
- EncryptionException ハンドリング

### 4. UI改善

#### 4.1 TimerScreen.kt
- BGM選択UI: AlertDialog → ModalBottomSheet
- より良いUX（ヘッダー、ディバイダー調整）

#### 4.2 TimerViewModel.kt
- タイマー開始/再開時のBGM連動強化

### 5. ビルド設定改善

#### 5.1 build.gradle.kts
- `buildConfig = true` 追加
- `BILLING_PUBLIC_KEY` BuildConfigフィールド追加
- ProGuard有効化（`isMinifyEnabled = true`, `isShrinkResources = true`）
- テスト依存関係追加（JUnit, MockK, Turbine, Coroutines-test）

#### 5.2 proguard-rules.pro
- Google Play Billing保持ルール
- Google Drive API保持ルール
- Google Auth保持ルール
- Glance Widget保持ルール

#### 5.3 libs.versions.toml
- テストライブラリバージョン追加

### 6. テスト

#### 6.1 SignatureVerifierTest.kt【新規】
- 空文字列検証テスト
- 公開鍵未設定時の検証テスト
- isKeyConfigured()テスト

---

## 変更ファイル一覧

### 修正ファイル（8件）
| ファイル | 変更内容 |
|---------|---------|
| `data/billing/BillingClientWrapper.kt` | 状態ハンドリング強化、ログ追加 |
| `data/billing/SignatureVerifier.kt` | SHA256、BuildConfig対応、セキュリティ修正 |
| `data/cloud/GoogleDriveManager.kt` | 暗号化対応、後方互換性 |
| `data/repository/BackupRepositoryImpl.kt` | 暗号化対応 |
| `di/AppModule.kt` | EncryptionManager DI追加 |
| `domain/usecase/BillingUseCase.kt` | 失効日計算改善 |
| `ui/screens/timer/TimerScreen.kt` | BGM選択UI改善 |
| `ui/screens/timer/TimerViewModel.kt` | BGM連動強化 |

### 新規ファイル（2件）
| ファイル | 内容 |
|---------|------|
| `data/encryption/EncryptionManager.kt` | AES-256-GCM暗号化マネージャー |
| `test/.../SignatureVerifierTest.kt` | 署名検証ユニットテスト |

---

## 未コミットの変更

```
M app/src/main/java/com/zenith/app/data/billing/BillingClientWrapper.kt
M app/src/main/java/com/zenith/app/data/billing/SignatureVerifier.kt
M app/src/main/java/com/zenith/app/data/cloud/GoogleDriveManager.kt
M app/src/main/java/com/zenith/app/data/repository/BackupRepositoryImpl.kt
M app/src/main/java/com/zenith/app/di/AppModule.kt
M app/src/main/java/com/zenith/app/domain/usecase/BillingUseCase.kt
M app/src/main/java/com/zenith/app/ui/screens/timer/TimerScreen.kt
M app/src/main/java/com/zenith/app/ui/screens/timer/TimerViewModel.kt
?? app/src/main/java/com/zenith/app/data/encryption/
```

---

## 手動作業が必要な項目

### 必須（リリース前）

| タスク | 手順 |
|-------|------|
| **Google Play Console公開鍵設定** | 1. Google Play Console → 収益化の設定 → ライセンス<br>2. Base64公開鍵をコピー<br>3. `gradle.properties`に`BILLING_PUBLIC_KEY=...`を追加 |
| **Google Cloud Console設定** | 1. プロジェクト作成<br>2. Google Drive API有効化<br>3. OAuth 2.0クライアントID作成（Android）<br>4. OAuth同意画面設定 |
| **実機テスト** | Premium購入フロー、バックアップ/復元の動作確認 |

---

## 技術スタック

| 項目 | バージョン |
|------|-----------|
| Compile SDK | 35 |
| Min SDK | 26 |
| Target SDK | 35 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Hilt | 2.53.1 |
| Room | 2.6.1 |
| Google Play Billing | 7.0.0 |
| Java | 17 |

---

## ビルドコマンド

```bash
cd C:/Users/hikit/projects/ZENITH
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.17.10-hotspot"
./gradlew.bat assembleDebug
```

---

## 実装済み機能一覧

| 機能 | 状態 | Premium |
|------|------|---------|
| ポモドーロタイマー | ✅ | - |
| タスク管理 | ✅ | - |
| 教科グループ | ✅ | - |
| 学習統計 | ✅ | - |
| カレンダー | ✅ | - |
| 復習リマインダー | ✅ | - |
| フォーカスモード | ✅ | ✅ |
| BGM再生 | ✅ | ✅ |
| ローカルバックアップ | ✅ | ✅ |
| クラウドバックアップ | ✅ | ✅ |
| **バックアップ暗号化** | ✅ NEW | ✅ |
| Google Play Billing | ✅ | - |
| 署名検証（SHA256） | ✅ | - |

---

## 次のアクション候補

1. **コミット作成**: 今回の変更をGitにコミット
2. **追加テスト作成**: EncryptionManager、BillingUseCaseのユニットテスト
3. **忘却曲線UI実装**: ドメイン層は実装済み、UI未実装
4. **ウィジェット機能強化**: 基本実装あり、機能拡張可能

---

## 注意事項

- **暗号化キー**: Android KeyStoreに保存されるため、アプリ再インストール時はキーが失われ、古い暗号化バックアップは復号不可
- **後方互換性**: 平文JSONバックアップも引き続き読み込み可能（移行期間対応）
- **署名検証**: 公開鍵未設定時は購入が拒否されるため、リリース前に必ず設定必要
