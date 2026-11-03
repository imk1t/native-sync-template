import CoreNavigation
import FeatureGitHub
import SwiftUI

struct RepositorySearchScreen: View {
    @State private var viewModel: RepositorySearchViewModel
    init(dependencies: GitHubDependencies, navigator: Navigator) {
        _viewModel = State(initialValue: RepositorySearchViewModel(dependencies: dependencies, navigator: navigator))
    }
    var body: some View { RepositorySearchView(viewModel: viewModel) }
}
