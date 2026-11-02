import CoreNavigation
import CoreNetwork
import FeatureItems
import SwiftUI

// 組み立てだけを行う。API サーバーができたら DefaultItemsAPI(transport: URLSessionTransport(baseURL: ...)) に置き換える。
private struct FakeItemsAPI: ItemsAPI {
    private let items = [Item(id: "1", title: "First"), Item(id: "2", title: "Second")]

    func listItems() async throws -> [Item] { items }

    func getItem(id: String) async throws -> Item {
        guard let item = items.first(where: { $0.id == id }) else { throw APIError(status: 404) }
        return item
    }
}

@main
struct AppMain: App {
    @State private var navigator = Navigator()
    private let api: any ItemsAPI = FakeItemsAPI()

    var body: some Scene {
        WindowGroup {
            RootView(navigator: navigator) { route in
                Screen(route: route, api: api, navigator: navigator)
            }
            // ディープリンク。URL スキームや Universal Links の登録はアプリごとに行う。
            .onOpenURL { url in
                if let route = Route(path: url.path) {
                    navigator.reset(.opening(route, in: navigator.state.selectedTab))
                }
            }
        }
    }
}

/// Route → 画面の対応。全機能の画面を知っているのはここだけ（docs/adr/0002）。
/// 画面ごとに @State で ViewModel を所有し、スタックから外れると破棄される。
private struct Screen: View {
    let route: Route
    let api: any ItemsAPI
    let navigator: Navigator

    var body: some View {
        switch route {
        case .items: ItemsScreen(api: api, navigator: navigator)
        case .itemDetail(let id): ItemDetailScreen(id: id, api: api)
        }
    }
}

private struct ItemsScreen: View {
    @State private var viewModel: ItemsViewModel

    init(api: any ItemsAPI, navigator: Navigator) {
        _viewModel = State(initialValue: ItemsViewModel(api: api, navigator: navigator))
    }

    var body: some View { ItemsView(viewModel: viewModel) }
}

private struct ItemDetailScreen: View {
    @State private var viewModel: ItemDetailViewModel

    init(id: String, api: any ItemsAPI) {
        _viewModel = State(initialValue: ItemDetailViewModel(id: id, api: api))
    }

    var body: some View { ItemDetailView(viewModel: viewModel) }
}
