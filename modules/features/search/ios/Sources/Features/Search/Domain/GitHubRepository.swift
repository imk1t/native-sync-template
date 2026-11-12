import SharedGitHub

public protocol GitHubRepository: Sendable {
    func search(query: String, sort: String?, page: Int, refresh: Bool) async throws -> Page<RepositorySummary>
    func repository(owner: String, repo: String, refresh: Bool) async throws -> RepositorySummary
    func issues(owner: String, repo: String, state: String, page: Int, refresh: Bool) async throws -> Page<GitHubIssue>
    func issue(owner: String, repo: String, number: Int, refresh: Bool) async throws -> GitHubIssue
}
