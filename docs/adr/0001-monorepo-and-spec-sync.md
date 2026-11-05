# ADR 0001: Share specifications in a monorepo

- Status: Accepted
- Decision: Keep iOS and Android in one monorepo, sharing API schemas, design tokens, and feature specifications (`spec.md`). Implement logic separately for each platform.
- Rationale: AI reduces the cost of maintaining two implementations. Native frameworks allow immediate adoption of OS features such as Liquid Glass.
- Consequences: Specifications and both implementations can change in one PR. `tools/check-spec-sync.ts` detects alignment gaps.

## iOS package structure

Separate local Swift packages for each module's `ios/` directory would collide because SwiftPM treats those identical directory names as the same package identity.
Instead, register all iOS modules as targets in `modules/Package.swift`, using each target's `path` to locate its implementation under `ios/`.
