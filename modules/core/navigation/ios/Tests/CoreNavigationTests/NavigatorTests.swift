import Foundation
import Testing

@testable import CoreNavigation

// specs/scenarios/navigation.yaml の各 step を実装する
@MainActor
struct NavigatorTests {
    private let searchSheet = Modal(style: .sheet, stack: [NavigationEntry(.repositorySearch)])

    // @scenario navigation/push-onto-selected-tab
    @Test func pushOntoSelectedTab() {
        let navigator = Navigator()
        let roots = navigator.state.roots
        navigator.push(.repositoryDetail(owner: "owner", repo: "1"))
        #expect(navigator.state.stacks[.home]?.map(\.route) == [.repositoryDetail(owner: "owner", repo: "1")])
        #expect(navigator.state.roots == roots)
        #expect(navigator.state.modals.isEmpty)
    }

    // @scenario navigation/push-inside-modal
    @Test func pushInsideModal() {
        let navigator = Navigator(state: NavigationState(modals: [searchSheet]))
        navigator.push(.repositoryDetail(owner: "owner", repo: "1"))
        #expect(navigator.state.stacks[.home] == [])
        #expect(
            navigator.state.modals[0].stack.map(\.route) == [
                .repositorySearch, .repositoryDetail(owner: "owner", repo: "1")
            ])
        #expect(navigator.state.modals[0].stack[0] == searchSheet.stack[0])
    }

    // @scenario navigation/pop-modal-root-dismisses
    @Test func popModalRootDismisses() {
        let navigator = Navigator(state: NavigationState(modals: [searchSheet]))
        navigator.pop()
        #expect(navigator.state.modals.isEmpty)
    }

    // @scenario navigation/pop-on-empty-stack
    @Test func popOnEmptyStack() {
        let navigator = Navigator()
        let initial = navigator.state
        navigator.pop()
        #expect(navigator.state == initial)
    }

    // @scenario navigation/pop-to-root
    @Test func popToRoot() {
        let navigator = Navigator()
        let root = navigator.state.root(.home)
        navigator.push(.repositoryDetail(owner: "owner", repo: "1"))
        navigator.push(.repositoryDetail(owner: "owner", repo: "2"))
        navigator.popToRoot()
        #expect(navigator.state.stacks[.home] == [])
        #expect(navigator.state.root(.home) == root)
    }

    // @scenario navigation/present-and-dismiss
    @Test func presentAndDismiss() {
        let navigator = Navigator()
        navigator.present(.repositoryDetail(owner: "owner", repo: "1"), style: .fullScreen)
        let first = navigator.state.modals[0]
        navigator.present(.repositorySearch)
        navigator.dismiss()
        #expect(navigator.state.modals == [first])
        #expect(first.style == .fullScreen)
        #expect(first.stack.map(\.route) == [.repositoryDetail(owner: "owner", repo: "1")])
    }

    // @scenario navigation/select-tab-dismisses-modals
    @Test func selectTabDismissesModals() {
        let state = NavigationState(
            stacks: [.home: [NavigationEntry(.repositoryDetail(owner: "owner", repo: "1"))]], modals: [searchSheet])
        let navigator = Navigator(state: state)
        navigator.selectTab(.home)
        #expect(navigator.state.selectedTab == .home)
        #expect(navigator.state.stacks == state.stacks)
        #expect(navigator.state.roots == state.roots)
        #expect(navigator.state.modals.isEmpty)
    }

    // @scenario navigation/open-deep-link
    @Test func openDeepLink() throws {
        let detail = try #require(Route(path: "/repositories/owner/a%2F1"))
        let state = NavigationState.opening(detail, in: .home)
        #expect(state.stacks[.home]?.map(\.route) == [.repositoryDetail(owner: "owner", repo: "a/1")])
        let list = try #require(Route(path: "/search"))
        #expect(NavigationState.opening(list, in: .home).stacks[.home] == [])
        #expect(Route(path: "/unknown") == nil)
    }

    // @scenario navigation/open-url-with-encoded-id
    @Test func openURLDecodesEachSegmentOnlyOnce() throws {
        for (encoded, id) in [("a%2F1", "a/1"), ("a%252F1", "a%2F1"), ("a%2B1", "a+1")] {
            let url = try #require(URL(string: "https://example.com/repositories/owner/\(encoded)?source=test#detail"))
            let route = try #require(Route(url: url))
            #expect(route == .repositoryDetail(owner: "owner", repo: id))
            #expect(NavigationState.opening(route, in: .home).stacks[.home]?.map(\.route) == [route])
        }
        #expect(Route(url: try #require(URL(string: "https://example.com/unknown"))) == nil)
    }

    // @scenario navigation/duplicate-route-entries
    @Test func duplicateRoutesHaveIndependentStableIdentities() throws {
        let navigator = Navigator()
        let route = Route.repositoryDetail(owner: "owner", repo: "1")
        navigator.push(route)
        let first = try #require(navigator.state.stacks[.home]?.last)
        navigator.push(route)
        let second = try #require(navigator.state.stacks[.home]?.last)
        #expect(first.route == second.route)
        #expect(first.id != second.id)
        navigator.pop()
        #expect(navigator.state.stacks[.home] == [first])
        navigator.push(route)
        #expect(navigator.state.stacks[.home]?.last?.id != second.id)

        navigator.present(route)
        let modalRoot = navigator.state.modals[0].stack[0]
        navigator.push(route)
        #expect(navigator.state.modals[0].stack[1].id != modalRoot.id)
        navigator.popToRoot()
        #expect(navigator.state.modals[0].stack == [modalRoot])
    }
}
