import CoreNavigation
import Observation

@MainActor
@Observable
public final class BookmarksViewModel {
    public let dependencies: GitHubDependencies
    public var pendingDelete: Bookmark?
    public private(set) var error: String?
    public private(set) var isLoading = false
    private let navigator: Navigator

    public init(dependencies: GitHubDependencies, navigator: Navigator) {
        self.dependencies = dependencies
        self.navigator = navigator
    }
    public func load() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        do {
            try await dependencies.loadLocalData()
            error = nil
        } catch { self.error = "保存データの読み込みに失敗しました。再試行してください。" }
    }
    public func select(_ bookmark: Bookmark) {
        navigator.push(.repositoryDetail(owner: bookmark.repository.owner, repo: bookmark.repository.name))
    }
    public func edit(_ bookmark: Bookmark) {
        navigator.present(.bookmarkNote(repositoryId: String(bookmark.id)), style: .sheet)
    }
    public func remove(_ id: Int64) async {
        do {
            try await dependencies.bookmarks.remove(id)
            error = nil
        } catch { self.error = "削除を保存できませんでした。再試行してください。" }
    }
}
