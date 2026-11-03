import CoreNavigation
import FeatureGitHub
import SwiftUI

struct IssueDetailScreen: View {
    @State private var viewModel: IssueDetailViewModel
    init(owner: String, repo: String, number: Int, dependencies: GitHubDependencies) {
        _viewModel = State(
            initialValue: IssueDetailViewModel(
                owner: owner, repo: repo, number: number, repository: dependencies.repository))
    }
    var body: some View { IssueDetailView(viewModel: viewModel) }
}
