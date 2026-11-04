public protocol GitHubAPI: Sendable {
    func searchRepositories(query: String, sort: String?, page: Int) async throws -> Page<RepositorySummary>
    func repository(owner: String, repo: String) async throws -> RepositorySummary
    func issues(owner: String, repo: String, state: String, page: Int) async throws -> Page<GitHubIssue>
    func issue(owner: String, repo: String, number: Int) async throws -> GitHubIssue
}
