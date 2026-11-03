import DesignSystem
import SwiftUI

public struct IssueDetailView: View {
    private let viewModel: IssueDetailViewModel
    public init(viewModel: IssueDetailViewModel) { self.viewModel = viewModel }
    public var body: some View {
        List {
            if let issue = viewModel.content.state.items.first {
                Section {
                    Text(issue.title).font(.title2).bold()
                    StatusBadge(issue.state, emphasized: issue.state == "open")
                    Text("#\(String(issue.number)) · \(issue.author ?? "不明な投稿者")").foregroundStyle(.secondary)
                }
                Section("本文") {
                    Text(issue.body.flatMap { $0.isEmpty ? nil : $0 } ?? "本文はありません").textSelection(.enabled)
                }
            }
            if viewModel.content.state.isLoading { LoadingIndicator() }
            if let error = viewModel.content.state.error { ErrorState(error) { Task { await viewModel.refresh() } } }
        }
        .navigationTitle("Issue").navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.loadIfNeeded() }
        .refreshable { await viewModel.refresh() }
    }
}
