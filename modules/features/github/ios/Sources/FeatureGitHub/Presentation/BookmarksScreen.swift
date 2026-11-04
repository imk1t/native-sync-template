import CoreNavigation
import SwiftUI

public struct BookmarksScreen: View {
    @State private var viewModel: BookmarksViewModel
    public init(dependencies: GitHubDependencies, navigator: Navigator) {
        _viewModel = State(initialValue: BookmarksViewModel(dependencies: dependencies, navigator: navigator))
    }
    public var body: some View { BookmarksView(viewModel: viewModel) }
}
