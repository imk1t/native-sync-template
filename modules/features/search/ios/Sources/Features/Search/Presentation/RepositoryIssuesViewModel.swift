import CoreNavigation
import Observation
import SharedGitHub

@MainActor
@Observable
final class RepositoryIssuesViewModel {
    var issueState = "open"
    private(set) var revision = 0
    let content = PageLoader<GitHubIssue>()
    private let owner: String
    private let repo: String
    private let repository: any GitHubRepository
    private let navigator: Navigator
    private var page = 1
    private var startedRevision: Int?

    init(owner: String, repo: String, repository: any GitHubRepository, navigator: Navigator) {
        self.owner = owner
        self.repo = repo
        self.repository = repository
        self.navigator = navigator
    }
    func changeState() {
        content.reset()
        page = 1
        revision += 1
    }
    func select(_ issue: GitHubIssue) {
        navigator.push(.issueDetail(owner: owner, repo: repo, number: issue.number))
    }
    func nextPage() {
        guard !content.state.isLoading, let next = content.state.nextPage else { return }
        page = next
        revision += 1
    }
    func retry() { revision += 1 }
    func loadIfNeeded() async {
        guard startedRevision != revision else { return }
        let current = revision
        startedRevision = current
        await load(page: page, refresh: false)
        if Task.isCancelled && current == revision { startedRevision = nil }
    }
    func refresh() async {
        guard !content.state.isLoading else { return }
        page = 1
        await load(page: 1, refresh: true)
    }
    private func load(page: Int, refresh: Bool) async {
        let (repository, owner, repo, state) = (repository, owner, repo, issueState)
        await content.load(page: page) {
            try await repository.issues(owner: owner, repo: repo, state: state, page: page, refresh: refresh)
        }
    }
}
