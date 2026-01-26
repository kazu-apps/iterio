# Iterio Google Play公開 - Web版Claude申し送り

## プロジェクト概要
**Iterio** - ポモドーロタイマー×忘却曲線の学習管理Androidアプリ

---

## 完了済み作業

### コード・ビルド関連
- [x] アプリ開発完了
- [x] 署名設定（キーストア作成済み）
- [x] テスト実行・修正（86テストパス）
- [x] リリースAPKビルド（4.6 MB）
- [x] リリースAABビルド（8.5 MB）
  - 場所: `app/build/outputs/bundle/release/app-release.aab`

### ドキュメント関連
- [x] プライバシーポリシー作成
  - Markdown: `docs/PRIVACY_POLICY.md`
  - HTML: `docs/privacy-policy.html`（GitHub Pages用）
- [x] ストア掲載テキスト作成: `docs/STORE_LISTING.md`
  - 簡単な説明（38文字）
  - 詳しい説明（約1,100文字）
  - 英語版も用意済み

---

## これからやること（順番通り）

### Step 1: GitHubアカウント作成
1. https://github.com にアクセス
2. 「Sign up」でアカウント作成
3. メール認証を完了

### Step 2: GitHubリポジトリ作成
1. 右上の「+」→「New repository」
2. Repository name: `iterio`（または任意の名前）
3. Public を選択
4. 「Create repository」をクリック

### Step 3: コードをプッシュ
ローカルのターミナルで実行:
```bash
cd C:/Users/hikit/projects/Iterio
git add .
git commit -m "Google Play ストア公開準備"
git remote add origin https://github.com/[ユーザー名]/iterio.git
git branch -M main
git push -u origin main
```

### Step 4: GitHub Pages有効化
1. リポジトリページ → **Settings**（タブ）
2. 左メニュー → **Pages**
3. Source: 「Deploy from a branch」
4. Branch: 「main」を選択、フォルダは「/docs」を選択
5. **Save**をクリック
6. 数分待つとURLが表示される

プライバシーポリシーURL例:
`https://[username].github.io/iterio/privacy-policy.html`

### Step 5: Google Play デベロッパー登録
1. https://play.google.com/console にアクセス
2. Googleアカウントでログイン
3. 「デベロッパーとして登録」
4. 登録料 $25 USD を支払い
5. 本人確認情報を入力
※審査に数日かかる場合あり

### Step 6: Play Consoleでアプリ作成
1. 「アプリを作成」をクリック
2. 入力項目:
   - アプリ名: `Iterio`
   - デフォルト言語: `日本語`
   - アプリまたはゲーム: `アプリ`
   - 有料/無料: `無料`
3. デベロッパープログラムポリシーに同意

### Step 7: ストア掲載情報入力
`docs/STORE_LISTING.md` の内容をコピペ:
- 簡単な説明
- 詳しい説明

### Step 8: 画像素材アップロード
**必要な画像**（別途作成が必要）:
| 項目 | サイズ |
|------|--------|
| アプリアイコン | 512x512 PNG |
| フィーチャーグラフィック | 1024x500 PNG |
| スクリーンショット | 最低2枚（スマホサイズ） |

### Step 9: コンテンツのレーティング
質問に回答（すべて「なし」で全年齢対象）:
- 暴力的コンテンツ: なし
- 性的コンテンツ: なし
- 言葉遣い: なし
- ユーザー生成コンテンツ: なし

### Step 10: アプリのコンテンツ設定
- プライバシーポリシーURL: GitHub PagesのURL
- 広告: なし
- カテゴリ: 仕事効率化

### Step 11: アプリ内課金設定
「収益化」→「アプリ内アイテム」→「定期購入を作成」:

| 商品ID | 名前 | 価格例 |
|--------|------|--------|
| iterio_premium_monthly | Premium月額 | ¥300/月 |
| iterio_premium_yearly | Premium年額 | ¥2,400/年 |
| iterio_premium_lifetime | Premium買い切り | ¥4,800 |

### Step 12: BILLING_PUBLIC_KEY取得
1. 「収益化の設定」→「ライセンス」
2. 「Base64でエンコードされたRSA公開鍵」をコピー
3. `gradle.properties` に設定:
```properties
BILLING_PUBLIC_KEY=MIIBIjANBgkqhki...（コピーした鍵）
```
4. AABを再ビルド: `./gradlew bundleRelease`

### Step 13: AABアップロード
1. 「リリース」→「テスト」→「内部テスト」
2. 「新しいリリースを作成」
3. `app-release.aab` をアップロード
4. リリースノートを入力
5. 「審査開始」

---

## 重要なファイルの場所

```
Iterio/
├── app/build/outputs/
│   ├── apk/release/app-release.apk  (4.6 MB)
│   └── bundle/release/app-release.aab (8.5 MB)
├── docs/
│   ├── PRIVACY_POLICY.md
│   ├── privacy-policy.html  ← GitHub Pages用
│   └── STORE_LISTING.md     ← ストア掲載テキスト
└── gradle.properties        ← BILLING_PUBLIC_KEY設定場所
```

---

## ストア掲載テキスト（コピペ用）

### 簡単な説明
```
集中力を高めるポモドーロタイマー×忘却曲線で効率的な学習をサポート
```

### 詳しい説明
`docs/STORE_LISTING.md` を参照

---

## 注意事項
- デベロッパー登録は審査に数日かかる場合あり
- 課金テストには「ライセンステスター」の設定が必要
- 本番リリース前に必ず内部テスト→クローズドテストで動作確認
- BILLING_PUBLIC_KEY設定後は必ずAAB再ビルド

---

## 困ったときは
スクショを貼ってClaudeに質問してください！
