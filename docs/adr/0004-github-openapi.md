# ADR 0004: Maintain the GitHub API contract directly in OpenAPI

The GitHub browsing sample maintains `specs/openapi/openapi.yaml` as its API source of truth. It defines the four operations and required fields used by the app, based on the official OpenAPI description. It uses OpenAPI 3.0 and GitHub API version 2026-03-10.

The initial source is `descriptions/api.github.com/api.github.com.2026-03-10.json` in `github/rest-api-description` at commit `6348fd455a44a445f9e37a505e981b2c3ed24046`. This records the initial contract's provenance; it does not automatically synchronize the current contract. The license is retained in `specs/openapi/GITHUB-LICENSE.md`.

`tools/generate.ts` reads `openapi.yaml` and generates Swift/Kotlin DTOs and clients without rewriting the contract. Generation and CI checks do not depend on the network. CI checks regenerated output and shared scenarios against the contract. Unsupported syntax fails generation.

Generated DTOs and clients remain inside shared/github-rest ([ADR 0008](0008-split-github-from-core-network.md)). The handwritten GitHubAPI wrapper maps them to domain models and page information. Response headers belong to each call's result and support Link parsing and rate-limit handling.

## Update procedure

Check the official API specification and edit `specs/openapi/openapi.yaml` directly. Verify types, required fields, nullability, and enums. Set IDs to `format: int64` so both platforms use 64-bit integers. Update module specs, shared scenarios, fixtures, and handwritten wrappers as needed.

```sh
bun tools/generate.ts
mise run check
```

Include generated files with the contract change. After Swift/Kotlin changes, also run `mise run lint` and relevant native tests. When changing the GitHub API version, update both the contract's `info.version` and the GitHubTransport headers on both platforms.

To keep template maintenance simple, there is no dedicated downloader, extraction pipeline, or selection configuration for the official description. Alignment with the upstream definition is maintained through verification and review during API changes, rather than automatic checks.
