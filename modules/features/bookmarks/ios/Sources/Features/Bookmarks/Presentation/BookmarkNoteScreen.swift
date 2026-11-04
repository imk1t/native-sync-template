import CoreNavigation
import SwiftUI

public struct BookmarkNoteScreen: View {
    @State private var viewModel: BookmarkNoteViewModel
    public init(repositoryId: String, dependencies: BookmarksDependencies, navigator: Navigator) {
        _viewModel = State(
            initialValue: BookmarkNoteViewModel(
                repositoryId: repositoryId, bookmarks: dependencies.bookmarks, navigator: navigator))
    }
    public var body: some View { BookmarkNoteView(viewModel: viewModel) }
}
