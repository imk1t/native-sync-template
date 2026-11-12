import CoreLocalization
import DesignSystem
import SwiftUI

struct IssueDetailView: View {
    @Environment(\.locale) private var locale
    private let viewModel: IssueDetailViewModel
    init(viewModel: IssueDetailViewModel) { self.viewModel = viewModel }
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Tokens.Spacing.medium) {
                if let issue = viewModel.content.state.items.first {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(issue.title).designFont(Tokens.Typography.h3, relativeTo: .title2)
                            .lineLimit(3).truncationMode(.tail)
                        StatusBadge(issueStateLabel(issue.state, locale: locale), emphasized: issue.state == "open")
                        let author = issue.author ?? Message.issueUnknownAuthor.resolve(locale: locale)
                        Text("#\(String(issue.number)) · \(author)").foregroundStyle(Tokens.Color.muted)
                    }.designCard()
                    VStack(alignment: .leading, spacing: 8) {
                        Text(Message.issueBody.resolve(locale: locale)).designCaption()
                            .foregroundStyle(Tokens.Color.muted)
                        Text(
                            issue.body.flatMap { $0.isEmpty ? nil : $0 }
                                ?? Message.issueEmptyBody.resolve(locale: locale)
                        ).textSelection(.enabled)
                    }.designCard()
                }
                if viewModel.content.state.isLoading { LoadingIndicator(Message.commonLoading.resolve(locale: locale)) }
                if let error = viewModel.content.state.error {
                    ErrorState(error.resolve(locale: locale), retryLabel: Message.commonRetry.resolve(locale: locale)) {
                        Task { await viewModel.refresh() }
                    }
                }
            }.padding(Tokens.Spacing.medium)
        }
        .background(Tokens.Color.background)
        .navigationTitle(Message.issueTitle.resolve(locale: locale)).navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.loadIfNeeded() }
        .refreshable { await viewModel.refresh() }
    }
}
