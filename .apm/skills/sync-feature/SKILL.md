---
name: sync-feature
description: Add or change feature or infrastructure behavior (screens, navigation, state, APIs, design tokens, or scenarios) with matching iOS and Android structures. Follow a spec-first workflow through sync-marker updates. Use add-module for new modules and add-third-party for external libraries.
---

# Add or change a feature with a spec-first workflow

Each module's `spec.md` is the source of truth. Update the specification before the implementation.

## Steps

1. Update the module's `spec.md`. Update `specs/openapi`, `DESIGN.md`, and `specs/scenarios` as needed.
   - APIs: Edit `specs/openapi/openapi.yaml` and run `bun tools/generate.ts` to regenerate DTOs and APIClient in `core/network`. Add domain-model mapping to its handwritten wrapper.
   - Colors and spacing: Edit the front matter of `DESIGN.md` (add each color to both `colors` and `colorsDark`) and regenerate with `bun tools/generate.ts`.
   - Screens: Add entries to `specs/routes.yaml`, regenerate, and describe screens and transitions (such as `push(...)`) under “Navigation” in the spec. ViewModels call `Navigator`. Each feature's public `*Screen` entry point creates and retains its ViewModel. Add the `Route` → entry-point mapping to both platforms' app-layer `AppRouter`. Do not create individual screens or their ViewModels in the app layer.
2. Implement one platform (`<module>/ios/` or `<module>/android/`).
3. Implement the other with **matching type names and structure**. Document intentional differences under “Platform differences” in `spec.md`.
4. If state transitions or errors change, update `specs/scenarios/*.yaml`. Give new steps an `id` and annotate both platforms' tests with `// @scenario <scenario-name>/<id>`.
5. Record alignment with `bun tools/check-spec-sync.ts --update both <module-path>` (use `ios` or `android` if only one platform is aligned).
6. Pass all applicable checks under “Commands” in AGENTS.md.
7. Include both platforms' changes in one PR. If a PR covers only one platform, link to the issue tracking the other platform's implementation.

## Notes

- `.spec-sync` contains the hash of the spec implemented by that platform. Do not run `--update` before the implementation is aligned.
- Follow the dependency rules in AGENTS.md.
