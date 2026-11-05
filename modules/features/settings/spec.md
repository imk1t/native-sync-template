# features/settings

## Overview

Toggle descriptions in lists and display app information.

## Navigation

`settings` is the Settings tab's root. Navigation goes through core/navigation's Navigator.

## Screens and state

Support initial loading, ready, saving, and error states. Description visibility defaults to true. Display read/write failures and allow retries.

The public entry point is SettingsScreen on both platforms, receiving only core/storage's DisplaySettings. ViewModel creation, retention per screen entry, and rendering remain within the feature. AppDependencies passes the same DisplaySettings used by GitHub screens, and AppRouter calls the entry point. Preserve state across tab changes.

## User actions and outcomes

A switch change saves through core/storage's DisplaySettings. Reflect changes on all screens only after a successful write, and restore them after relaunch. Disable interaction while saving; preserve the saved value on failure.

## APIs and tokens

No external APIs. Depend on `core/storage`, `core/navigation`, and `shared/designsystem`, with no dependencies on other features.

## Platform differences

The iOS module is `FeatureSettings`, with implementation in `Sources/Features/Settings` and tests in `Tests/Features/Settings`. The Android package is `com.example.features.settings`.

iOS uses Observation and SwiftUI Form / Toggle; Android uses StateFlow and Compose Switch. The public entry point is SettingsScreen, internal rendering is SettingsView / SettingsContent, and state is managed by SettingsViewModel. Retain the ViewModel with @State on iOS and the Navigation 3 entry's ViewModelStore on Android.

## Scenarios

- `specs/scenarios/settings.yaml`

## Localization

Resolve UI text through core/localization Message in the display language. ViewModels retain unresolved keys and arguments. Present network/storage failures according to their meaning; do not expose internal messages from unknown errors. Preserve state transitions, retries, and cancellation behavior. Do not translate API bodies/names or user notes.
