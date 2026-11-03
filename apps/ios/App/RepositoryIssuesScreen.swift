import CoreNavigation
import FeatureGitHub
import SwiftUI

struct RepositoryIssuesScreen: View {
    @State private var viewModel: RepositoryIssuesViewModel
    init(owner: String, repo: String, dependencies: GitHubDependencies, navigator: Navigator) {
        _viewModel = State(
            initialValue: RepositoryIssuesViewModel(
                owner: owner, repo: repo, repository: dependencies.repository, navigator: navigator))
    }
    var body: some View { RepositoryIssuesView(viewModel: viewModel) }
}
