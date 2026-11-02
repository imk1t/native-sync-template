import DesignSystem
import SwiftUI

public struct ItemDetailView: View {
    let viewModel: ItemDetailViewModel

    public init(viewModel: ItemDetailViewModel) { self.viewModel = viewModel }

    public var body: some View {
        Group {
            switch viewModel.state {
            case .loading: ProgressView()
            case .loaded(let item):
                VStack(alignment: .leading, spacing: Tokens.Spacing.medium) {
                    Text(item.title).font(.title)
                    Text(item.id).foregroundStyle(.secondary)
                }
                .padding(Tokens.Spacing.medium)
            case .error: Text("読み込みに失敗しました").foregroundStyle(Tokens.Color.primary)
            }
        }
        .task { await viewModel.loadIfNeeded() }
    }
}
