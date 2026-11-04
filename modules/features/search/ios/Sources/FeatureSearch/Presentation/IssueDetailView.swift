import CoreLocalization
import DesignSystem
import SwiftUI

struct IssueDetailView: View {
    @Environment(\.locale) private var locale
    private let viewModel: IssueDetailViewModel
    init(viewModel: IssueDetailViewModel) { self.viewModel = viewModel }
    var body: some View {
        List {
            if let issue = viewModel.content.state.items.first {
                Section {
                    Text(issue.title).font(.title2).bold()
                    StatusBadge(issueStateLabel(issue.state, locale: locale), emphasized: issue.state == "open")
                    let author = issue.author ?? Message.issueUnknownAuthor.resolve(locale: locale)
                    Text("#\(String(issue.number)) · \(author)").foregroundStyle(.secondary)
                }
                Section(Message.issueBody.resolve(locale: locale)) {
                    Text(issue.body.flatMap { $0.isEmpty ? nil : $0 } ?? Message.issueEmptyBody.resolve(locale: locale))
                        .textSelection(.enabled)
                }
            }
            if viewModel.content.state.isLoading { LoadingIndicator(Message.commonLoading.resolve(locale: locale)) }
            if let error = viewModel.content.state.error {
                ErrorState(error.resolve(locale: locale), retryLabel: Message.commonRetry.resolve(locale: locale)) {
                    Task { await viewModel.refresh() }
                }
            }
        }
        .navigationTitle(Message.issueTitle.resolve(locale: locale)).navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.loadIfNeeded() }
        .refreshable { await viewModel.refresh() }
    }
}
