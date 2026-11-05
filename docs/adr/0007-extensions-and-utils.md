# ADR 0007: Separate general extensions and dedicated utility types in Core

- Status: Accepted

## Decision

- `core/extensions` provides general extensions of standard types. Its initial StringExtensions implementation centralizes blank checks, whitespace trimming, and Unicode code point counting.
- `core/utils` contains general operations exposed through dedicated types. The initial DateTimeUtils implementation formats times; callers supply locale and time zone at display time.
- `core/extensions` has no internal dependencies. `core/utils` may depend only on `core/extensions`. Neither may use third-party libraries; the standard library, Foundation, JDK, and test dependencies are allowed.
- `shared/designsystem` may reference only these two modules. Add dependencies only when used; this change adds none.
- Keep the business rule for note length in `shared/bookmarks`' BookmarkNoteRules, shared by display and persistence.

## Extension conventions

Use properties for queries and predicates, and functions for transformations. Prefer standard APIs when they satisfy the contract. Add extensions only to align platform contracts or remove duplication. Do not expose the same operation through both a utility and an extension.

Keep Message.localized in core/localization, Route URL conversion in core/navigation, and screen-specific conversions in their feature. Extension syntax alone is not a reason to move code into CoreExtensions.

## Rationale and consequences

Shared contains UI, business models, and logic used by multiple features, with no shared-to-shared dependencies. Placing general string operations needed during persistence in Core preserves this direction. A separate CoreExtensions makes ownership of general extensions explicit.

Blank checks and trimming use the same Unicode whitespace set defined by the core/extensions spec, independent of platform defaults. Note limits and storage formats stay unchanged. Existing Message APIs handle numeric text and plurals; no NumberUtils is added. Improving issue timestamp parsing and display is outside this decision's scope.

Reflect the dependency constraints in AGENTS.md and tools/check-deps.ts, and align build definitions and checks on both platforms.
