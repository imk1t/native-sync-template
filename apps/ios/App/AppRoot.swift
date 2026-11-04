import CoreLocalization
import CoreNavigation
import SwiftUI

/// Navigator の状態を描くだけ（docs/adr/0002）。タブ・スタック・モーダルは OS の部品に任せる。
struct AppRoot<Screen: View>: View {
    @Environment(\.locale) private var locale
    @Bindable var navigator: Navigator
    let screen: (Route) -> Screen

    init(navigator: Navigator, @ViewBuilder screen: @escaping (Route) -> Screen) {
        self.navigator = navigator
        self.screen = screen
    }

    var body: some View {
        TabView(selection: Binding(
            get: { navigator.state.selectedTab },
            set: { navigator.selectTab($0) }
        )) {
            ForEach(AppTab.allCases, id: \.self) { tab in
                NavigationStack(path: path(of: tab)) {
                    screen(tab.root)
                        .id(navigator.state.root(tab).id)
                        .navigationDestination(for: NavigationEntry.self) { entry in
                            screen(entry.route).id(entry.id)
                        }
                }
                .tabItem { Label(tabMessage(tab).resolve(locale: locale), systemImage: tabIcon(tab)) }
                .tag(tab)
            }
        }
        .modifier(ModalLayer(navigator: navigator, level: 0, screen: screen))
    }

    private func tabMessage(_ tab: AppTab) -> Message {
        switch tab {
        case .home: .searchTitle
        case .saved: .commonSaved
        case .settings: .settingsTitle
        }
    }

    private func tabIcon(_ tab: AppTab) -> String {
        switch tab {
        case .home: "magnifyingglass"
        case .saved: "bookmark"
        case .settings: "gearshape"
        }
    }

    /// スワイプで戻る操作なども状態に書き戻す
    private func path(of tab: AppTab) -> Binding<[NavigationEntry]> {
        Binding(
            get: { navigator.state.stacks[tab] ?? [] },
            set: { navigator.state.stacks[tab] = $0 }
        )
    }
}

/// modals[level] を sheet / fullScreenCover で出し、その中で次の level を重ねる。
private struct ModalLayer<Screen: View>: ViewModifier {
    @Bindable var navigator: Navigator
    let level: Int
    let screen: (Route) -> Screen

    func body(content: Content) -> some View {
        content
            .sheet(isPresented: isPresented(.sheet)) { modal }
            .fullScreenCover(isPresented: isPresented(.fullScreen)) { modal }
    }

    private var current: Modal? {
        navigator.state.modals.indices.contains(level) ? navigator.state.modals[level] : nil
    }

    private func isPresented(_ style: ModalStyle) -> Binding<Bool> {
        Binding(
            get: { current?.style == style },
            set: { shown in
                // スワイプで閉じたときは、このモーダルと上に重なったモーダルを閉じる
                if !shown, navigator.state.modals.count > level {
                    navigator.state.modals.removeSubrange(level...)
                }
            }
        )
    }

    private var path: Binding<[NavigationEntry]> {
        Binding(
            get: { Array(current?.stack.dropFirst() ?? []) },
            set: { newPath in
                guard let root = current?.stack.first else { return }
                navigator.state.modals[level].stack = [root] + newPath
            }
        )
    }

    // 再帰する型を AnyView で切る
    private var modal: AnyView {
        AnyView(
            NavigationStack(path: path) {
                if let root = current?.stack.first {
                    screen(root.route)
                        .id(root.id)
                        .navigationDestination(for: NavigationEntry.self) { entry in
                            screen(entry.route).id(entry.id)
                        }
                }
            }
            .modifier(ModalLayer(navigator: navigator, level: level + 1, screen: screen))
        )
    }
}
