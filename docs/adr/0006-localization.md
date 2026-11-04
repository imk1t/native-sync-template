# ADR 0006: 言語別 YAML を原本にし、Core にネイティブ翻訳リソースを集約する

- 状態: 採用・実装済み
- 対応言語: 日本語・英語。既定言語は日本語。
- 原本: `specs/localization/messages-ja.yaml` と `messages-en.yaml`

## 背景と決定

画面が増えても翻訳の配置先を迷わず、1 回の文言変更を両 OS に反映できる構成にする。共通文言と画面固有の文言はすべて `core/localization` に集約する。iOS は String Catalog、Android は文字列リソースを使用し、YAML の読み込みや翻訳処理をアプリの実行時に持ち込まない。

原本は言語ごとに一つの YAML にする。`Common`・`App`・feature ごとのファイルには分けず、`common_`・`search_`・`bookmarks_`・`settings_` などのキー接頭辞で整理する。生成先も iOS の `Localizable.xcstrings` 一つ、Android の言語ごとの `strings.xml` 一つとする。

`locales.yaml` と引数だけを管理する `messages.yaml` は設けない。対応言語は `messages-*.yaml` のファイル名から検出する。ファイルの存在はその言語を公開することを意味する。既定言語 `ja` は生成器の `DEFAULT_LOCALE` 一か所に定義し、SwiftPM・XcodeGen・Gradle 向けの設定へ反映する。

## 責務と依存

| 配置先 | 責務 |
| --- | --- |
| `specs/localization` | 文言 ID・翻訳・引数・複数形の原本 |
| `tools/lib/localization.ts` | 入力検証、ネイティブリソース・型付き参照・言語設定の生成 |
| `core/localization` | 生成したリソースと Message、表示時のネイティブ API 呼び出し |
| feature の Presentation | 表示する文言の選択、業務エラーから Message への変換 |
| designsystem | 呼び出し側が渡したラベルの表示 |
| apps | 言語設定の接続、タブ名の対応、OS がアプリ本体から読むリソース |

`features/* → core/localization` と `apps → core/localization` を追加する。CoreLocalization は他の内部モジュールに依存しない。画面、ViewModel、navigation、業務エラー型は置かない。

designsystem は従来の依存制約を維持する。LoadingIndicator・ErrorState・RepositoryRow は、読み込み・再試行・Star 数・保存済みのラベルを String で受け取る。呼び出し側が現在の表示言語で解決する。

## ファイル構成

```text
specs/localization/
  messages-ja.yaml
  messages-en.yaml
modules/core/localization/
  spec.md
  ios/Sources/CoreLocalization/
    LocalizationResources.swift
    Generated/
      Message.swift
      Resources/Localizable.xcstrings
  ios/Tests/CoreLocalizationTests/MessageTests.swift
  android/
    build.gradle.kts
    src/main/kotlin/com/example/corelocalization/Localized.kt
    src/main/generated/
      kotlin/com/example/corelocalization/Message.kt
      res/values/strings.xml
      res/values-en/strings.xml
    src/test/kotlin/com/example/corelocalization/MessageTest.kt
modules/Generated/Localization/config.json
apps/ios/
  Generated/Localization/config.yml
  App/Generated/Localization/InfoPlist.xcstrings
apps/android/app/src/main/generated/localization/
  locales.txt
  res/resources.properties
```

原本は ID 順に並べる。生成物はコミットし、手編集しない。Xcode の翻訳編集から YAML への逆輸入は行わない。ファイル分割は編集競合や翻訳担当の分離など具体的な問題が発生した段階で検討し、画面追加だけを理由に増やさない。

## YAML の契約

通常の文言はスカラー文字列とする。引数・複数形・コメント・アプリメタデータが必要な項目だけ構造化する。

```yaml
# messages-ja.yaml
common_retry: 再試行
error_rate_limited:
  arguments:
    - name: time
      type: string
  text: 取得回数の上限です。{time}以降に再試行してください。
repository_stars:
  arguments:
    - name: count
      type: integer
  plural: count
  forms:
    other: "Star {count}"
```

```yaml
# messages-en.yaml
common_retry: Retry
error_rate_limited:
  text: Rate limit reached. Try again after {time}.
repository_stars:
  forms:
    one: "{count} star"
    other: "{count} stars"
```

日本語だけが引数と複数形の契約を宣言し、他言語は本文を持つ。引数は `string` / `integer`、複数形は整数引数一つを対象とする。全形式で全引数を参照する。語順変更と引数の繰り返しを許容する。波括弧は名前付き引数専用とする。

日本語側では任意の `comment` と、アプリ名の `iosInfoPlist: CFBundleDisplayName` を指定できる。現時点の OS メタデータはこのキーだけを受理し、権限説明などの拡張は契約を追加してから行う。アプリ名は Core の文字列にも生成し、AndroidManifest から `@string/app_name` で参照する。

言語タグは BCP 47 の `language[-Script][-REGION]` を受理する（例: `en`、`pt-BR`、`zh-Hant`）。Android の複合タグは `values-b+...` に変換する。翻訳漏れをフォールバックで隠さず、全言語のキー・引数一致を生成時に要求する。

## ネイティブの参照

両 OS に同じキー・引数を持つ `Message` を生成する。生成器がすべての参照を組み立てるため、呼び出し側に文字列キーや動的リソース ID 探索を持ち込まない。

```swift
@Environment(\.locale) private var locale
Text(Message.repositoryStars(count: repository.stars).resolve(locale: locale))
```

iOS は SwiftPM の `.process("Generated/Resources")` と `Bundle.module` を使う。`LocalizedStringResource` に bundle と locale を明示し、Foundation で文字列に変換する。`String(localized:bundle:locale:)` だけでは表示言語の明示切替が反映されなかったため、リソースを経由する API に統一する。未対応言語の場合は bundle の developmentLocalization に戻す。

複数形には Catalog の substitutions を使い、対象引数の位置を指定する。翻訳中の位置指定と Swift の補間順序を対応させ、文字列引数の後に数量が来る場合や語順変更も扱う。

```kotlin
Text(Message.RepositoryStars(repository.stars).localized())
```

Android の `localized()` は Compose の Configuration と Context を参照し、`getString` / `getQuantityString` で解決する。リソースの選択と複数形の判定は OS が行う。生成ディレクトリを Gradle の sourceSets に登録する。

## 言語選択とアプリ設定

初期実装は OS 設定に従い、アプリ内の言語選択画面・独自の設定保存は設けない。iOS は OS のアプリ別言語と優先言語、Android 13 以降は OS のアプリ別言語、それ以前は端末言語を使う。対応言語がない場合の最終フォールバックは日本語とする。

SwiftPM の defaultLocalization と XcodeGen の developmentLanguage / knownRegions を生成設定に接続する。OS がアプリ本体から読む InfoPlist.xcstrings はアプリ側へ生成する。

Android は `generateLocaleConfig` を使用し、生成した `resources.properties` の既定言語と、`locales.txt` の対応言語にリソース設定を限定する。依存ライブラリの翻訳言語をアプリの対応言語として公開しない。手書きの locale_config.xml は併用しない。

## 既存機能への適用

search・bookmarks・settings のタイトル、説明、操作、エラーと読み上げラベルを置き換える。ユーザーのメモ、GitHub の名前・本文、検索条件の送信値、ID は翻訳しない。既知の Issue 状態だけラベルに変換し、未知の値はそのまま表示する。

`specs/routes.yaml` の title と `AppTab.title` を除去する。アプリ層で AppTab を網羅的に分岐し、`searchTitle`・`commonSaved`・`settingsTitle` に対応付ける。core/navigation への翻訳依存は加えない。

GitHubError と BookmarkError は種類と関連データを保持し、表示文言を持たない。feature で Message に変換し、未知の内部エラーメッセージはユーザーに表示しない。ViewModel は翻訳済み String を保持しない。レート制限の再試行時刻は SearchFailure に生の時刻として保持し、表示時に地域・タイムゾーンに従って整形する。

既存の API 呼び出し、取得済み一覧や入力の保持、再試行、キャンセル処理は維持する。

## 検査と運用

`bun tools/generate.ts` は全入力を検証してから出力する。YAML の重複キーは開発用の `yaml` パーサーで拒否する。この依存は生成ツールだけに使い、アプリには含まれない。キー欠落・余分なキー・空の翻訳・未対応フィールド・不正な言語タグ・引数不一致・複数形不足・識別子の衝突をエラーにする。

XML と JSON の特殊文字、改行、パーセント記号をエスケープする。生成器が所有するディレクトリだけを清掃し、削除言語の生成物を残さない。`check-generated` は固定出力とローカライズの生成先を検査する。未生成、手編集、削除済み言語の残存を tools のテストで確認する。

共通シナリオ `localization.yaml` を両 OS で検証する。iOS は Simulator、Android は AndroidX Test とエミュレーターで、実際のリソースの日本語・英語・未対応言語・複数形・引数を確認する。Android のソースは規約の `src/test` に置き、当該モジュールのみ androidTest の sourceSet に接続する。通常の JVM 単体テストからは除外する。

変更時は再生成、`mise run fmt`、`mise run check`、`mise run lint` を行い、必要なネイティブテストとビルドを実施する。両 OS の変更を一つの PR にまとめる。

## 参考

- [Apple: パッケージリソースのローカライズ](https://developer.apple.com/documentation/xcode/localizing-package-resources)
- [Apple: LocalizedStringResource.locale](https://developer.apple.com/documentation/foundation/localizedstringresource/locale)
- [Android: アプリ別言語設定](https://developer.android.com/guide/topics/resources/app-languages)
- [Android: AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test)
