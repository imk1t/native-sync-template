# shared/github

## Overview

Generate clients from a directly maintained API contract based on the required subset of GitHub's official OpenAPI definition. Map responses to public repositories, issues, and page information. Do not expose generated DTOs or APIClient outside the module. Transport primitives come from `core/network`.

## Navigation

None.

## Screens and state

None. This module has no screens or ViewModels.

## User actions and outcomes

None.

## APIs and tokens

- The app always connects to the live API at https://api.github.com. Replaceable transports and fixed responses are used only in tests; there is no app demo-launch mode.
- GitHubAPI provides searchRepositories(query, sort, page), repository(owner, repo), issues(owner, repo, state, page), and issue(owner, repo, number). DefaultGitHubAPI calls the generated code.
- `RepositorySummary`, `GitHubIssue`, and `Page<Value>`. IDs are Int64 / Long; next-page information comes from Link. Search is capped at 1,000 results and exposes incomplete_results.
- Successful responses contain the body and headers from the same call, with no sharing across concurrent calls (`core/network`'s APIResponse).
- GitHubTransport wraps an HTTPTransport and adds Accept, User-Agent, and X-GitHub-Api-Version: 2026-03-10.
- Classify GitHub 403 / 429 responses using remaining quota, Retry-After, and error bodies. Rate-limit errors include the earliest retry time and are distinct from ordinary 403 errors. Suppress requests to the same resource while waiting.
- Depend on `core/network` (transport) only. Other shared modules must not depend on this module; convert to their own types in the consuming feature (for example `BookmarkedRepository`).

## Generation and errors

Edit specs/openapi/openapi.yaml as the API source of truth, then run bun tools/generate.ts to generate both platforms' DTOs and APIClient into this module. Generation runs offline without modifying the contract. CI checks regenerated output and shared scenarios against the contract. The original official definition and update procedure are documented in docs/adr/0004-github-openapi.md; the license is retained in specs/openapi/GITHUB-LICENSE.md.

Validate optionality and nullability separately. Validate input enum values against the contract. Ignore unknown response keys and display unknown state strings unchanged. Support empty bodies and unknown authors. Exclude pull_request entries from issue lists, while retaining pagination from the original HTTP headers.

## Platform differences

The iOS module is `SharedGitHub`, with implementation in `Sources/Shared/GitHub` and tests in `Tests/Shared/GitHub`. The Android package is `com.example.shared.github`.

Swift uses API; Kotlin uses Api. JSON uses Codable / kotlinx.serialization.

## Scenarios

- `specs/scenarios/github-api.yaml`

## Error presentation

Error types retain kinds and associated data, with no resolved display text. The presentation layer maps them to core/localization messages.
