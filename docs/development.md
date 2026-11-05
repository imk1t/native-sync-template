# Development guide

Start with the [README](../README.md) for setup and everyday commands. Platform requirements and native build instructions are in the [iOS](platforms/ios.md) and [Android](platforms/android.md) guides.

## Architecture

Both implementations use MVVM with unidirectional data flow and Presentation, Domain, and Data layers. SwiftUI uses Observation and Swift Concurrency; Compose uses ViewModel, StateFlow, and Kotlin Coroutines. Dependencies are injected through initializers or constructors.

| Layer | Responsibility | Examples |
| --- | --- | --- |
| Presentation | Screens and screen state | `RepositorySearchView` / `RepositorySearchScreen`, `RepositorySearchViewModel` |
| Domain | Business rules, models, and repository contracts | `Bookmark`, `GitHubRepository` |
| Data | Repository implementations and API/storage mapping | `GitHubRepositoryImpl` |

Domain and Data types have matching responsibilities and names across platforms. Swift uses `API` / `HTTP` and Kotlin uses `Api` / `Http`. Remote data goes through the domain's `GitHubRepository`; local state is observed through `shared/bookmarks` and `core/storage`.

The app layer owns startup, the `AppRoot` navigation container, shared dependencies in `AppDependencies`, and route mappings in `AppRouter`. Each feature's public `*Screen` entry point creates and retains its own ViewModel. Screen state belongs to a `NavigationEntry` ID and survives tab changes and back navigation while that entry remains.

Use `core/navigation` for all navigation. Routes and tabs are generated from `specs/routes.yaml`; tab labels are resolved by the app through `core/localization`. See [ADR 0002](adr/0002-navigation.md) and the [navigation specification](../modules/core/navigation/spec.md) for operations, deep links, and entry lifetime.

Dependency rules are defined in [AGENTS.md](../AGENTS.md#rules) and enforced by `tools/check-deps.ts`, including parity between the iOS and Android dependency graphs. Shared code within each platform belongs in `modules/shared`; shared specifications belong in `specs`. See [ADR 0005](adr/0005-feature-boundaries.md) and [ADR 0007](adr/0007-extensions-and-utils.md).

## Generation and specification checks

Run `mise run generate` (or `bun tools/generate.ts`) after changing generation inputs. Commit the generated outputs with their sources; do not edit them directly.

| Input | Output |
| --- | --- |
| `specs/openapi/openapi.yaml` | Internal DTOs and API clients in `core/network` |
| `specs/tokens/tokens.json` | Color and spacing tokens in `shared/designsystem` |
| `specs/routes.yaml` | `Route`, `AppTab`, and deep-link parsing in `core/navigation` |
| `specs/localization/messages-*.yaml` | Native resources, typed messages, and app language settings |

Generation runs offline without rewriting the API contract. `DefaultGitHubAPI` / `DefaultGitHubApi` maps generated DTOs to public domain models; `HTTPTransport` / `HttpTransport` can be replaced in tests. The supported OpenAPI subset is documented in `tools/lib/openapi.ts`; unsupported syntax fails generation. See [ADR 0004](adr/0004-github-openapi.md) for contract updates and [ADR 0006](adr/0006-localization.md) for localization.

Each step in `specs/scenarios/*.yaml` has an `id` and must be referenced in both platforms' tests with `// @scenario <scenario-name>/<id>`. The scenario checker verifies those references, the module spec's scenario references, and API fixtures against OpenAPI. Fixtures can describe navigation, local storage values and failures, response headers, and error bodies.

Each module's `ios/.spec-sync` and `android/.spec-sync` records the hash of the specification implemented by that platform. After aligning an implementation, update its marker:

```sh
bun tools/check-spec-sync.ts --update both modules/features/search
```

Use `ios` or `android` instead of `both` when only that implementation is aligned. The check fails if either marker is stale.

## Verification

`mise run check` runs the same repository checks as CI: tooling formatting, TypeScript type checking, tooling tests, spec alignment, dependency rules, generated-file freshness, and shared scenarios. Individual commands are useful when investigating a failure:

| Check | Command |
| --- | --- |
| Spec alignment | `bun tools/check-spec-sync.ts` |
| Module dependencies | `bun tools/check-deps.ts` |
| Generated files | `bun tools/check-generated.ts` |
| Shared scenarios | `bun tools/check-scenarios.ts` |
| Tooling tests | `bun test tools/tests` |
| Tooling types | `bun x tsc --noEmit` |
| Swift/Kotlin lint | `mise run lint` |

Native test commands and simulator/emulator requirements are in the platform guides. Tests use stubs and fixtures rather than the live GitHub API. Android localization resource tests run on an emulator separately from JVM tests.

The Bun tooling uses TypeScript, Bun type definitions, and the development-only `yaml` parser. Package versions are pinned. `.npmrc` and `bunfig.toml` restrict installation to the official registry, disable lifecycle scripts, and require a seven-day release age. CI also runs `bun audit`. If TypeScript reports `TS2688: Cannot find type definition file for 'bun'`, restore dependencies with `bun install --frozen-lockfile`.

## CI

| Workflow/job | Trigger | Checks |
| --- | --- | --- |
| CI / `repo-checks` | PRs and pushes to `main` | Dependency audit and `mise run check` on Linux |
| CI / `lint` | PRs and pushes to `main` | SwiftLint and ktlint; generated code is excluded |
| CI / `actions-lock` | PRs and pushes to `main` | `gh actions-lock --verify` |
| Native build | Manual, through Actions → Run workflow | iOS simulator tests and app build; Android JVM tests, debug app build, and localization test APK build |

Native builds are manual to limit runtime and cost. The native workflow builds the Android localization test APK but does not run its instrumentation tests on an emulator.

Workflow actions are pinned through `.github/workflows/actions.lock`. After changing a `uses:` entry, run `gh actions-lock`, review the workflow and lockfile changes, and commit both. Do not edit the lockfile by hand. Use exact action versions such as `@v4.4.0`.

Install the same extension version used by CI with `gh extension install github/gh-actions-lock --pin v0.4.0`. The lockfile records action commits, including nested actions; it does not cover Docker images or script downloads. The repository's Require lockfile policy can require a lockfile for execution.

## Agent skills

Project skill sources live in `.apm/skills`. `mise run setup` installs dependencies and runs `apm install`, deploying project and external skills to `.claude/skills` and `.agents/skills`. It also generates MCP configuration in `.mcp.json` and `.codex/config.toml`. These generated files are ignored by Git.

Run `apm install` again after changing a skill source. Commit `apm.lock.yaml` when dependency resolution changes. The APM version is pinned in `mise.toml`; do not run `apm compile`, because `AGENTS.md` is maintained by hand.

## Sample app

The Search, Saved, and Settings tabs support repository search and details, issue lists and details, and a bookmark-note sheet. Saved rows use local snapshots and need no API request. Limits are 100 bookmarks and 1,000 Unicode code points per note.

The app uses the unauthenticated GitHub REST API with the configured `2026-03-10` version and performs no writes to GitHub. Searches run on submission, results are cached for five minutes, and manual refresh bypasses the cache while respecting rate limits. Rate-limited requests show a retry time and are suppressed until then. Search results are capped at 1,000; pull requests are excluded from issue lists.

Bookmarks, notes, and display settings persist locally. Authentication, Markdown rendering, structured storage with SwiftData/Room, UI tests, detekt, and a third-party SDK wrapper example remain possible extensions.
