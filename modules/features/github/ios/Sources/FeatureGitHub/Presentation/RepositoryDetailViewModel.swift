import CoreNavigation
import CoreNetwork
import Observation

@MainActor
@Observable
public final class RepositoryDetailViewModel {
    public let content = PageLoader<RepositorySummary>()
    public let dependencies: GitHubDependencies
    public private(set) var saveError: String?
    public private(set) var isSaving = false
    private let owner: String
    private let repo: String
    private let navigator: Navigator

    public init(owner: String, repo: String, dependencies: GitHubDependencies, navigator: Navigator) {
        self.owner = owner
        self.repo = repo
        self.dependencies = dependencies
        self.navigator = navigator
    }

    public func loadIfNeeded() async {
        do { try await dependencies.bookmarks.load() } catch { saveError = "保存データを読み込めませんでした。" }
        if !content.state.hasLoaded { await load(refresh: false) }
    }
    public func refresh() async { await load(refresh: true) }
    private func load(refresh: Bool) async {
        let (repository, owner, repo) = (dependencies.repository, owner, repo)
        await content.load(page: 1) {
            Page(items: [try await repository.repository(owner: owner, repo: repo, refresh: refresh)])
        }
    }
    public func showIssues() { navigator.push(.repositoryIssues(owner: owner, repo: repo)) }
    public func toggleBookmark() async {
        guard !isSaving, let repository = content.state.items.first else { return }
        isSaving = true
        defer { isSaving = false }
        do {
            try await dependencies.bookmarks.toggle(repository)
            saveError = nil
        } catch { saveError = error.localizedDescription }
    }
}
