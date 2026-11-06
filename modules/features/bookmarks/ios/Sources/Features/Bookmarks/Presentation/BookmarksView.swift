import CoreLocalization
import DesignSystem
import SwiftUI

struct BookmarksView: View {
    @Environment(\.locale) private var locale
    @Bindable private var viewModel: BookmarksViewModel
    init(viewModel: BookmarksViewModel) { self.viewModel = viewModel }
    var body: some View {
        List {
            if viewModel.isLoading { LoadingIndicator(Message.commonLoading.resolve(locale: locale)) }
            if let error = viewModel.error {
                ErrorState(error.resolve(locale: locale), retryLabel: Message.commonRetry.resolve(locale: locale)) {
                    Task { await viewModel.load() }
                }
            }
            if viewModel.dependencies.bookmarks.isLoaded && viewModel.dependencies.bookmarks.items.isEmpty {
                EmptyState(
                    Message.bookmarksEmptyTitle.resolve(locale: locale),
                    message: Message.bookmarksEmptyMessage.resolve(locale: locale), symbol: "bookmark")
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
                            starsLabel: Message.repositoryStars(count: bookmark.repository.stars).resolve(
                                locale: locale),
                            savedLabel: Message.commonSaved.resolve(locale: locale),
                            loadImage: { try await viewModel.dependencies.images.load($0) })
                    }.buttonStyle(.plain)
                    if !bookmark.note.isEmpty {
                        Text(bookmark.note).font(.subheadline).lineLimit(2).foregroundStyle(.secondary)
                    }
                    HStack {
                        Button(Message.bookmarksEditNote.resolve(locale: locale), systemImage: "square.and.pencil") {
                            viewModel.edit(bookmark)
                        }
                        Spacer()
                        Button(Message.commonDelete.resolve(locale: locale), systemImage: "trash", role: .destructive) {
                            viewModel.pendingDelete = bookmark
                        }
                    }.buttonStyle(.borderless).font(.subheadline)
                }
                .designCard().designListRow()
            }
        }
        .designScreenBackground()
        .navigationTitle(Message.commonSaved.resolve(locale: locale))
        .task { await viewModel.load() }
        .alert(
            Message.bookmarksDeleteTitle.resolve(locale: locale),
            isPresented: Binding(
                get: { viewModel.pendingDelete != nil }, set: { if !$0 { viewModel.pendingDelete = nil } }
            ), presenting: viewModel.pendingDelete
        ) { bookmark in
            Button(Message.commonDelete.resolve(locale: locale), role: .destructive) {
                Task { await viewModel.remove(bookmark.id) }
            }
            Button(Message.commonCancel.resolve(locale: locale), role: .cancel) {}
        } message: { _ in
            Text(Message.bookmarksDeleteMessage.resolve(locale: locale))
        }
    }
}
