# features/bookmarks

## Overview

Own the Saved tab and note-editing sheet. Delegate persistence to `shared/bookmarks`' BookmarkRepository without handling storage keys or JSON formats.

## Navigation

| Screen (`specs/routes.yaml`) | Purpose |
| --- | --- |
| `bookmarks` | Saved tab |
| `bookmarkNote` | Sheet for editing the note for `repositoryId` |

Selecting a row calls `push(repositoryDetail)`. AppRouter maps it to the search feature's entry point, with no feature-to-feature dependency. Notes open with `present(bookmarkNote, sheet)`; successful saves and cancellation call `dismiss()`. Route IDs are strings; stored IDs are 64-bit integers.

## Screens and state

- Public entry points are BookmarksScreen and BookmarkNoteScreen. They receive BookmarksDependencies, Navigator, and repositoryId for note editing. ViewModel creation, retention, and rendering remain within the feature.
- Retain screen state per NavigationEntry ID, preserving surviving entries through tab changes and back navigation.
- Display loading, error, and empty states. Rows use repository snapshots captured when saved, without API requests for the list.
- Share the same app-created BookmarkRepository, DisplaySettings, and ImageLoader with search. Reflect bookmark additions/removals, note edits, and description visibility changes.
- Pass display values and image-loading behavior to row UI, using a fallback when image loading fails.

## User actions and outcomes

- Show confirmation before deletion. Update the list after success; on failure, preserve saved data and show an error.
- Notes may be empty and contain at most 1,000 Unicode code points. Prevent duplicate submissions while saving. On failure, keep the input and leave the sheet open. Cancellation closes without saving the draft.
- Use `shared/bookmarks`' BookmarkNoteRules for character counts and length validation. The ViewModel decides whether saving is allowed based on screen state, including loading and saving.
- Show errors for storage read failures and unknown storage versions. Do not overwrite them with empty data.

## APIs and tokens

No external API calls. Depend on `shared/bookmarks`, `core/navigation`, `core/network` (image loading), `core/storage` (display settings), and `shared/designsystem`. Do not depend on other features.

## Platform differences

- The iOS module is `FeatureBookmarks`, with implementation in `Sources/Features/Bookmarks` and tests in `Tests/Features/Bookmarks`. The Android package is `com.example.features.bookmarks`.
- iOS uses @MainActor / Observation and @State; Android uses ViewModel / StateFlow and a ViewModelStore per entry. Public entry points are Screen; internal rendering uses Swift View / Kotlin Content.
- Use iOS List / sheets and Android LazyColumn / bottom sheets. Support accessibility labels and text scaling. Restoring screen stacks after process termination is out of scope.

## Scenarios

- `specs/scenarios/bookmark-note.yaml`

## Localization

Resolve UI text through core/localization Message in the display language. ViewModels retain unresolved keys and arguments. Present network/storage failures according to their meaning; do not expose internal messages from unknown errors. Preserve state transitions, retries, and cancellation behavior. Do not translate API bodies/names or user notes.
