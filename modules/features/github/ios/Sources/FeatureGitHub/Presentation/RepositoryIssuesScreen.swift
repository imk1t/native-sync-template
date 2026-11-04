import CoreNavigation
import SwiftUI

public struct RepositoryIssuesScreen: View {
    @State private var viewModel: RepositoryIssuesViewModel
    public init(owner: String, repo: String, dependencies: GitHubDependencies, navigator: Navigator) {
        _viewModel = State(
            initialValue: RepositoryIssuesViewModel(
                owner: owner, repo: repo, repository: dependencies.repository, navigator: navigator))
    }
    public var body: some View { RepositoryIssuesView(viewModel: viewModel) }
}
