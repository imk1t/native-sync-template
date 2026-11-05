# iOS configuration and notes

Record platform-specific configuration, environment requirements, and pitfalls here. Document intentional feature differences under “Platform differences” in each module's `spec.md`.

## Build environment

- Xcode and XcodeGen are required. Run `xcodegen generate` in `apps/ios` to create `App.xcodeproj`; do not commit it.
- The deployment target is iOS 18.0 (`apps/ios/project.yml`).
- All modules are registered in `modules/Package.swift` ([ADR 0001](../adr/0001-monorepo-and-spec-sync.md)).

## Directory layout

- Implementations live in `ios/Sources/<Group>/<Name>` and tests in `ios/Tests/<Group>/<Name>`. Examples: `Core/Network`, `Features/Search`, and `Shared/DesignSystem`.
- Use unique SwiftPM target names such as `CoreNetwork`, `FeatureSearch`, and `DesignSystem`, with explicit `path` entries. Imports and test targets use these module names.
- Paths ending in the same directory name, such as `Core/Network` and `Features/Network`, can coexist with distinct target names and paths. Target names must be unique within the package.
- Generated code and resources live under each implementation's `Generated/`. Change output paths in `tools/generate.ts` and `tools/lib/localization.ts`, then regenerate.

## Build and run

```sh
cd apps/ios
xcodegen generate
xcodebuild -project App.xcodeproj -scheme App -destination 'platform=iOS Simulator,name=<device-name>' CODE_SIGNING_ALLOWED=NO build
```

You can also open the generated project in Xcode and run the `App` scheme. To install and launch a built `App.app`, use `xcrun simctl install` and `xcrun simctl launch` with bundle ID `com.example.app`.

## Tests

Run module tests on an iOS simulator from the repository root:

```sh
cd modules
xcodebuild test -scheme Modules-Package -destination 'platform=iOS Simulator,name=iPhone 16'
```

Use `xcrun simctl list devices available` to find an installed simulator and replace `name=` as needed. Do not use `swift test`: it builds for the macOS host rather than the iOS simulator required by this package.

## Layout

- SwiftUI handles safe areas by default, so the app layer does not need to apply insets explicitly.

## Localization

- Sources live in `specs/localization/messages-*.yaml`. `bun tools/generate.ts` creates CoreLocalization's String Catalog, the app's InfoPlist.xcstrings, and language configuration.
- SwiftPM's `defaultLocalization` and XcodeGen's `developmentLanguage` / `knownRegions` read generated settings. Run `xcodegen generate` after regeneration.
- Views pass `@Environment(\.locale)` to `Message.resolve(locale:)`. Core specifies `LocalizedStringResource` and `Bundle.module`. Do not store resolved strings in ViewModels.
- CoreLocalizationTests verifies Japanese, English, unsupported-language fallback, and plurals using real resources on a simulator. See [ADR 0006](../adr/0006-localization.md).
