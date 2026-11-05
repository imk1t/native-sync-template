# ADR 0005: Separate tab features, shared implementations, and specifications

[ADR 0007](0007-extensions-and-utils.md) updates the general-purpose core modules and expands the allowed dependencies of designsystem.

## Decision

- Split screens into `features/search` (search, repository details, and issues), `features/bookmarks` (saved repositories and notes), and `features/settings` (settings).
- Use `modules/shared/*` for UI, models, and logic used by multiple features. Put common UI and tokens in `shared/designsystem`, initially restricted to `core/utils`. Other shared modules may depend only on core, with no shared-to-shared dependencies. Features may use shared and core, but must not depend on each other.
- `shared/bookmarks`' BookmarkRepository wraps core/storage's KeyValueStore, owning the storage key, JSON format, serialized updates, and state notifications. It has no screens, ViewModels, or API requests.
- AppDependencies creates BookmarkRepository, DisplaySettings, and ImageLoader once and passes them to each feature's dependency container. Opening details from Saved goes through Route and AppRouter.
- Put the common RepositoryRow in `shared/designsystem`. It receives RepositoryRowData and image-loading behavior rather than API models.
- Rename root-level `shared/` to `specs/`, distinguishing specifications and generation inputs (OpenAPI, tokens, routes, scenarios, and third-party mappings) from shared implementations in `modules/shared/`.

## Rationale and consequences

Letting each feature write directly through storage keys scatters JSON formats, concurrent updates, and notifications. A shared wrapper keeps core/storage generic and search/bookmark state aligned.

Common UI also belongs under shared as implementation reused across features. Restricting designsystem to `core/utils` prevents coupling with networking, storage, and other shared modules.

Preserve the `github.bookmarks.v1` storage key and JSON version 1 format, routes, deep links, and screen behavior. Generation and checks reference specs and verify the shared layer's dependency direction on both platforms. Each module's behavior remains defined by `modules/**/spec.md`.
