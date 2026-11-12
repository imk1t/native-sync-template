# features/search

## Overview

Own the Search tab and repository/issue browsing. Separate Presentation, Domain, and Data layers without handling generated DTOs. Do not authenticate or write to GitHub.

## Navigation

| Screen (`specs/routes.yaml`) | Purpose |
| --- | --- |
| `repositorySearch` | Search tab with query, language, sort order, and results |
| `repositoryDetail` | Details for `owner`, `repo`; also opened through the same entry point from Saved |
| `repositoryIssues` | Issues for `owner`, `repo` |
| `issueDetail` | Issue details for `owner`, `repo`, `number` |

The Search tab's top-level screen (`repositorySearch`) shows a large left-aligned title like Saved and Settings; the screens it pushes (details, issues, issue details) show a small centered title next to the back button.

Selecting a search result calls `push(repositoryDetail)`; details opens `push(repositoryIssues)`; selecting an issue calls `push(issueDetail)`. Back navigation uses `pop()`. API IDs are 64-bit integers.

## Screens and state

- Public entry points use `<ScreenName>Screen` on both platforms and receive screen arguments, SearchDependencies, and Navigator where needed. ViewModel creation and retention, and screen rendering, remain within the feature. The app's AppRouter only maps Route to entry points.
- Retain screen state per NavigationEntry ID. Repeated openings of the same Route are independent; surviving entries preserve state through back navigation and tab changes.
- Lists have `items`, `hasLoaded`, `isLoading`, `error`, `nextPage`, and `incomplete`. Initially, nothing is loaded and the list is empty. Distinguish initial loading, refresh, and pagination; preserve loaded items on failure.
- Search only on submission; do not send whitespace-only queries. Language choices are All, Swift, Kotlin, TypeScript, and Rust; sort choices are relevance, stars, and update time. Append `is:public` and the selected language to the query.
- Trim leading/trailing query whitespace with `core/extensions`' trimmedWhitespace, preserving internal whitespace. Follow that module's defined whitespace set.
- Fetch 20 items per page, deduplicate by ID, and use API pagination to detect the end. Display the 1,000-result search limit and incomplete results.
- Details have loading, success, and error states. Support null descriptions, languages, authors, and bodies. Issue bodies are plain text.
- Allow Open / Closed issue selection. Exclude pull requests while preserving next-page controls even on PR-only pages.
- Load initially only once per screen scope. Explicit refresh bypasses the cache. Ignore stale results after condition changes or cancellation. Cancellation does not display an error; if nothing was loaded, retry when the screen reappears.

## User actions and outcomes

- GitHubRepository maintains a five-minute memory cache keyed by search conditions and page, with an injectable clock.
- Delegate bookmark toggles to `shared/bookmarks`' BookmarkRepository, sharing the instance with the bookmarks feature. Reflect successful writes in lists and details. Show retryable errors on failure.
- Display storage read failures without overwriting saved data with empty data.
- Apply the shared DisplaySettings description preference to lists. Pass display values and ImageLoader behavior to row UI, with a fallback for image failures.

## APIs and tokens

- APIs: `search/repos`, `repos/get`, `issues/list-for-repo`, and `issues/get`. Wrap `shared/github`'s GitHubAPI in a Repository.
- Depend on `core/navigation`, `core/network` (image loading), `core/storage`, `core/extensions`, `core/utils`, `shared/github`, `shared/bookmarks`, and `shared/designsystem`. Do not depend on other features.
- Use shared/designsystem rows, state displays, and badges.

## Platform differences

- The iOS module is `FeatureSearch`, with implementation in `Sources/Features/Search` and tests in `Tests/Features/Search`. The Android package is `com.example.features.search`.
- iOS uses @MainActor / Observation and per-screen @State; Android uses ViewModel / StateFlow and a ViewModelStore per Navigation 3 entry. Fetching is owned by iOS task and Android viewModelScope.
- Public entry points are Screen on both platforms. Internal rendering uses Swift View / Kotlin Content; API acronyms are GitHubAPI / GitHubApi. Keep Domain and Data responsibilities and type names aligned.
- The search field is a pill drawn as the first row of the list on both platforms (iOS does not use `searchable`: its always-visible drawer forces a small inline title, which conflicts with the large title of a top-level screen). It has a clear button and submits with the keyboard's search key.
- Use iOS List / Picker and Android LazyColumn / selection chips. Support accessibility labels and text scaling. Restoring screen stacks after process termination is out of scope.

## Scenarios

- `specs/scenarios/github-search.yaml`
- `specs/scenarios/github-detail.yaml`

## Localization

SearchFailure retains the raw retry timestamp. At display time, pass the current display locale and time zone to DateTimeUtils.formatTime. Error-to-Message mapping and translation resolution stay in the feature.

Resolve UI text through core/localization Message in the display language. ViewModels retain unresolved keys and arguments. Present network/storage failures according to their meaning; do not expose internal messages from unknown errors. Preserve state transitions, retries, and cancellation behavior. Do not translate API bodies/names or user notes.
