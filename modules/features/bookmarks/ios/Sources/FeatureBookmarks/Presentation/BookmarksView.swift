import DesignSystem
import SwiftUI

struct BookmarksView: View {
    @Bindable private var viewModel: BookmarksViewModel
    init(viewModel: BookmarksViewModel) { self.viewModel = viewModel }
    var body: some View {
        List {
            if viewModel.isLoading { LoadingIndicator() }
            if let error = viewModel.error { ErrorState(error) { Task { await viewModel.load() } } }
            if viewModel.dependencies.bookmarks.isLoaded && viewModel.dependencies.bookmarks.items.isEmpty {
                EmptyState("まだ保存されていません", message: "「探す」からリポジトリを開き、保存してみましょう。", symbol: "bookmark")
            }
            ForEach(viewModel.dependencies.bookmarks.items) { bookmark in
                VStack(alignment: .leading, spacing: 8) {
                    Button {
                        viewModel.select(bookmark)
                    } label: {
                        RepositoryRow(
                            repository: RepositoryRowData(
                                fullName: bookmark.repository.fullName, owner: bookmark.repository.owner,
                                avatarURL: bookmark.repository.avatarURL, description: bookmark.repository.description,
                                language: bookmark.repository.language, stars: bookmark.repository.stars),
                            saved: true,
                            showDescription: viewModel.dependencies.settings.showDescriptions,
                            loadImage: { try await viewModel.dependencies.images.load($0) })
                    }.buttonStyle(.plain)
                    if !bookmark.note.isEmpty {
                        Text(bookmark.note).font(.subheadline).lineLimit(2).foregroundStyle(.secondary)
                    }
                    HStack {
                        Button("メモを編集", systemImage: "square.and.pencil") { viewModel.edit(bookmark) }
                        Spacer()
                        Button("削除", systemImage: "trash", role: .destructive) { viewModel.pendingDelete = bookmark }
                    }.buttonStyle(.borderless).font(.subheadline)
                }
            }
        }
        .navigationTitle("保存済み")
        .task { await viewModel.load() }
        .alert(
            "保存を解除しますか？",
            isPresented: Binding(
                get: { viewModel.pendingDelete != nil }, set: { if !$0 { viewModel.pendingDelete = nil } }
            ), presenting: viewModel.pendingDelete
        ) { bookmark in
            Button("削除", role: .destructive) { Task { await viewModel.remove(bookmark.id) } }
            Button("キャンセル", role: .cancel) {}
        } message: { _ in
            Text("このリポジトリのメモも削除されます。")
        }
    }
}
