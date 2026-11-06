import CoreLocalization
import DesignSystem
import SwiftUI

struct BookmarkNoteView: View {
    @Environment(\.locale) private var locale
    @Bindable private var viewModel: BookmarkNoteViewModel
    init(viewModel: BookmarkNoteViewModel) { self.viewModel = viewModel }
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Tokens.Spacing.medium) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(Message.bookmarksNote.resolve(locale: locale)).font(.footnote).bold()
                        .foregroundStyle(Tokens.Color.muted)
                    TextField(
                        Message.bookmarksNotePrompt.resolve(locale: locale), text: $viewModel.note, axis: .vertical
                    )
                    .lineLimit(6...12).disabled(!viewModel.isLoaded || viewModel.isSaving)
                    Text(Message.bookmarksNoteCount(count: viewModel.characterCount).resolve(locale: locale))
                        .foregroundStyle(viewModel.characterCount > 1_000 ? .red : Tokens.Color.muted)
                        .font(.caption)
                }.designCard()
                if let error = viewModel.error {
                    if viewModel.isLoaded {
                        Text(error.resolve(locale: locale)).foregroundStyle(.red)
                    } else {
                        ErrorState(
                            error.resolve(locale: locale), retryLabel: Message.commonRetry.resolve(locale: locale)
                        ) {
                            Task { await viewModel.load() }
                        }
                    }
                }
                if viewModel.isSaving { LoadingIndicator(Message.commonLoading.resolve(locale: locale)) }
            }.padding(Tokens.Spacing.medium)
        }
        .background(Tokens.Color.background)
        .navigationTitle(Message.bookmarksEditNote.resolve(locale: locale)).navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(Message.commonCancel.resolve(locale: locale), action: viewModel.cancel).disabled(
                    viewModel.isSaving)
            }
            ToolbarItem(placement: .confirmationAction) {
                Button(Message.commonSave.resolve(locale: locale)) { Task { await viewModel.save() } }.disabled(
                    !viewModel.canSave)
            }
        }
        .interactiveDismissDisabled(viewModel.isSaving)
        .task { await viewModel.load() }
    }
}
