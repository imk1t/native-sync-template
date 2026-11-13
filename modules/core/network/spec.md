# core/network

## Overview

Provide HTTP transport that does not depend on any particular API. Service-specific clients, domain models, and error classification belong in `shared/*` modules such as `shared/github-rest` ([ADR 0008](../../../docs/adr/0008-split-github-from-core-network.md)).

## Public API

- HTTPRequest / HTTPResponse retain body and headers. Response header names are lowercased. HTTPRequest carries a path and query; the transport resolves the base URL.
- HTTPTransport is the replaceable transport. The production implementation is URLSessionTransport / UrlConnectionTransport. Tests pass stubs.
- `call` helpers on HTTPTransport decode a 2xx JSON body into `APIResponse<Value>` (value and the headers of the same call, with no sharing across concurrent calls) and throw APIError / ApiException, which retain status, headers, and body, for any other status. `pathSegment` encodes one path segment, including "/". Generated clients in `shared/*` use these.
- ImageLoader fetches HTTPS image data and maintains a bounded memory cache (50 entries, 2 MB each), with no UI dependency.

## Platform differences

The iOS module is `CoreNetwork`, with implementation in `Sources/Core/Network`. The Android package is `com.example.core.network`.

Swift uses API / HTTP; Kotlin uses Api / Http. JSON uses Codable / kotlinx.serialization; HTTP uses URLSession / HttpURLConnection. Android uses 15-second connection and 30-second read timeouts, disconnecting and interrupting on cancellation. iOS follows URLSession task cancellation. Image data uses Data / ByteArray. Android tests for UrlConnectionTransport live in `android/src/test`; iOS exercises the transport through the `shared/github-rest` tests with stubs.

## Error presentation

Error types retain status, headers, and body, with no resolved display text. Wrappers in `shared/*` classify them.
