import Testing
@testable import CoreNavigation

// shared/scenarios/navigation.yaml の各 step を実装する
@MainActor
struct NavigatorTests {
    private let sheetWithItems = Modal(style: .sheet, stack: [.items])

    // @scenario navigation/push-onto-selected-tab
    @Test func pushOntoSelectedTab() {
        let navigator = Navigator()
        navigator.push(.itemDetail(id: "1"))
        #expect(navigator.state == NavigationState(stacks: [.home: [.itemDetail(id: "1")]]))
    }

    // @scenario navigation/push-inside-modal
    @Test func pushInsideModal() {
        let navigator = Navigator(state: NavigationState(modals: [sheetWithItems]))
        navigator.push(.itemDetail(id: "1"))
        #expect(navigator.state.stacks[.home] == [])
        #expect(navigator.state.modals == [Modal(style: .sheet, stack: [.items, .itemDetail(id: "1")])])
    }

    // @scenario navigation/pop-modal-root-dismisses
    @Test func popModalRootDismisses() {
        let navigator = Navigator(state: NavigationState(modals: [sheetWithItems]))
        navigator.pop()
        #expect(navigator.state.modals.isEmpty)
    }

    // @scenario navigation/pop-on-empty-stack
    @Test func popOnEmptyStack() {
        let navigator = Navigator()
        navigator.pop()
        #expect(navigator.state == NavigationState())
    }

    // @scenario navigation/pop-to-root
    @Test func popToRoot() {
        let navigator = Navigator(state: NavigationState(stacks: [.home: [.itemDetail(id: "1"), .itemDetail(id: "2")]]))
        navigator.popToRoot()
        #expect(navigator.state.stacks[.home] == [])
    }

    // @scenario navigation/present-and-dismiss
    @Test func presentAndDismiss() {
        let navigator = Navigator()
        navigator.present(.itemDetail(id: "1"), style: .fullScreen)
        navigator.present(.items)
        navigator.dismiss()
        #expect(navigator.state.modals == [Modal(style: .fullScreen, stack: [.itemDetail(id: "1")])])
    }

    // @scenario navigation/select-tab-dismisses-modals
    @Test func selectTabDismissesModals() {
        let state = NavigationState(stacks: [.home: [.itemDetail(id: "1")]], modals: [sheetWithItems])
        let navigator = Navigator(state: state)
        navigator.selectTab(.home)
        #expect(navigator.state == NavigationState(selectedTab: .home, stacks: [.home: [.itemDetail(id: "1")]]))
    }

    // @scenario navigation/open-deep-link
    @Test func openDeepLink() throws {
        let detail = try #require(Route(path: "/items/a%2F1"))
        let expected = NavigationState(stacks: [.home: [.itemDetail(id: "a/1")]])
        #expect(NavigationState.opening(detail, in: .home) == expected)
        let list = try #require(Route(path: "/items"))
        #expect(NavigationState.opening(list, in: .home) == NavigationState(selectedTab: .home))
        #expect(Route(path: "/unknown") == nil)
    }
}
