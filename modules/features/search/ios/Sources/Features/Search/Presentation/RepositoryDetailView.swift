import CoreLocalization
import DesignSystem
import SwiftUI

struct RepositoryDetailView: View {
    @Environment(\.locale) private var locale
    @Bindable private var viewModel: RepositoryDetailViewModel
    init(viewModel: RepositoryDetailViewModel) { self.viewModel = viewModel }

    var body: some View {
        List {
            if let repository = viewModel.content.state.items.first {
                Section {
                    VStack(alignment: .leading, spacing: 12) {
                        RepositoryRow(
                            repository: RepositoryRowData(
                                fullName: repository.fullName, owner: repository.owner,
                                avatarURL: repository.avatarURL, description: repository.description,
                                language: repository.language, stars: repository.stars),
                            saved: viewModel.dependencies.bookmarks.contains(repository.id),
                            showDescription: true,
                            starsLabel: Message.repositoryStars(count: repository.stars).resolve(locale: locale),
                            savedLabel: Message.commonSaved.resolve(locale: locale),
                            loadImage: { try await viewModel.dependencies.images.load($0) })
                        Text(Message.repositoryForks(count: repository.forks).resolve(locale: locale))
                            .designBody(relativeTo: .subheadline).foregroundStyle(Tokens.Color.muted)
                        if !repository.topics.isEmpty {
                            ScrollView(.horizontal) {
                                HStack { ForEach(repository.topics, id: \.self) { StatusBadge($0) } }
                            }
                        }
                    }.designCard().designListRow()
                }
                Section {
                    Button {
                        Task { await viewModel.toggleBookmark() }
                    } label: {
                        Label(
                            viewModel.dependencies.bookmarks.contains(repository.id)
                                ? Message.repositoryUnsave.resolve(locale: locale)
                                : Message.repositorySave.resolve(locale: locale),
                            systemImage: viewModel.dependencies.bookmarks.contains(repository.id)
                                ? "bookmark.fill" : "bookmark")
                    }.disabled(viewModel.isSaving)
                    Button(
                        Message.repositoryIssues.resolve(locale: locale), systemImage: "exclamationmark.bubble",
                        action: viewModel.showIssues)
                }
            }
            if let error = viewModel.saveError { Text(error.resolve(locale: locale)).foregroundStyle(.red) }
            if viewModel.content.state.isLoading { LoadingIndicator(Message.commonLoading.resolve(locale: locale)) }
            if let error = viewModel.content.state.error {
                ErrorState(error.resolve(locale: locale), retryLabel: Message.commonRetry.resolve(locale: locale)) {
                    Task { await viewModel.refresh() }
                }
            }
        }
        .designScreenBackground()
        .navigationTitle(Message.repositoryTitle.resolve(locale: locale)).navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.loadIfNeeded() }
        .refreshable { await viewModel.refresh() }
    }
}
