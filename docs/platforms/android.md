# Android 固有の設定と注意点

OS 固有の設定・環境・落とし穴をここに残す。機能ごとの意図した違いは各 `spec.md` の「OS 固有の差分」に書く。

## ビルド環境
- Android SDK(compileSdk 36)と JDK 17 が必要。各モジュールが `jvmToolchain(17)` を指定している。
- Gradle は `apps/android` の wrapper(`./gradlew`)で 8.14.3 に固定する。手元の `gradle` は使わない(理由: `docs/adr/0003`)。
- モジュールは `apps/android/settings.gradle.kts` に追加する(理由: `docs/adr/0001`)。

## 実行
```
cd apps/android && ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.app/.MainActivity
```

## 画面まわり
- edge-to-edge で描画されるため、コンテンツはシステムバーの下まで描かれる。`AppRoot` が `safeDrawing` の上端 inset をスタックに適用している。下端はボトムバー(`NavigationSuiteScaffold`)が扱う。
- ステータスバーのアイコン色は `enableEdgeToEdge` で指定しないと、明るい背景で薄く見える(未対応)。

## 落とし穴
- Gradle 9.x では、実在しないディレクトリ `:core`(`settings.gradle.kts` の中間プロジェクト)が設定エラーになる。8.14.3 を使う。
