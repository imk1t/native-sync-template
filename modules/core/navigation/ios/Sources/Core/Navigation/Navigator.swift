import Foundation
import Observation

/// 遷移先と、その画面インスタンスの識別子。同じ Route を重ねても ID は共有しない。
public struct NavigationEntry: Hashable, Sendable, Identifiable {
    public let id: String
    public let route: Route

    public init(_ route: Route, id: String = UUID().uuidString) {
        self.id = id
        self.route = route
    }
}

public enum ModalStyle: String, Hashable, Sendable {
    case sheet
    case fullScreen
}

public struct Modal: Hashable, Sendable {
    public var style: ModalStyle
    /// 先頭がモーダルで開いた画面。モーダル表示中の push はここに積む
    public var stack: [NavigationEntry]

    public init(style: ModalStyle, stack: [NavigationEntry]) {
        self.style = style
        self.stack = stack
    }
}

/// 画面遷移の状態。タブごとのスタック（タブの先頭の画面は含めない）と、下から順に重なったモーダル。
public struct NavigationState: Hashable, Sendable {
    public var selectedTab: AppTab
    public let roots: [AppTab: NavigationEntry]
    public var stacks: [AppTab: [NavigationEntry]]
    public var modals: [Modal]

    public init(
        selectedTab: AppTab = AppTab.allCases[0],
        stacks: [AppTab: [NavigationEntry]] = [:],
        modals: [Modal] = []
    ) {
        self.selectedTab = selectedTab
        self.roots = Dictionary(uniqueKeysWithValues: AppTab.allCases.map { ($0, NavigationEntry($0.root)) })
        self.stacks = Dictionary(uniqueKeysWithValues: AppTab.allCases.map { ($0, stacks[$0] ?? []) })
        self.modals = modals
    }

    public func root(_ tab: AppTab) -> NavigationEntry { roots[tab]! }

    /// ディープリンク用。route がタブの先頭ならそのタブを、それ以外は tab の上に route を開く
    public static func opening(_ route: Route, in tab: AppTab) -> NavigationState {
        if let root = AppTab.allCases.first(where: { $0.root == route }) {
            return NavigationState(selectedTab: root)
        }
        return NavigationState(selectedTab: tab, stacks: [tab: [NavigationEntry(route)]])
    }
}

/// 画面遷移の操作。すべての遷移はここを通す（docs/adr/0002）。
@MainActor
@Observable
public final class Navigator {
    public var state: NavigationState

    public init(state: NavigationState = NavigationState()) {
        self.state = state
    }

    public func push(_ route: Route) {
        let entry = NavigationEntry(route)
        if let top = state.modals.indices.last {
            state.modals[top].stack.append(entry)
        } else {
            state.stacks[state.selectedTab, default: []].append(entry)
        }
    }

    public func pop() {
        if let top = state.modals.indices.last {
            if state.modals[top].stack.count > 1 {
                state.modals[top].stack.removeLast()
            } else {
                state.modals.removeLast()
            }
        } else if state.stacks[state.selectedTab]?.isEmpty == false {
            state.stacks[state.selectedTab]?.removeLast()
        }
    }

    public func popToRoot() {
        if let top = state.modals.indices.last {
            state.modals[top].stack = Array(state.modals[top].stack.prefix(1))
        } else {
            state.stacks[state.selectedTab] = []
        }
    }

    public func present(_ route: Route, style: ModalStyle = .sheet) {
        state.modals.append(Modal(style: style, stack: [NavigationEntry(route)]))
    }

    public func dismiss() {
        if !state.modals.isEmpty {
            state.modals.removeLast()
        }
    }

    public func selectTab(_ tab: AppTab) {
        state.modals = []
        state.selectedTab = tab
    }

    public func reset(_ state: NavigationState) {
        self.state = state
    }
}
