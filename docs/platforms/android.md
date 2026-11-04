# Android 固有の設定と注意点

OS 固有の設定・環境・落とし穴をここに残す。機能ごとの意図した違いは各 `spec.md` の「OS 固有の差分」に書く。

## ビルド環境
- Android SDK(compileSdk 36)と JDK 17 が必要。各モジュールが `jvmToolchain(17)` を指定している。
- macOS で JDK を mise 管理する場合も、Gradle と ktlint が JDK 17 を使うよう `JAVA_HOME` を設定する。インストール済みでも未選択の場合、ktlint が Java Runtime を見つけられず失敗する。
- Gradle は `apps/android` の wrapper(`./gradlew`)で 8.14.3 に固定する。手元の `gradle` は使わない(理由: `docs/adr/0003`)。
- モジュールは `apps/android/settings.gradle.kts` に追加する(理由: `docs/adr/0001`)。

## パッケージ構成
- パッケージは `com.example.<modules/ からの階層>` とする。`core/network` は `com.example.core.network`、`shared/bookmarks` は `com.example.shared.bookmarks`、`features/search` は `com.example.features.search`。
- `src/main/kotlin` と `src/test/kotlin` のディレクトリもパッケージに合わせる。Android ライブラリの `namespace` はそのモジュールのルートパッケージと一致させる。
- `shared/designsystem` は `com.example.shared.designsystem`、アプリは `com.example.app`。`domain`・`data`・`presentation` は実装を分類するサブディレクトリで、パッケージはモジュール内で共通とする。
- 生成コードは同じモジュールのパッケージを使う。API・画面遷移・トークンは各パッケージ下の `generated/`、ローカライズは `src/main/generated/kotlin/com/example/core/localization/` に置く。出力先と package 宣言は `tools/generate.ts` と `tools/lib/localization.ts` で管理する。

## 実行
```
cd apps/android && ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.app/.MainActivity
```

## 画面まわり
- edge-to-edge で描画されるため、コンテンツはシステムバーの下まで描かれる。`AppRoot` が `safeDrawing` の上端 inset をスタックに適用している。下端はボトムバー(`NavigationSuiteScaffold`)が扱う。
- システムバーのアイコン色は、`MainActivity.onCreate` の `enableEdgeToEdge()` で背景の明暗に合わせる。`targetSdk = 36` では OS が edge-to-edge を強制するが、アイコン色は自動では切り替わらない。

## 落とし穴
- Gradle 9.x では、実在しないディレクトリ `:core`(`settings.gradle.kts` の中間プロジェクト)が設定エラーになる。8.14.3 を使う。

## ローカライズ
- 原本は `specs/localization/messages-*.yaml`。`bun tools/generate.ts` で CoreLocalization の strings.xml とアプリの言語設定を生成する。
- Compose は `Message.localized()` で現在の Configuration のリソースを解決する。ViewModel は Message と引数だけを保持する。
- アプリの generateLocaleConfig と resourceConfigurations を生成した言語一覧に接続する。初期は日本語・英語のみを OS のアプリ言語設定に公開する。
- CoreLocalization のリソーステストは AndroidX Test を使う。規約に従いソースは `src/test/kotlin` に置き、Gradle で androidTest に接続する。エミュレーター接続後、`./gradlew :core:localization:connectedDebugAndroidTest` で実行する。通常の `./gradlew test` はこのモジュールのリソーステストを含まない。
