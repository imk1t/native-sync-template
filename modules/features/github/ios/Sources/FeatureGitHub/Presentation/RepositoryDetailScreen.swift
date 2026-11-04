import CoreNavigation
import SwiftUI

public struct RepositoryDetailScreen: View {
    @State private var viewModel: RepositoryDetailViewModel
    public init(owner: String, repo: String, dependencies: GitHubDependencies, navigator: Navigator) {
        _viewModel = State(
            initialValue: RepositoryDetailViewModel(
                owner: owner, repo: repo, dependencies: dependencies, navigator: navigator))
    }
    public var body: some View { RepositoryDetailView(viewModel: viewModel) }
}
