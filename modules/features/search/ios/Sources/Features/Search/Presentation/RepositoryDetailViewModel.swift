import CoreLocalization
import CoreNavigation
import Observation
import SharedBookmarks
import SharedGitHubREST

@MainActor
@Observable
final class RepositoryDetailViewModel {
    let content = PageLoader<RepositorySummary>()
    let dependencies: SearchDependencies
    private(set) var saveError: Message?
    private(set) var isSaving = false
    private let owner: String
    private let repo: String
    private let navigator: Navigator

    init(owner: String, repo: String, dependencies: SearchDependencies, navigator: Navigator) {
        self.owner = owner
        self.repo = repo
        self.dependencies = dependencies
        self.navigator = navigator
    }

    func loadIfNeeded() async {
        do { try await dependencies.bookmarks.load() } catch { saveError = Message.errorStorageLoad }
        if !content.state.hasLoaded { await load(refresh: false) }
    }
    func refresh() async { await load(refresh: true) }
    private func load(refresh: Bool) async {
        let (repository, owner, repo) = (dependencies.repository, owner, repo)
        await content.load(page: 1) {
            Page(items: [try await repository.repository(owner: owner, repo: repo, refresh: refresh)])
        }
    }
    func showIssues() { navigator.push(.repositoryIssues(owner: owner, repo: repo)) }
    func toggleBookmark() async {
        guard !isSaving, let repository = content.state.items.first else { return }
        isSaving = true
        defer { isSaving = false }
        do {
            try await dependencies.bookmarks.toggle(BookmarkedRepository(repository))
            saveError = nil
        } catch { saveError = bookmarkMessage(for: error, fallback: .errorStorageSave) }
    }
}
