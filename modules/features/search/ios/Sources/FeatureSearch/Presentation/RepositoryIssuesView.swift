import DesignSystem
import SwiftUI

struct RepositoryIssuesView: View {
    @Bindable private var viewModel: RepositoryIssuesViewModel
    init(viewModel: RepositoryIssuesViewModel) { self.viewModel = viewModel }

    var body: some View {
        List {
            Picker("状態", selection: $viewModel.issueState) {
                Text("Open").tag("open")
                Text("Closed").tag("closed")
            }.pickerStyle(.segmented).onChange(of: viewModel.issueState) { viewModel.changeState() }
            ForEach(viewModel.content.state.items) { issue in
                Button {
                    viewModel.select(issue)
                } label: {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(issue.title).font(.headline).foregroundStyle(.primary)
                        HStack {
                            StatusBadge(issue.state, emphasized: issue.state == "open")
                            Text("#\(String(issue.number))").foregroundStyle(.secondary)
                        }
                        Text(issue.updatedAt).font(.caption).foregroundStyle(.secondary)
                    }.padding(.vertical, 4)
                }.buttonStyle(.plain)
            }
            PageFooter(state: viewModel.content.state, retry: viewModel.retry, next: viewModel.nextPage)
        }
        .navigationTitle("Issues").navigationBarTitleDisplayMode(.inline)
        .task(id: viewModel.revision) { await viewModel.loadIfNeeded() }
        .refreshable { await viewModel.refresh() }
    }
}
