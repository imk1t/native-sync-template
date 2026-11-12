# shared/bookmarks

## Overview

Provide bookmark models and BookmarkRepository for multiple features. Wrap core/storage's KeyValueStore to centralize the storage key, JSON format, updates, and notifications. This module has no screens, ViewModels, or API requests.

## Navigation

None.

## Screens and state

Create BookmarkRepository once in the app and share the same instance with search and bookmarks. Retain loaded data and update public state only after successful writes. Callers do not handle the storage key or JSON format.

## User actions and outcomes

- Provide `load`, `contains`, `toggle`, `remove`, and `saveNote`.
- Use storage key `github.bookmarks.v1` and JSON version 1. Preserve the ID, repository display snapshot, note, and saved-time format so data saved before the module move remains readable.
- Allow at most 100 bookmarks. Notes may be empty and contain at most 1,000 Unicode code points.
- BookmarkNoteRules provides `maxCodePoints`, `characterCount(note)`, and `isValid(note)`. Count through `core/extensions`, sharing the same rules for persistence validation, feature character counts, and save eligibility. Do not normalize or truncate input.
- Serialize updates so concurrent actions do not lose additions or notes. Preserve public state after write failures.
- Treat read failures, malformed JSON, and unknown or missing versions as errors. Do not overwrite them with empty data.

## APIs and tokens

Depend on `core/storage` (KeyValueStore), and `core/extensions` (code point counting). `BookmarkedRepository` is this module's own repository snapshot (id, name, full name, owner, avatar URL, description, language, stars, forks, topics, HTML URL) and does not depend on GitHub API types; callers convert. No external APIs or tokens.

## Platform differences

The iOS module is `SharedBookmarks`, with implementation in `Sources/Shared/Bookmarks` and tests in `Tests/Shared/Bookmarks`. The Android package is `com.example.shared.bookmarks`.

iOS uses @MainActor / Observation and Task serialization; Android uses StateFlow and Mutex. JSON field names and saved-time units (Unix seconds) are identical.

## Scenarios

- `specs/scenarios/github-bookmarks.yaml`

## Error presentation

Error types retain kinds and associated data, with no resolved display text. The presentation layer maps them to core/localization messages.
