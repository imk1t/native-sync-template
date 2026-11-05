# core/localization

## Overview

Centralize common and feature-specific UI text. Translation sources are `specs/localization/messages-<language>.yaml`. Japanese is the base language, with English also provided. Detect supported languages from filenames; do not maintain a separate list.

## Public API

Generated `Message` types retain keys and typed arguments, resolved through native resources at display time. Arguments are string/integer; plurals target one integer argument. Only Japanese declares the argument and plural contract. Before generation, verify matching keys and arguments across languages, required `other` forms, and unsupported syntax.

## Language and display

Use the app language selected by the OS. Unsupported languages fall back to Japanese. Do not provide in-app language switching. Read SwiftUI Locale and Android resource Configuration at display time. Do not translate user input, GitHub bodies or names, or submitted search values. Numbers and plurals follow OS rules.

## Resource management

Generate one Localizable.xcstrings on iOS and a strings.xml per language on Android. Commit generated files; do not edit them by hand. Generate app-name metadata and supported-language settings from the same sources. To add a language, add a file containing all keys and regenerate. Remove old generated resources when deleting a language.

## Dependencies

No other module dependencies. Features and the app layer use this module. Pass resolved labels to designsystem.

## Platform differences

The iOS module is `CoreLocalization`, with implementation in `Sources/Core/Localization` and tests in `Tests/Core/Localization`. The Android package is `com.example.core.localization`.

iOS uses the SwiftPM resource bundle and Foundation; Android uses Context's getString / getQuantityString. Android 13 and later supports OS per-app language settings; earlier versions follow the system language.

## Verification

Tooling tests verify generator contracts, escaping, argument order, plurals, and language addition/removal. Both platforms test Japanese, English, fallback, plurals, and arguments. Android resolves real resources through AndroidX Test instrumentation. Sources remain in the repository-standard `src/test` directory and are connected to androidTest by this module's build definition.

## Scenarios

- `specs/scenarios/localization.yaml`
