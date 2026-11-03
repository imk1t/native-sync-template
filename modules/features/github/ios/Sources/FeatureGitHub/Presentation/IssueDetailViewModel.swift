import CoreNetwork
import Observation

@MainActor
@Observable
public final class IssueDetailViewModel {
    public let content = PageLoader<GitHubIssue>()
    private let owner: String
    private let repo: String
    private let number: Int
    private let repository: any GitHubRepository

    public init(owner: String, repo: String, number: Int, repository: any GitHubRepository) {
        self.owner = owner
        self.repo = repo
        self.number = number
        self.repository = repository
    }
    public func loadIfNeeded() async { if !content.state.hasLoaded { await load(refresh: false) } }
    public func refresh() async { await load(refresh: true) }
    private func load(refresh: Bool) async {
        let (repository, owner, repo, number) = (repository, owner, repo, number)
        await content.load(page: 1) {
            Page(items: [try await repository.issue(owner: owner, repo: repo, number: number, refresh: refresh)])
        }
    }
}
