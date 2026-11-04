import CoreNetwork
import Foundation

public actor GitHubRepositoryImpl: GitHubRepository {
    private let api: any GitHubAPI
    private let now: @Sendable () -> Date
    private var searches: [String: CacheEntry<Page<RepositorySummary>>] = [:]
    private var repositories: [String: CacheEntry<RepositorySummary>] = [:]
    private var issuePages: [String: CacheEntry<Page<GitHubIssue>>] = [:]
    private var issues: [String: CacheEntry<GitHubIssue>] = [:]

    public init(api: any GitHubAPI, now: @escaping @Sendable () -> Date = { Date() }) {
        self.api = api
        self.now = now
    }

    public func search(query: String, sort: String?, page: Int, refresh: Bool) async throws -> Page<RepositorySummary> {
        let key = "\(query)\u{0}\(sort ?? "")\u{0}\(page)"
        if !refresh, let entry = searches[key], entry.expires > now() { return entry.value }
        let value = try await api.searchRepositories(query: query, sort: sort, page: page)
        try Task.checkCancellation()
        if searches.count >= 100 { searches.removeAll() }
        searches[key] = CacheEntry(value: value, expires: now().addingTimeInterval(300))
        return value
    }

    public func repository(owner: String, repo: String, refresh: Bool) async throws -> RepositorySummary {
        let key = "\(owner)/\(repo)"
        if !refresh, let entry = repositories[key], entry.expires > now() { return entry.value }
        let value = try await api.repository(owner: owner, repo: repo)
        try Task.checkCancellation()
        if repositories.count >= 100 { repositories.removeAll() }
        repositories[key] = CacheEntry(value: value, expires: now().addingTimeInterval(300))
        return value
    }

    public func issues(owner: String, repo: String, state: String, page: Int, refresh: Bool) async throws -> Page<
        GitHubIssue
    > {
        let key = "\(owner)/\(repo)/\(state)/\(page)"
        if !refresh, let entry = issuePages[key], entry.expires > now() { return entry.value }
        let value = try await api.issues(owner: owner, repo: repo, state: state, page: page)
        try Task.checkCancellation()
        if issuePages.count >= 100 { issuePages.removeAll() }
        issuePages[key] = CacheEntry(value: value, expires: now().addingTimeInterval(300))
        return value
    }

    public func issue(owner: String, repo: String, number: Int, refresh: Bool) async throws -> GitHubIssue {
        let key = "\(owner)/\(repo)/\(number)"
        if !refresh, let entry = issues[key], entry.expires > now() { return entry.value }
        let value = try await api.issue(owner: owner, repo: repo, number: number)
        try Task.checkCancellation()
        if issues.count >= 100 { issues.removeAll() }
        issues[key] = CacheEntry(value: value, expires: now().addingTimeInterval(300))
        return value
    }
}
