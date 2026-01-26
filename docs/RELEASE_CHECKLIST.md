# Iterio リリースチェックリスト

## 完了済み項目

| 項目 | 状態 | 備考 |
|------|------|------|
| アプリアイコン (512x512) | ✅ | `docs/store-assets/iterio_icon.png` |
| フィーチャーグラフィック (1024x500) | ✅ | `docs/store-assets/iterio_feature_graphic_1024x500.png` |
| ストア掲載テキスト（日本語） | ✅ | `docs/STORE_LISTING.md` |
| ストア掲載テキスト（英語） | ✅ | `docs/STORE_LISTING.md` |
| プライバシーポリシー | ✅ | `docs/PRIVACY_POLICY.md` + `docs/privacy-policy.html` |
| 言語切り替え機能 | ✅ | 日本語/English 対応済み |
| 署名キー設定 | ✅ | `secrets.properties` で管理 |
| リリースビルド | ✅ | ProGuard/R8 有効 |
| セキュリティ修正 | ✅ | 秘密情報を `secrets.properties` に移動 |

---

## 未完了項目

### 1. Google Play Console 公開鍵の設定（必須）

**手順:**
1. [Google Play Console](https://play.google.com/console) にログイン
2. アプリを選択（まだ作成していない場合は作成）
3. 左メニュー → **収益化** → **収益化の設定**
4. **ライセンス** セクションの Base64 公開鍵をコピー
5. `secrets.properties` を開く
6. `BILLING_PUBLIC_KEY=` の後に公開鍵を貼り付け

```properties
# secrets.properties
BILLING_PUBLIC_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...（コピーした鍵）
```

**重要:** 公開鍵がないと課金機能が動作しません。

---

### 2. スクリーンショットの撮影（必須）

**必要枚数:** 最低2枚、推奨4〜8枚

**推奨サイズ:**
- スマートフォン: 1080×1920 または 1080×2400
- 7インチタブレット（任意）: 1200×1920
- 10インチタブレット（任意）: 1800×2560

**撮影対象:**
1. ホーム画面（今日のタスク・復習リスト表示）
2. タイマー画面（作業中の状態）
3. 統計画面（学習時間グラフ）
4. カレンダー画面（ヒートマップ表示）
5. 設定画面（言語選択を含む）
6. フォーカスモード設定
7. Premium機能紹介
8. ウィジェット表示

**撮影手順:**
```bash
# エミュレータで撮影する場合
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png ./docs/store-assets/screenshot_01.png
```

**保存先:** `docs/store-assets/` フォルダ

---

### 3. Google Cloud Console 設定（クラウドバックアップ用）

Google Drive バックアップ機能を有効にするには、OAuth 2.0 の設定が必要です。

**手順:**

1. **プロジェクト作成**
   - [Google Cloud Console](https://console.cloud.google.com/) にアクセス
   - 新しいプロジェクトを作成（例: `iterio-app`）

2. **Google Drive API 有効化**
   - APIs & Services → Library → "Google Drive API" を検索
   - 「有効にする」をクリック

3. **OAuth 同意画面の設定**
   - APIs & Services → OAuth consent screen
   - User Type: External
   - アプリ名: Iterio
   - サポートメール: iterio.timer.app.help@gmail.com
   - スコープ: `https://www.googleapis.com/auth/drive.file`

4. **OAuth 2.0 クライアントIDの作成**
   - APIs & Services → Credentials → Create Credentials → OAuth client ID
   - Application type: Android
   - Package name: `com.iterio.app`
   - SHA-1 フィンガープリント（下記参照）

**SHA-1 フィンガープリントの取得:**
```bash
# リリースキーストアから
keytool -list -v -keystore iterio-release.jks -alias iterio
# SHA1: XX:XX:XX:... の行をコピー

# デバッグキーストアから（開発用）
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

---

### 4. 実機テスト

**テスト項目チェックリスト:**

#### 基本機能
- [ ] アプリ起動・ホーム画面表示
- [ ] タイマー開始・一時停止・完了
- [ ] 休憩タイマー自動開始
- [ ] 通知表示（作業完了・休憩完了）

#### フォーカスモード
- [ ] フォーカスモード有効化
- [ ] 他アプリへのアクセスブロック確認
- [ ] 許可アプリの動作確認
- [ ] 厳格モードのロック動作

#### 学習記録
- [ ] 学習セッションの記録
- [ ] 復習リマインダーの生成
- [ ] 統計グラフの表示

#### Premium機能（テストトラック使用）
- [ ] 購入フロー
- [ ] 購入後の機能解放
- [ ] 復元機能

#### その他
- [ ] 言語切り替え（日本語⇔英語）
- [ ] バックアップ/復元
- [ ] ウィジェット表示・更新

---

## Google Play Console リリース手順

### ステップ 1: アプリの作成

1. [Google Play Console](https://play.google.com/console) にログイン
2. 「アプリを作成」をクリック
3. 以下を入力:
   - アプリ名: `Iterio - 集中力向上&学習管理`
   - デフォルト言語: 日本語
   - アプリまたはゲーム: アプリ
   - 無料または有料: 無料（アプリ内課金あり）
4. 宣言事項に同意してアプリを作成

### ステップ 2: ストア掲載情報の入力

**メインのストアの掲載情報:**
- 簡単な説明: `docs/STORE_LISTING.md` から日本語版をコピー
- 詳しい説明: 同上
- アプリアイコン: `docs/store-assets/iterio_icon.png` をアップロード
- フィーチャーグラフィック: `docs/store-assets/iterio_feature_graphic_1024x500.png` をアップロード
- スクリーンショット: 撮影したものをアップロード

### ステップ 3: アプリのコンテンツ設定

1. **プライバシーポリシー**
   - URL: プライバシーポリシーをホスティングしたURL
   - 例: GitHub Pages, Firebase Hosting など

2. **広告**
   - 「このアプリには広告が含まれていますか？」→ いいえ

3. **アプリのアクセス**
   - すべての機能は特別なアクセスなしで利用可能

4. **コンテンツのレーティング**
   - 質問票に回答（暴力なし、性的コンテンツなし等）
   - 結果: IARC 3+（全年齢）

5. **ターゲットユーザー**
   - 対象年齢: 13歳以上
   - 子ども向けの訴求なし

6. **データセーフティ**
   - 収集するデータ: なし（ローカル保存のみ）
   - サーバーへの送信: なし
   - 暗号化: はい（AES-256でバックアップを暗号化）

### ステップ 4: リリース設定

1. **App Signing**
   - Google Play App Signing に登録（推奨）
   - またはアップロード鍵を使用

2. **AAB のビルドとアップロード**
```bash
# AAB（Android App Bundle）をビルド
./gradlew bundleRelease

# 出力先: app/build/outputs/bundle/release/app-release.aab
```

3. **リリーストラック**
   - 内部テスト → クローズドテスト → オープンテスト → 本番
   - まず内部テストで動作確認を推奨

---

## AAB ビルドコマンド

```bash
# クリーンビルド
./gradlew clean bundleRelease

# 出力先確認
ls -la app/build/outputs/bundle/release/
```

---

## 重要な注意事項

1. **キーストアのバックアップ**
   - `iterio-release.jks` を安全な場所にバックアップ
   - 紛失するとアプリのアップデートが不可能に

2. **secrets.properties のバックアップ**
   - パスワードを忘れないよう別途保管
   - このファイルは Git にコミットされない

3. **バージョン管理**
   - リリースごとに `versionCode` を増加（現在: 1）
   - `versionName` も適宜更新（現在: 1.0.0）

---

## 連絡先

- サポートメール: iterio.timer.app.help@gmail.com
