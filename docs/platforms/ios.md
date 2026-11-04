# iOS 固有の設定と注意点

OS 固有の設定・環境・落とし穴をここに残す。機能ごとの意図した違いは各 `spec.md` の「OS 固有の差分」に書く。

## ビルド環境
- Xcode と XcodeGen が必要。`apps/ios` で `xcodegen generate` を実行して `App.xcodeproj` を作る(コミットしない)。
- デプロイメントターゲットは iOS 18.0(`apps/ios/project.yml`)。
- Package は `modules/Package.swift` に集約する(理由: `docs/adr/0001`)。

## ディレクトリ構成
- 実装は `ios/Sources/<Group>/<Name>`、テストは `ios/Tests/<Group>/<Name>` に配置する。`core/network` は `Core/Network`、`features/search` は `Features/Search`、`shared/designsystem` は `Shared/DesignSystem`。
- SwiftPM の target 名は `CoreNetwork`・`FeatureSearch`・`DesignSystem` など一意な名前を使い、`path` でディレクトリを対応付ける。import とテストの target 名もこのモジュール名を使う。
- `Core/Network` と `Features/Network` のように末尾のディレクトリ名が同じでも、target 名と対象パスを分ければ共存できる。同じ Package 内で target 名を重複させない。
- 生成物とリソースは各実装の `Generated/` に置く。生成先の変更は `tools/generate.ts`・`tools/lib/localization.ts` に反映して再生成する。

## 実行
```
cd apps/ios && xcodegen generate
xcodebuild -project App.xcodeproj -scheme App -destination 'platform=iOS Simulator,name=<端末名>' CODE_SIGNING_ALLOWED=NO build
```
ビルドした `App.app` を `xcrun simctl install` / `launch`(バンドル ID は `com.example.app`)で起動できる。

## 画面まわり
- セーフエリアは SwiftUI が既定で扱うため、アプリ層で inset を意識する必要はない。

## ローカライズ
- 原本は `specs/localization/messages-*.yaml`。`bun tools/generate.ts` で CoreLocalization の String Catalog とアプリの InfoPlist.xcstrings、言語設定を生成する。
- SwiftPM の defaultLocalization と XcodeGen の developmentLanguage / knownRegions は生成設定を参照する。生成後に `xcodegen generate` を実行する。
- View は `@Environment(\.locale)` を渡して `Message.resolve(locale:)` を呼ぶ。Core 内で LocalizedStringResource と Bundle.module を指定する。翻訳済み文字列を ViewModel に保存しない。
- CoreLocalizationTests は Simulator で実リソースの日本語・英語・未対応言語・複数形を検証する。詳細は ADR 0006。
