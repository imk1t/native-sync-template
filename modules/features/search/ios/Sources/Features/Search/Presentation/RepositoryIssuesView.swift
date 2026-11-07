import CoreLocalization
import DesignSystem
import SwiftUI

struct RepositoryIssuesView: View {
    @Environment(\.locale) private var locale
    @Bindable private var viewModel: RepositoryIssuesViewModel
    init(viewModel: RepositoryIssuesViewModel) { self.viewModel = viewModel }

    var body: some View {
        List {
            Picker(Message.issueState.resolve(locale: locale), selection: $viewModel.issueState) {
                Text(Message.issueOpen.resolve(locale: locale)).tag("open")
                Text(Message.issueClosed.resolve(locale: locale)).tag("closed")
            }.pickerStyle(.segmented).onChange(of: viewModel.issueState) { viewModel.changeState() }
                .designListRow()
            ForEach(viewModel.content.state.items) { issue in
                Button {
                    viewModel.select(issue)
                } label: {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(issue.title).font(.headline).foregroundStyle(.primary)
                        HStack {
                            StatusBadge(issueStateLabel(issue.state, locale: locale), emphasized: issue.state == "open")
                            Text("#\(String(issue.number))").foregroundStyle(Tokens.Color.muted)
                        }
                        Text(issue.updatedAt).designCaption().foregroundStyle(Tokens.Color.muted)
                    }.designCard()
                }.buttonStyle(.plain).designListRow()
            }
            PageFooter(state: viewModel.content.state, retry: viewModel.retry, next: viewModel.nextPage)
        }
        .listStyle(.plain).designScreenBackground()
        .navigationTitle(Message.issuesTitle.resolve(locale: locale)).navigationBarTitleDisplayMode(.inline)
        .task(id: viewModel.revision) { await viewModel.loadIfNeeded() }
        .refreshable { await viewModel.refresh() }
    }
}
