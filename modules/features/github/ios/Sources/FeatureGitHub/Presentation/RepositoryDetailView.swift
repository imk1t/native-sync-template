import DesignSystem
import SwiftUI

struct RepositoryDetailView: View {
    @Bindable private var viewModel: RepositoryDetailViewModel
    init(viewModel: RepositoryDetailViewModel) { self.viewModel = viewModel }

    var body: some View {
        List {
            if let repository = viewModel.content.state.items.first {
                Section {
                    RepositoryRow(
                        repository: repository, saved: viewModel.dependencies.bookmarks.contains(repository.id),
                        showDescription: true, images: viewModel.dependencies.images)
                    LabeledContent("Fork", value: repository.forks.formatted())
                    if !repository.topics.isEmpty {
                        ScrollView(.horizontal) {
                            HStack { ForEach(repository.topics, id: \.self) { StatusBadge($0) } }
                        }
                    }
                }
                Section {
                    Button {
                        Task { await viewModel.toggleBookmark() }
                    } label: {
                        Label(
                            viewModel.dependencies.bookmarks.contains(repository.id) ? "保存を解除" : "保存する",
                            systemImage: viewModel.dependencies.bookmarks.contains(repository.id)
                                ? "bookmark.fill" : "bookmark")
                    }.disabled(viewModel.isSaving)
                    Button("Issue を見る", systemImage: "exclamationmark.bubble", action: viewModel.showIssues)
                }
            }
            if let error = viewModel.saveError { Text(error).foregroundStyle(.red) }
            if viewModel.content.state.isLoading { LoadingIndicator() }
            if let error = viewModel.content.state.error { ErrorState(error) { Task { await viewModel.refresh() } } }
        }
        .navigationTitle("リポジトリ").navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.loadIfNeeded() }
        .refreshable { await viewModel.refresh() }
    }
}
