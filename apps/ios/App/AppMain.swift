import CoreNetwork
import FeatureItems
import SwiftUI

// 組み立てだけを行う。API サーバーができたら DefaultItemsAPI(transport: URLSessionTransport(baseURL: ...)) に置き換える。
private struct FakeItemsAPI: ItemsAPI {
    func listItems() async throws -> [Item] { [Item(id: "1", title: "First"), Item(id: "2", title: "Second")] }
}

@main
struct AppMain: App {
    var body: some Scene {
        WindowGroup { ItemsRoot() }
    }
}

private struct ItemsRoot: View {
    @State private var viewModel = ItemsViewModel(api: FakeItemsAPI())

    var body: some View {
        ItemsView(viewModel: viewModel)
    }
}
