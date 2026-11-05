# core/network

## Overview

Generate clients from a directly maintained API contract based on the required subset of GitHub's official OpenAPI definition. Map responses to public repositories, issues, and page information. Do not expose generated DTOs or APIClient outside the module.

## Public API

- The app always connects to the live API at https://api.github.com. Replaceable transports and fixed responses are used only in tests; there is no app demo-launch mode.
- GitHubAPI provides searchRepositories(query, sort, page), repository(owner, repo), issues(owner, repo, state, page), and issue(owner, repo, number). DefaultGitHubAPI calls the generated code.
- `RepositorySummary`, `GitHubIssue`, and `Page<Value>`. IDs are Int64 / Long; next-page information comes from Link. Search is capped at 1,000 results and exposes incomplete_results.
- HTTPRequest / HTTPResponse retain body and headers. Successful APIResponse values contain the body and headers from the same call, with no sharing across concurrent calls.
- GitHubTransport adds Accept, User-Agent, and X-GitHub-Api-Version: 2026-03-10. Response header names are lowercased.
- Classify GitHub 403 / 429 responses using remaining quota, Retry-After, and error bodies. Rate-limit errors include the earliest retry time and are distinct from ordinary 403 errors. Suppress requests to the same resource while waiting.
- ImageLoader fetches HTTPS image data and maintains a bounded memory cache, with no UI dependency.

## Generation and errors

Edit specs/openapi/openapi.yaml as the API source of truth, then run bun tools/generate.ts to generate both platforms' DTOs and APIClient. Generation runs offline without modifying the contract. CI checks regenerated output and shared scenarios against the contract. The original official definition and update procedure are documented in docs/adr/0004-github-openapi.md; the license is retained in specs/openapi/GITHUB-LICENSE.md.

Validate optionality and nullability separately. Validate input enum values against the contract. Ignore unknown response keys and display unknown state strings unchanged. Support empty bodies and unknown authors. Exclude pull_request entries from issue lists, while retaining pagination from the original HTTP headers.

## Platform differences

The iOS module is `CoreNetwork`, with implementation in `Sources/Core/Network` and tests in `Tests/Core/Network`. The Android package is `com.example.core.network`.

Swift uses API / HTTP; Kotlin uses Api / Http. JSON uses Codable / kotlinx.serialization; HTTP uses URLSession / HttpURLConnection. Android uses 15-second connection and 30-second read timeouts, disconnecting and interrupting on cancellation. iOS follows URLSession task cancellation. Image data uses Data / ByteArray.

## Scenarios

- `specs/scenarios/github-network.yaml`

## Error presentation

Error types retain kinds and associated data, with no resolved display text. The presentation layer maps them to core/localization messages.
