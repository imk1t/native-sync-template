import CoreNavigation
import FeatureGitHub
import SwiftUI

struct BookmarkNoteScreen: View {
    @State private var viewModel: BookmarkNoteViewModel
    init(repositoryId: String, dependencies: GitHubDependencies, navigator: Navigator) {
        _viewModel = State(
            initialValue: BookmarkNoteViewModel(
                repositoryId: repositoryId, bookmarks: dependencies.bookmarks, navigator: navigator))
    }
    var body: some View { BookmarkNoteView(viewModel: viewModel) }
}
