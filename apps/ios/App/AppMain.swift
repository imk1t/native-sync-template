import CoreNetwork
import FeatureItems
import SwiftUI

// 組み立てだけを行う。API の実装は生成クライアントに置き換える（tools/generate.py の拡張）。
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
