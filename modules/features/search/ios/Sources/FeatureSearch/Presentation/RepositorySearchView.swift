import CoreLocalization
import DesignSystem
import SwiftUI

struct RepositorySearchView: View {
    @Environment(\.locale) private var locale
    @Bindable private var viewModel: RepositorySearchViewModel
    init(viewModel: RepositorySearchViewModel) { self.viewModel = viewModel }

    var body: some View {
        List {
            Section {
                Picker(Message.searchLanguage.resolve(locale: locale), selection: $viewModel.language) {
                    Text(Message.searchAll.resolve(locale: locale)).tag("")
                    ForEach(["Swift", "Kotlin", "TypeScript", "Rust"], id: \.self) { Text($0).tag($0) }
                }
                Picker(Message.searchSort.resolve(locale: locale), selection: $viewModel.sort) {
                    Text(Message.searchRelevance.resolve(locale: locale)).tag("")
                    Text(Message.searchStarsSort.resolve(locale: locale)).tag("stars")
                    Text(Message.searchUpdatedSort.resolve(locale: locale)).tag("updated")
                }
                Button(
                    Message.searchSubmit.resolve(locale: locale), systemImage: "magnifyingglass",
                    action: viewModel.submit)
            }
            if let error = viewModel.storageError {
                ErrorState(
                    error.resolve(locale: locale), retryLabel: Message.commonRetry.resolve(locale: locale),
                    retry: viewModel.retry)
            }
            if !viewModel.content.state.hasLoaded && !viewModel.content.state.isLoading
                && viewModel.content.state.error == nil {
                Section(Message.searchTry.resolve(locale: locale)) {
                    Text(Message.searchIntro.resolve(locale: locale)).foregroundStyle(.secondary)
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
                            repository: RepositoryRowData(
                                fullName: repository.fullName, owner: repository.owner,
                                avatarURL: repository.avatarURL, description: repository.description,
                                language: repository.language, stars: repository.stars),
                            saved: viewModel.dependencies.bookmarks.contains(repository.id),
                            showDescription: viewModel.dependencies.settings.showDescriptions,
                            starsLabel: Message.repositoryStars(count: repository.stars).resolve(locale: locale),
                            savedLabel: Message.commonSaved.resolve(locale: locale),
                            loadImage: { try await viewModel.dependencies.images.load($0) })
                    }.buttonStyle(.plain)
                }
                PageFooter(state: viewModel.content.state, retry: viewModel.retry, next: viewModel.nextPage)
            }
        }
        .navigationTitle(Message.searchTitle.resolve(locale: locale))
        .searchable(
            text: $viewModel.query, placement: .navigationBarDrawer(displayMode: .always),
            prompt: Message.searchPrompt.resolve(locale: locale)
        )
        .onSubmit(of: .search, viewModel.submit)
        .task(id: viewModel.revision) { await viewModel.loadIfNeeded() }
        .refreshable { await viewModel.refresh() }
    }
}
