---
name: add-third-party
description: Add a third-party library or SDK to iOS and Android through a core wrapper, updating the ADR, specs/third-party.yaml, and version management. Not needed for official infrastructure such as AndroidX, Kotlin, or kotlinx.
---

# Add a third-party library

## Principles

- Import third-party libraries only inside `core/*` wrapper modules, such as `core/analytics`. Features depend only on the wrapper's interface or protocol.
- Do not add a library when official platform APIs are sufficient.

## Steps

1. Before adding the library, write an ADR in `docs/adr/` covering its purpose, alternatives, license, maintenance status, and counterpart on the other platform.
2. Create the wrapper module using `add-module`.
3. Manage versions in these locations:
   - iOS: Use Swift Package Manager only, not CocoaPods. Declare dependencies with `.product(name:package:)` in `modules/Package.swift` and commit `Package.resolved` to use one version across all modules.
   - Android: Declare versions only in `apps/android/gradle/libs.versions.toml`. Use a BoM when the SDK provides one.
4. Add the role, module, library names for both platforms, license, and owner to `specs/third-party.yaml`.
5. Define names that must match across platforms, such as analytics events and configuration keys, in `specs/` and generate constants.
6. Do not commit environment-specific configuration files (API keys, `GoogleService-Info.plist`, `google-services.json`, etc.). Select dev/prod through build settings (iOS Configuration, Android productFlavor), and inject production values from CI secrets.
7. Pass `bun tools/check-deps.ts`.
