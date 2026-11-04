import CoreNavigation
import Observation
import SharedBookmarks

@MainActor
@Observable
final class BookmarksViewModel {
    let dependencies: BookmarksDependencies
    var pendingDelete: Bookmark?
    private(set) var error: String?
    private(set) var isLoading = false
    private let navigator: Navigator

    init(dependencies: BookmarksDependencies, navigator: Navigator) {
        self.dependencies = dependencies
        self.navigator = navigator
    }
    func load() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        do {
            try await dependencies.loadLocalData()
            error = nil
        } catch { self.error = "保存データの読み込みに失敗しました。再試行してください。" }
    }
    func select(_ bookmark: Bookmark) {
        navigator.push(.repositoryDetail(owner: bookmark.repository.owner, repo: bookmark.repository.name))
    }
    func edit(_ bookmark: Bookmark) {
        navigator.present(.bookmarkNote(repositoryId: String(bookmark.id)), style: .sheet)
    }
    func remove(_ id: Int64) async {
        do {
            try await dependencies.bookmarks.remove(id)
            error = nil
        } catch { self.error = "削除を保存できませんでした。再試行してください。" }
    }
}
