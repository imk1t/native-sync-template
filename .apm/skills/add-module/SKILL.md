---
name: add-module
description: Create a new module under modules/ (features/<name>, core/<name>, or shared/<name>) for both iOS and Android, including Package.swift and settings.gradle.kts registration. Use sync-feature for changes to existing modules.
---

# Add a module

## Location and naming

- Features go in `modules/features/<name>/`, infrastructure in `modules/core/<name>/`, and UI, models, or logic shared across features in `modules/shared/<name>/`.
- Common UI and tokens belong in `shared/designsystem`, which may depend only on `core/utils`, `core/extensions`, and `core/font`. Other shared modules may depend only on core. Shared modules must not depend on each other.
- General extensions of standard types belong in `core/extensions` (no internal dependencies); general operations exposed through dedicated types belong in `core/utils` (only `core/extensions` is allowed as an internal dependency). Neither may use third-party libraries. Keep feature-specific extensions in their feature.
- Each module contains `spec.md`, `ios/`, and `android/`. Use corresponding names on both platforms (`FeatureLogin` on iOS ⇔ `:features:login` on Android).

## Steps

1. Choose the location and dependencies according to AGENTS.md.
2. Write `spec.md` using the headings in `modules/features/search/spec.md`: Overview, Navigation, Screens and state, User actions and outcomes, APIs and tokens, Platform differences, and Scenarios. Features with screens depend on `core/navigation`; register their screens in `specs/routes.yaml`.
3. iOS: Add a target to `modules/Package.swift`, with **one target per line**, and add a `.library` product. Set `path:` to `<module>/ios/Sources/<Group>/<Name>` (for example, `features/login/ios/Sources/Features/Login`). Put tests in `<module>/ios/Tests/<Group>/<Name>` and add `.testTarget(name: "<Target>Tests", dependencies: ["<Target>"], path: ...)`. Use a unique target name such as `FeatureLogin`. Do not create individual `Package.swift` files: identical `ios/` directory names collide in SwiftPM (docs/adr/0001).
4. Android: Create `modules/<module>/android/build.gradle.kts` (implementation in `src/main/kotlin/`, tests in `src/test/kotlin/`) and add `module(":<name>", "<module-path>")` to `apps/android/settings.gradle.kts`. Set the JVM toolchain to 17 with `kotlin { jvmToolchain(17) }`.
5. Declare module dependencies in `Package.swift`'s `dependencies` on iOS and with `project(":...")` on Android. **Keep dependencies identical across platforms.**
6. If the module is used by screens, add assembly code only to `apps/ios` and `apps/android`.
7. Run `bun tools/check-spec-sync.ts --update both <module-path>`, then pass `bun tools/check-deps.ts` and `bun tools/check-spec-sync.ts`.
8. Implement behavior using the `sync-feature` workflow.
