# ADR 0002: Use a single navigation model through Navigator state

- Status: Accepted
- Decision:
  - Define screens (names, arguments, tabs, and deep links) in `specs/routes.yaml`. Generate `Route` and `AppTab` for both platforms with `tools/generate.ts`.
  - Use `core/navigation`'s `Navigator` for all navigation within and across features. Do not open screens directly with `NavigationLink` or similar APIs.
  - Represent navigation as `NavigationState`: per-tab stacks and layered modals, each with its own stack. Mutate it through seven `Navigator` operations: `push`, `pop`, `popToRoot`, `present`, `dismiss`, `selectTab`, and `reset`. The caller chooses between push and modal presentation.
  - Stacks contain `NavigationEntry(id, route)`. The route identifies the destination; the ID identifies the screen instance. Every push creates a UUID, even for the same route. Entries remaining after back navigation retain their IDs. NavigationState also owns each tab's root entry.
  - Associate screen state with entry IDs: NavigationStack elements and View `.id` on iOS, and NavEntry `contentKey` on Android.
  - The app layer (`apps/*`) owns startup, shared dependency assembly (AppDependencies), route-to-entry-point mappings (AppRouter), and the tab/stack/modal container (AppRoot). Each feature's public `*Screen` entry point creates and retains individual screens and ViewModels.
  - Render the container with `TabView`, `NavigationStack(path:)`, and `.sheet` / `.fullScreenCover` on iOS; use `NavigationSuiteScaffold`, Navigation 3's `NavDisplay`, and bottom sheets / full-screen dialogs on Android. Feature ViewModels follow the lifetime of the screen entry provided by that container.
  - AppDependencies passes DisplaySettings directly to settings and shares the same instance with SearchDependencies and BookmarksDependencies. Both also share `shared/bookmarks`' BookmarkRepository and the image cache, without dependencies between features.
- Rationale:
  - One approach removes uncertainty about where navigation belongs and keeps new features consistent.
  - Cross-feature navigation and deep links work without allowing feature dependencies or cycles.
  - Pure state transitions can be tested through shared scenarios, reducing platform differences.
  - Native rendering components allow adoption of OS changes such as Liquid Glass tab bars and adaptive layouts.
- Consequences:
  - Add a screen to `routes.yaml`, create its feature's `*Screen`, and register it in both AppRouters. Exhaustive `switch` / `when` checks fail the build if a mapping is missing. Do not put per-screen wrappers or ViewModel factories in the app layer.
  - Do not introduce a screen-result mechanism. Pass results through the data layer (`core/*`).
  - Restoring the back stack after process termination is out of scope. Make NavigationState persistable if that becomes necessary.

## Alternatives considered

- Allow features to open each other directly: reciprocal navigation creates dependency cycles and widens the impact of changes.
- Use direct navigation within features and routes between features: two approaches require rewrites when an internal screen becomes an external entry point.
- Expose callbacks and wire them in the app: this provides no shared basis for deep links or cross-platform consistency checks.
