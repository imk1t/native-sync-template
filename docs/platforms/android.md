# Android configuration and notes

Record platform-specific configuration, environment requirements, and pitfalls here. Document intentional feature differences under “Platform differences” in each module's `spec.md`.

## Build environment

- Android SDK (compileSdk 36) and JDK 17 are required. Each module specifies `jvmToolchain(17)`.
- The app supports Android API 28 and later (`minSdk = 28`) and targets API 36.
- On macOS, set `JAVA_HOME` so Gradle and ktlint use JDK 17, including when managing Java with mise. An installed but unselected JDK can cause ktlint to fail to find a Java runtime.
- Set the SDK path through `ANDROID_HOME` or `sdk.dir` in `apps/android/local.properties`. With Homebrew's android-commandlinetools, an example SDK root is `/opt/homebrew/share/android-commandlinetools`. Choose the directory containing `platforms/android-36`.
- Use the wrapper (`./gradlew`) in `apps/android`, pinned to Gradle 8.14.3. Do not use a local `gradle` installation ([ADR 0003](../adr/0003-gradle-wrapper.md)).
- Register modules in `apps/android/settings.gradle.kts` ([ADR 0001](../adr/0001-monorepo-and-spec-sync.md)).

## Package layout

- Packages use `com.example.<hierarchy-under-modules/>`: `core/network` → `com.example.core.network`, `shared/bookmarks` → `com.example.shared.bookmarks`, and `features/search` → `com.example.features.search`.
- Match directories under `src/main/kotlin` and `src/test/kotlin` to the package. Each Android library's `namespace` matches its root package.
- `shared/designsystem` uses `com.example.shared.designsystem`; the app uses `com.example.app`. The `domain`, `data`, and `presentation` subdirectories organize implementation files while sharing the module's package.
- Generated code uses the owning module's package. API, navigation, and token code lives under each package's `generated/`; localization lives in `src/main/generated/kotlin/com/example/core/localization/`. Output paths and package declarations are managed by `tools/generate.ts` and `tools/lib/localization.ts`.

## Build and run

```sh
cd apps/android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.app/.MainActivity
```

## Tests

Run JVM tests from `apps/android` with `./gradlew test`. Localization resource tests require a connected emulator and run separately as described below.

## Layout

- Edge-to-edge rendering places content underneath system bars. `AppRoot` applies the top `safeDrawing` inset to the stack; the bottom bar (`NavigationSuiteScaffold`) handles the bottom inset.
- `enableEdgeToEdge()` in `MainActivity.onCreate` sets system-bar icon colors to match the background. With `targetSdk = 36`, the OS enforces edge-to-edge rendering but does not automatically switch icon colors.

## Pitfalls

- Gradle 9.x rejects the missing directory for `:core`, an intermediate project in `settings.gradle.kts`. Use 8.14.3.

## Localization

- Sources live in `specs/localization/messages-*.yaml`. `bun tools/generate.ts` creates CoreLocalization's strings.xml and app language settings.
- Compose resolves resources for the current Configuration with `Message.localized()`. ViewModels retain only Message and its arguments.
- Connect the app's `generateLocaleConfig` and `resourceConfigurations` to the generated language list. Initially, only Japanese and English are exposed in the OS app-language settings.
- CoreLocalization resource tests use AndroidX Test. Following repository conventions, sources live in `src/test/kotlin` and are connected to androidTest through Gradle. With an emulator connected, run `./gradlew :core:localization:connectedDebugAndroidTest`. Regular `./gradlew test` does not include these resource tests.

## Fonts
- The typefaces are `res/font` resources in `:core:font` (`inter_tight_regular.ttf`, and so on). Resource names must be lower-case letters, digits, and underscores, so they differ from the iOS file names; the files are byte-identical (`tools/tests/font-files.test.ts`).
- No registration is needed. `FontFamilies` builds the `FontFamily` values, and `FontFamilies.lookup(name)` maps a `DESIGN.md` family name to one (null when not bundled).
- Compose has no per-script font cascade in one `FontFamily`: a glyph missing from the bundled font (such as Japanese) falls back to the system font (Noto Sans CJK) automatically. A weight that is not bundled is replaced by the nearest bundled weight.
- Material 3 `Typography` has no default family, so `DesignTheme` copies each of its 15 styles with the family.
