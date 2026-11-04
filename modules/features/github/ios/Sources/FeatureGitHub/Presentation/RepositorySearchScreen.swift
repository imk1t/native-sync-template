import CoreNavigation
import SwiftUI

public struct RepositorySearchScreen: View {
    @State private var viewModel: RepositorySearchViewModel
    public init(dependencies: GitHubDependencies, navigator: Navigator) {
        _viewModel = State(initialValue: RepositorySearchViewModel(dependencies: dependencies, navigator: navigator))
    }
    public var body: some View { RepositorySearchView(viewModel: viewModel) }
}
