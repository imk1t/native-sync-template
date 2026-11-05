# core/navigation

## Overview

Own navigation state and operations (docs/adr/0002). Generate screens from `specs/routes.yaml`. The app layer renders them using native components; this module has no UI dependency.

## Public API

| Name | Behavior |
| --- | --- |
| `Route` (generated) | Screens from `specs/routes.yaml`, with arguments where defined |
| `AppTab` (generated) | Tabs with a `root` route; display labels are mapped in the app layer |
| `Route(path:)` (generated) | Creates a Route from a deep-link path; returns no value if unmatched |
| `Route(url:)` | Passes the URL's encoded path to `Route(path:)`; splits the path before decoding each segment exactly once |
| `NavigationEntry` | Immutable `id: String` (UUID by default) and `route: Route`, identifying a screen instance |
| `ModalStyle` | `sheet` / `fullScreen` |
| `Modal` | `style` and `stack: [NavigationEntry]`; the first entry is the initially presented screen |
| `NavigationState` | `selectedTab`, `roots` (root entry per tab), `stacks` (entries for every tab, excluding roots), and `modals` (bottom to top) |
| `NavigationState.root(tab)` | The tab's root entry; its ID survives state updates and tab switches |
| `NavigationState.opening(route, in: tab)` | Deep-link state: select a root route's tab with an empty stack, or place one new entry on the specified tab's stack for other routes |
| `Navigator` | Owns `state`, updated through the operations below |

## Operations

| Operation | Result |
| --- | --- |
| `push(route)` | Pushes onto the selected tab's stack, or the top modal's stack if one is open |
| `pop()` | Removes one entry from the top modal's stack, dismissing the modal if it has only one entry. With no modal, removes one entry from the selected tab's stack; does nothing if empty |
| `popToRoot()` | Keeps only the top modal's first entry, or clears the selected tab's stack if no modal is open |
| `present(route, style)` | Adds a new top modal with the route as its initial entry; style defaults to `sheet` |
| `dismiss()` | Closes the top modal; does nothing if none is open |
| `selectTab(tab)` | Closes all modals and selects the tab, preserving each tab's stack |
| `reset(state)` | Replaces the entire state |

Every `push` and `present` creates a new entry ID. Opening the same Route multiple times creates independent screens. Entries remaining after `pop`, `popToRoot`, or tab changes retain their IDs. Rendering associates screen state and ViewModels with entry IDs, not Route values.

Pass encoded strings as deep-link paths. For example, `/items/a%2F1` yields ID `a/1`, while `/items/a%252F1` yields `a%2F1`.

## Platform differences

- The iOS module is `CoreNavigation`, with implementation in `Sources/Core/Navigation` and tests in `Tests/Core/Navigation`. The Android package is `com.example.core.navigation`.
- Android names follow Kotlin capitalization conventions: `Route.ItemDetail(id)`, `AppTab.Home`, and `ModalStyle.Sheet`. `Route(path:)` becomes `Route.fromPath(path)`; `opening(route, in:)` becomes `NavigationState.opening(route, tab)`.
- URL entry points are `Route(url: URL)` using URLComponents.percentEncodedPath on iOS and `Route.fromUrl(String)` using java.net.URI.rawPath on Android. The Android app passes the full Intent URI as a string.
- Integer arguments use Int on iOS and Long on Android, both 64-bit.
- iOS exposes `state` through `@Observable`; the app writes back through Binding for interactions such as swipe-to-go-back. Android exposes `StateFlow<NavigationState>` and only changes state through operations.
- If `stacks` omits tabs, iOS fills them with empty stacks; Android throws an exception.
- Rendering in AppRoot: Android retains decorated entries and ViewModelStores for all tabs, switching only the displayed NavDisplay. iOS TabView also retains each tab's screens. Feature Screen entry points create and retain ViewModels within each entry, preserving loaded state when leaving a tab.

## Scenarios

`specs/scenarios/navigation.yaml`

## Tab labels

AppTab generates only identifiers and roots. The app layer exhaustively maps display labels to core/localization Message values.
