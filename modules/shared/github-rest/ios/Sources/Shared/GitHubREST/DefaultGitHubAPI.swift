import CoreNetwork
import Foundation

public struct DefaultGitHubAPI: GitHubAPI {
    private let client: APIClient

    public init(transport: any HTTPTransport) { client = APIClient(transport: transport) }

    public func searchRepositories(query: String, sort: String?, page: Int) async throws -> Page<RepositorySummary> {
        let response = try await client.searchRepos(q: query, sort: sort, order: "desc", perPage: 20, page: page)
        let result = response.value
        return Page(
            items: result.items.map(Self.summary), nextPage: page < 50 ? Self.nextPage(response.headers) : nil,
            incomplete: result.incompleteResults, limitReached: result.totalCount > 1_000
        )
    }

    public func repository(owner: String, repo: String) async throws -> RepositorySummary {
        let dto = try await client.reposGet(owner: owner, repo: repo).value
        return RepositorySummary(
            id: dto.id, name: dto.name, fullName: dto.fullName, owner: dto.owner.login,
            avatarURL: dto.owner.avatarUrl, description: dto.description, language: dto.language,
            stars: dto.stargazersCount, forks: dto.forksCount, topics: dto.topics ?? [], htmlURL: dto.htmlUrl
        )
    }

    public func issues(owner: String, repo: String, state: String, page: Int) async throws -> Page<GitHubIssue> {
        let response = try await client.issuesListForRepo(
            owner: owner, repo: repo, state: state, perPage: 20, page: page)
        return Page(
            items: response.value.filter { $0.pullRequest == nil }.map(Self.issue),
            nextPage: Self.nextPage(response.headers))
    }

    public func issue(owner: String, repo: String, number: Int) async throws -> GitHubIssue {
        let dto = try await client.issuesGet(owner: owner, repo: repo, issueNumber: number).value
        guard dto.pullRequest == nil else { throw GitHubError.notFound }
        return Self.issue(dto)
    }

    private static func summary(_ dto: RepoSummaryDTO) -> RepositorySummary {
        RepositorySummary(
            id: dto.id, name: dto.name, fullName: dto.fullName,
            owner: dto.owner?.login ?? String(dto.fullName.split(separator: "/").first ?? ""),
            avatarURL: dto.owner?.avatarUrl, description: dto.description, language: dto.language,
            stars: dto.stargazersCount, forks: dto.forksCount, topics: dto.topics ?? [], htmlURL: dto.htmlUrl
        )
    }

    private static func issue(_ dto: IssueDTO) -> GitHubIssue {
        GitHubIssue(
            id: dto.id, number: dto.number, title: dto.title, state: dto.state,
            body: dto.body, author: dto.user?.login, updatedAt: dto.updatedAt)
    }

    static func nextPage(_ headers: [String: String]) -> Int? {
        for link in (headers["link"] ?? "").split(separator: ",") {
            let parts = link.split(separator: ";").map { $0.trimmingCharacters(in: .whitespaces) }
            guard parts.dropFirst().contains(where: { $0 == "rel=\"next\"" }), let first = parts.first,
                let components = URLComponents(string: first.trimmingCharacters(in: CharacterSet(charactersIn: "<>"))),
                components.scheme == "https", components.host == "api.github.com",
                let raw = components.queryItems?.first(where: { $0.name == "page" })?.value,
                let page = Int(raw), page > 0
            else { continue }
            return page
        }
        return nil
    }
}
