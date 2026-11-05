# ADR 0006: Generate native localization resources in Core from per-language YAML

- Status: Accepted and implemented
- Supported languages: Japanese and English; Japanese is the default.
- Sources: `specs/localization/messages-ja.yaml` and `messages-en.yaml`

## Context and decision

Keep translation ownership clear as screens are added, and apply each wording change to both platforms at once. Centralize all common and feature-specific UI text in `core/localization`. Use String Catalogs on iOS and string resources on Android, without loading YAML or introducing custom translation processing at runtime.

Use one YAML file per language. Organize keys with prefixes such as `common_`, `search_`, `bookmarks_`, and `settings_`, rather than splitting files by Common, App, or feature. Generate one iOS `Localizable.xcstrings` and one Android `strings.xml` per language.

Do not add `locales.yaml` or a separate `messages.yaml` for arguments. Detect supported languages from `messages-*.yaml` filenames: adding a file exposes that language. Define the default language `ja` once in the generator's `DEFAULT_LOCALE` and propagate it to SwiftPM, XcodeGen, and Gradle settings.

## Responsibilities and dependencies

| Location | Responsibility |
| --- | --- |
| `specs/localization` | Source message IDs, translations, arguments, and plurals |
| `tools/lib/localization.ts` | Validate inputs and generate native resources, typed references, and language settings |
| `core/localization` | Generated resources and Message types; native API calls at display time |
| Feature Presentation | Select messages and map business errors to Message |
| designsystem | Display labels supplied by callers |
| apps | Connect language settings, map tab labels, and provide resources read by the OS from the app bundle |

Add `features/* → core/localization` and `apps → core/localization`. CoreLocalization has no internal module dependencies and contains no screens, ViewModels, navigation, or business error types.

Keep designsystem's existing dependency constraints. LoadingIndicator, ErrorState, and RepositoryRow receive loading, retry, star-count, and saved labels as strings resolved by callers in the current display language.

## File layout

```text
specs/localization/
  messages-ja.yaml
  messages-en.yaml
modules/core/localization/
  spec.md
  ios/Sources/Core/Localization/
    LocalizationResources.swift
    Generated/
      Message.swift
      Resources/Localizable.xcstrings
  ios/Tests/Core/Localization/MessageTests.swift
  android/
    build.gradle.kts
    src/main/kotlin/com/example/core/localization/Localized.kt
    src/main/generated/
      kotlin/com/example/core/localization/Message.kt
      res/values/strings.xml
      res/values-en/strings.xml
    src/test/kotlin/com/example/core/localization/MessageTest.kt
modules/Generated/Localization/config.json
apps/ios/
  Generated/Localization/config.yml
  App/Generated/Localization/InfoPlist.xcstrings
apps/android/app/src/main/generated/localization/
  locales.txt
  res/resources.properties
```

Sort source entries by ID. Commit generated files and do not edit them by hand. Do not import Xcode translation edits back into YAML. Consider splitting files only when concrete problems arise, such as editing conflicts or separate translation ownership, rather than whenever a screen is added.

## YAML contract

Use scalar strings for ordinary messages. Use structured entries only when arguments, plurals, comments, or app metadata are needed. The Japanese example below intentionally retains Japanese message text.

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

Only Japanese declares the argument and plural contract; other languages provide text. Arguments are `string` or `integer`, and plurals target one integer argument. Every form must reference every argument. Reordering and repeating arguments is allowed. Braces are reserved for named arguments.

Japanese entries may include an optional `comment` and `iosInfoPlist: CFBundleDisplayName` for the app name. This is currently the only accepted OS metadata key; extend the contract before adding permission descriptions or other metadata. The app name is also generated as a Core string and referenced from AndroidManifest as `@string/app_name`.

Accept BCP 47 tags in `language[-Script][-REGION]` form, such as `en`, `pt-BR`, and `zh-Hant`. Convert compound Android tags to `values-b+...`. Require matching keys and arguments across all languages during generation, rather than hiding missing translations through fallback.

## Native references

Generate Message types with matching keys and arguments on both platforms. The generator constructs all references, so callers do not use string keys or dynamic resource-ID lookups.

```swift
@Environment(\.locale) private var locale
Text(Message.repositoryStars(count: repository.stars).resolve(locale: locale))
```

iOS uses SwiftPM's `.process("Generated/Resources")` and `Bundle.module`. Specify the bundle and locale in `LocalizedStringResource`, then resolve it through Foundation. Explicit display-language changes were not reflected by `String(localized:bundle:locale:)` alone, so use the resource-based API consistently. Unsupported languages fall back to the bundle's developmentLocalization.

Use Catalog substitutions for plurals, specifying the target argument's position. Match positional references in translations to Swift interpolation order, including quantities following string arguments and reordered text.

```kotlin
Text(Message.RepositoryStars(repository.stars).localized())
```

Android's `localized()` reads Compose's Configuration and Context and resolves through `getString` / `getQuantityString`. The OS selects resources and plural categories. Register generated directories in Gradle sourceSets.

## Language selection and app configuration

The initial implementation follows OS settings, with no in-app language picker or custom persistence. iOS uses per-app and preferred language settings; Android 13 and later uses per-app language settings, while earlier versions use the device language. Japanese is the final fallback when no supported language matches.

Connect SwiftPM's defaultLocalization and XcodeGen's developmentLanguage / knownRegions to generated settings. Generate InfoPlist.xcstrings in the app because the OS reads it from the app bundle.

Android uses `generateLocaleConfig`. Restrict resource settings to the default language in generated `resources.properties` and supported languages in `locales.txt`. Do not expose translations from dependencies as app-supported languages or combine this with a handwritten locale_config.xml.

## Applying localization to existing features

Replace titles, descriptions, actions, errors, and accessibility labels in search, bookmarks, and settings. Do not translate user notes, GitHub names or bodies, submitted search values, or IDs. Map only known issue states to labels; display unknown values unchanged.

Remove title from `specs/routes.yaml` and `AppTab.title`. Exhaustively map AppTab to `searchTitle`, `commonSaved`, and `settingsTitle` in the app layer. Do not add a localization dependency to core/navigation.

GitHubError and BookmarkError contain error kinds and associated data, with no display text. Features map them to Message, keeping unknown internal error messages out of the UI. ViewModels do not retain resolved strings. SearchFailure retains the raw rate-limit retry timestamp, formatted with the display locale and time zone when shown.

Preserve existing API calls, loaded lists, input retention, retries, and cancellation behavior.

## Validation and maintenance

`bun tools/generate.ts` validates all inputs before writing outputs. The development-only `yaml` parser rejects duplicate keys and is not included in the app. Reject missing or extra keys, empty translations, unsupported fields, invalid language tags, mismatched arguments, missing plural forms, and identifier collisions.

Escape XML/JSON special characters, newlines, and percent signs. Clean only generator-owned directories and remove outputs for deleted languages. `check-generated` checks fixed outputs and localization destinations. Tooling tests cover missing outputs, manual edits, and stale resources for removed languages.

Verify the shared `localization.yaml` scenario on both platforms using real resources: Japanese, English, unsupported languages, plurals, and arguments. iOS uses a simulator; Android uses AndroidX Test on an emulator. Android sources remain in the conventional `src/test` directory, connected to androidTest only for this module and excluded from regular JVM unit tests.

After changes, regenerate and run `mise run fmt`, `mise run check`, and `mise run lint`, plus relevant native tests and builds. Include both platforms in one PR.

## References

- [Apple: Localizing package resources](https://developer.apple.com/documentation/xcode/localizing-package-resources)
- [Apple: LocalizedStringResource.locale](https://developer.apple.com/documentation/foundation/localizedstringresource/locale)
- [Android: Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages)
- [Android: AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test)
