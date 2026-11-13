import Observation
import SharedGitHubREST

@MainActor
@Observable
final class IssueDetailViewModel {
    let content = PageLoader<GitHubIssue>()
    private let owner: String
    private let repo: String
    private let number: Int
    private let repository: any GitHubRepository

    init(owner: String, repo: String, number: Int, repository: any GitHubRepository) {
        self.owner = owner
        self.repo = repo
        self.number = number
        self.repository = repository
    }
    func loadIfNeeded() async { if !content.state.hasLoaded { await load(refresh: false) } }
    func refresh() async { await load(refresh: true) }
    private func load(refresh: Bool) async {
        let (repository, owner, repo, number) = (repository, owner, repo, number)
        await content.load(page: 1) {
            Page(items: [try await repository.issue(owner: owner, repo: repo, number: number, refresh: refresh)])
        }
    }
}
