import CoreNavigation
import SwiftUI

public struct IssueDetailScreen: View {
    @State private var viewModel: IssueDetailViewModel
    public init(owner: String, repo: String, number: Int, dependencies: GitHubDependencies) {
        _viewModel = State(
            initialValue: IssueDetailViewModel(
                owner: owner, repo: repo, number: number, repository: dependencies.repository))
    }
    public var body: some View { IssueDetailView(viewModel: viewModel) }
}
