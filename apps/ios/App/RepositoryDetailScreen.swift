import CoreNavigation
import FeatureGitHub
import SwiftUI

struct RepositoryDetailScreen: View {
    @State private var viewModel: RepositoryDetailViewModel
    init(owner: String, repo: String, dependencies: GitHubDependencies, navigator: Navigator) {
        _viewModel = State(
            initialValue: RepositoryDetailViewModel(
                owner: owner, repo: repo, dependencies: dependencies, navigator: navigator))
    }
    var body: some View { RepositoryDetailView(viewModel: viewModel) }
}
