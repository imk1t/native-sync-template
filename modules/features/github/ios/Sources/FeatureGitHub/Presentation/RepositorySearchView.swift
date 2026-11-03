import DesignSystem
import SwiftUI

public struct RepositorySearchView: View {
    @Bindable private var viewModel: RepositorySearchViewModel
    public init(viewModel: RepositorySearchViewModel) { self.viewModel = viewModel }

    public var body: some View {
        List {
            Section {
                Picker("言語", selection: $viewModel.language) {
                    Text("すべて").tag("")
                    ForEach(["Swift", "Kotlin", "TypeScript", "Rust"], id: \.self) { Text($0).tag($0) }
                }
                Picker("並び順", selection: $viewModel.sort) {
                    Text("関連度").tag("")
                    Text("Star 数").tag("stars")
                    Text("更新日時").tag("updated")
                }
                Button("この条件で検索", systemImage: "magnifyingglass", action: viewModel.submit)
            }
            if let error = viewModel.storageError { ErrorState(error, retry: viewModel.retry) }
            if !viewModel.content.state.hasLoaded && !viewModel.content.state.isLoading
                && viewModel.content.state.error == nil {
                Section("試してみる") {
                    Text("気になるOSSを探して、自分だけのリストに保存しましょう。").foregroundStyle(.secondary)
                    ForEach(["swift", "compose", "developer tools"], id: \.self) { query in
                        Button(query) { viewModel.example(query) }
                    }
                }
            }
            Section {
                ForEach(viewModel.content.state.items) { repository in
                    Button {
                        viewModel.select(repository)
                    } label: {
                        RepositoryRow(
                            repository: repository, saved: viewModel.dependencies.bookmarks.contains(repository.id),
                            showDescription: viewModel.dependencies.settings.showDescriptions,
                            images: viewModel.dependencies.images)
                    }.buttonStyle(.plain)
                }
                PageFooter(state: viewModel.content.state, retry: viewModel.retry, next: viewModel.nextPage)
            }
        }
        .navigationTitle("探す")
        .searchable(
            text: $viewModel.query, placement: .navigationBarDrawer(displayMode: .always), prompt: "リポジトリを検索"
        )
        .onSubmit(of: .search, viewModel.submit)
        .task(id: viewModel.revision) { await viewModel.loadIfNeeded() }
        .refreshable { await viewModel.refresh() }
    }
}
