import CoreNavigation
import FeatureGitHub
import SwiftUI

struct BookmarksScreen: View {
    @State private var viewModel: BookmarksViewModel
    init(dependencies: GitHubDependencies, navigator: Navigator) {
        _viewModel = State(initialValue: BookmarksViewModel(dependencies: dependencies, navigator: navigator))
    }
    var body: some View { BookmarksView(viewModel: viewModel) }
}
