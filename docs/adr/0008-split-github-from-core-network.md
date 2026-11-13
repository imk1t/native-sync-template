# ADR 0008: Split GitHub-specific code out of core/network

`core/network` held the generic HTTP transport together with GitHub's API, domain models (`RepositorySummary`, `GitHubIssue`, `Page`), error classification, rate-limit handling, and generated DTOs. Every new API would have collected in this one module, and `core` would have known a specific service.

## Decision

- `core/network` keeps only API-independent pieces: HTTPRequest / HTTPResponse, HTTPTransport and its URLSession / HttpURLConnection implementations, the `call` decoding helpers, APIResponse, APIError, `pathSegment`, and ImageLoader.
- A new `shared/github-rest` module (`SharedGitHubREST` / `com.example.shared.githubrest`) holds GitHubAPI, DefaultGitHubAPI, GitHubTransport, GitHubError, the domain models, and the generated DTOs and APIClient. `tools/generate.ts` writes there. The shared scenario is renamed `github-api`.
- Each additional API gets its own `shared/<service>` module on the same pattern.
- `shared/bookmarks` must not depend on `shared/github-rest` (shared-to-shared dependencies are forbidden). It owns `BookmarkedRepository`, a snapshot type whose JSON matches the previously saved format, and `features/search` converts `RepositorySummary` to it.

## Consequences

Saved bookmark data remains readable. ImageLoader stays in `core/network` because it is generic HTTPS image fetching; a separate image module can follow if a third-party image library is adopted (avoid the name `CoreImage` on iOS, which collides with the system framework).
