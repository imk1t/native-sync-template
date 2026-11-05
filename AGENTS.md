# AGENTS.md

A monorepo for developing native iOS (Swift) and Android (Kotlin) apps together. Keep both platforms aligned using `modules/**/spec.md` as the source of truth.

## Structure

- `apps/{ios,android}`: Assembly only; no application logic.
- The app layer owns startup, the AppRoot container, shared dependencies in AppDependencies, and route mappings in AppRouter. Each feature's public `*Screen` entry point creates and retains its screens and ViewModels.
- `specs/`: Shared sources of truth: `openapi` (API), `tokens` (design tokens), `routes.yaml` (screens), `scenarios` (shared scenarios), and `third-party.yaml` (cross-platform library mappings).
- `modules/<core|shared|features>/<name>/{spec.md, ios, android}`: Features and infrastructure. `spec.md` defines the contract for platform alignment.
- `modules/shared/*`: UI, models, and logic used by multiple features. Common UI and tokens belong in `shared/designsystem`. Root-level `specs/` contains specifications and generation inputs, separate from shared implementations.
- `docs/adr/`: Architecture decision records.
- `docs/platforms/{ios,android}.md`: Platform-specific configuration, environment requirements, and pitfalls. Record new platform-specific findings here.

## Commands

Tool versions are pinned in `mise.toml`; check scripts use Bun (TypeScript). Run the following after changes:

```sh
bun tools/generate.ts     # Regenerate after changing tokens, OpenAPI, routes, or localization
mise run check            # Formatting, types, tooling tests, dependencies, generated files, spec alignment, scenarios (same as CI)
mise run lint             # SwiftLint and ktlint after Swift/Kotlin changes
mise run fmt              # bun fmt after changing TypeScript in tools/
```

Record alignment with `bun tools/check-spec-sync.ts --update both <module-path>`. Do not update a platform's marker unless its implementation matches the spec.

## Use skills for workflows

- Add or change feature/infrastructure behavior → `sync-feature` (spec first → one platform → the other with matching types and structure → checks → one PR).
- Add a module → `add-module`.
- Add a third-party library → `add-third-party`.

## Rules

- Do not edit files under `Generated/` or `generated/` by hand. Regenerate with `tools/generate.ts`.
- Keep generated DTOs and APIClient inside `core/network`. Expose domain models through handwritten wrappers such as `DefaultGitHubAPI`.
- Use `core/navigation`'s `Navigator` (`push`, `present`, etc.) for all navigation. Do not open screens directly with `NavigationLink` or similar APIs. When adding a screen, register it in `specs/routes.yaml`, document it under “Navigation” in the feature's spec, and add the app-layer `Route` → screen mapping ([ADR 0002](docs/adr/0002-navigation.md)).
- Document intentional platform differences under “Platform differences” in `spec.md`. Undocumented differences are bugs.
- Implement every step in `specs/scenarios` in both platforms' tests, annotated with `// @scenario <scenario-name>/<id>`.
- Dependencies: `core/extensions` has no internal dependencies; `core/utils` may depend only on `core/extensions`; other `core/*` may depend only on other `core/*`; `shared/designsystem` may depend only on `core/utils` and `core/extensions`; other `shared/*` may depend only on `core/*` (no shared-to-shared dependencies); `features/*` may depend only on `shared/*` and `core/*` (no feature-to-feature dependencies). Add a dependency only when it is used.
- Put general extensions of standard types in `core/extensions` and general operations exposed through dedicated types in `core/utils`. Keep feature-, business-, and UI-specific extensions in their owning modules. Use properties for queries and predicates, functions for transformations. Do not expose the same operation through both a utility and an extension.
- Use third-party libraries through `core/*` wrappers and record both platforms in `specs/third-party.yaml`. `core/extensions` and `core/utils` must not depend on third-party libraries (the standard library, Foundation, and JDK are allowed). AndroidX, Kotlin, and kotlinx are official infrastructure and may be used directly outside those two modules; they do not require library mappings. Test dependencies are exempt.
- Register iOS modules and external products (`.product(name:package:)`) in `modules/Package.swift`, and Android modules in `apps/android/settings.gradle.kts` ([ADR 0001](docs/adr/0001-monorepo-and-spec-sync.md)).
- Build Android with `./gradlew` from `apps/android` (Gradle 8.14.3 is pinned in `gradle/wrapper`; JDK 17 is required). Upgrade using `./gradlew wrapper --gradle-version <version>`, not a locally installed `gradle`.
- Use platform-standard test locations: `ios/Tests` and `android/src/test`.
- Put iOS implementations and tests in `ios/Sources/<Group>/<Name>` and `ios/Tests/<Group>/<Name>` (for example, `Core/Network`, `Features/Search`, `Shared/DesignSystem`). Use unique SwiftPM target names such as `CoreNetwork`, with explicit `path` entries in `Package.swift`.
- Android packages and namespaces follow `com.example` plus the module hierarchy (for example, `core/network` → `com.example.core.network`). Source and test directories follow the same hierarchy.
- Include both platforms' changes in one PR.
- Use exact versions in workflow `uses:` entries, such as `@v4.4.0`. After changing them, regenerate `.github/workflows/actions.lock` with `gh actions-lock`; do not edit it by hand.

## Skill management (APM)

- Sources live in `.apm/skills/<name>/SKILL.md`. Do not commit or manually edit deployed copies in `.claude/skills/` or `.agents/skills/`. Run `apm install` after changing a source.
- After cloning, run `mise install` and `mise run setup`. Users install mise themselves (see “Setup” in the README). APM is pinned in `mise.toml`. Agents must not install mise or APM.
- Commit `apm.lock.yaml`. Do not commit `.mcp.json` or `.codex/`, which are generated by APM.
- Do not run `apm compile`; this AGENTS.md is maintained by hand.
- Keep skills short. Their `description` must explain what they do and when to use them.
