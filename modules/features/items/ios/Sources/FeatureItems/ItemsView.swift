import DesignSystem
import SwiftUI

public struct ItemsView: View {
    let viewModel: ItemsViewModel

    public init(viewModel: ItemsViewModel) { self.viewModel = viewModel }

    public var body: some View {
        Group {
            switch viewModel.state {
            case .loading: ProgressView()
            case .loaded(let items): List(items, id: \.id) { Text($0.title) }
            case .error: Text("読み込みに失敗しました").foregroundStyle(Tokens.Color.primary)
            }
        }
        .task { await viewModel.loadIfNeeded() }
    }
}
