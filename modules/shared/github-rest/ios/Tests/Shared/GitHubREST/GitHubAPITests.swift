import CoreNetwork
import Foundation
import Synchronization
import Testing

@testable import SharedGitHubREST

/// 決まったレスポンスを返す。本文を加工した応答を試すときに使う。
private actor StubTransport: HTTPTransport {
    let response: HTTPResponse
    var requests: [HTTPRequest] = []
    init(_ response: HTTPResponse) { self.response = response }
    func send(_ request: HTTPRequest) -> HTTPResponse {
        requests.append(request)
        return response
    }
}

private func jsonObject(_ text: String) throws -> [String: Any] {
    try #require(JSONSerialization.jsonObject(with: Data(text.utf8)) as? [String: Any])
}

struct GitHubAPITests {
    // @scenario github-api/path-encoding
    @Test func encodesPathSegments() async throws {
        let transport = ScenarioTransport(ScenarioFixtures.GithubApi.pathEncoding)
        _ = try await DefaultGitHubAPI(transport: transport).repository(owner: "owner/name", repo: "repo/name")
        #expect(await transport.requests.first?.path == "/repos/owner%2Fname/repo%2Fname")
    }

    // @scenario github-api/nullable
    @Test func nullableUnknownFieldsAndLargeIDs() async throws {
        let fixture = try #require(ScenarioFixtures.GithubApi.nullable.stubs.first)
        let api = DefaultGitHubAPI(transport: ScenarioTransport(ScenarioFixtures.GithubApi.nullable))
        let page = try await api.searchRepositories(query: "swift", sort: nil, page: 1)
        #expect(page.items.first?.id == 5_000_000_001)
        #expect(page.items.first?.owner == "octocat")
        #expect(page.items.first?.description == nil)

        var body = try jsonObject(fixture.body)
        var item = try #require((body["items"] as? [[String: Any]])?.first)
        item["future_field"] = true
        body["items"] = [item]
        let unknown = DefaultGitHubAPI(
            transport: StubTransport(HTTPResponse(status: 200, body: try JSONSerialization.data(withJSONObject: body))))
        _ = try await unknown.searchRepositories(query: "swift", sort: nil, page: 1)

        item.removeValue(forKey: "description")
        body["items"] = [item]
        let missing = DefaultGitHubAPI(
            transport: StubTransport(HTTPResponse(status: 200, body: try JSONSerialization.data(withJSONObject: body))))
        await #expect(throws: DecodingError.self) {
            try await missing.searchRepositories(query: "swift", sort: nil, page: 1)
        }
    }

    // @scenario github-api/pull-requests
    @Test func prOnlyPagePreservesNextLinkAndNullableIssue() async throws {
        let api = DefaultGitHubAPI(transport: ScenarioTransport(ScenarioFixtures.GithubApi.pullRequests))
        let detail = try await api.issue(owner: "octocat", repo: "Hello-World", number: 1)
        #expect(detail.author == nil && detail.body == nil)
        let page = try await api.issues(owner: "octocat", repo: "Hello-World", state: "open", page: 1)
        #expect(page.items.isEmpty && page.nextPage == 2)
    }

    // @scenario github-api/search-limit
    @Test func searchLimitAndResponseHeaders() async throws {
        let transport = ScenarioTransport(ScenarioFixtures.GithubApi.searchLimit)
        let api = DefaultGitHubAPI(transport: GitHubTransport(underlying: transport))
        let result = try await api.searchRepositories(query: "a/b", sort: "stars", page: 50)
        #expect(result.nextPage == nil && result.limitReached && result.incomplete)
        let headers = try #require(await transport.requests.first?.headers)
        #expect(headers["X-GitHub-Api-Version"] == "2026-03-10")
        #expect(headers["Accept"] == "application/vnd.github+json")
        #expect(headers["User-Agent"] != nil)
    }

    // @scenario github-api/rate-limit
    @Test func suppressesRequestsUntilTheRetryTime() async throws {
        let step = ScenarioFixtures.GithubApi.rateLimit
        for stub in step.stubs {
            let clock = Mutex(Date(timeIntervalSince1970: 1000))
            let underlying = ScenarioTransport(ScenarioStep(id: step.id, stubs: [stub]))
            let transport = GitHubTransport(underlying: underlying, now: { clock.withLock { $0 } })
            let request = HTTPRequest(method: "GET", path: "/search/repositories")
            for _ in 0..<2 {
                do {
                    _ = try await transport.send(request)
                    Issue.record("expected rate limit")
                } catch GitHubError.rateLimited(let until) { #expect(until.timeIntervalSince1970 >= 1060) }
            }
            #expect(await underlying.requests.count == 1)
            clock.withLock { $0 += 121 }
            await #expect(throws: GitHubError.self) { try await transport.send(request) }
            #expect(await underlying.requests.count == 2)
        }
    }

    // @scenario github-api/ordinary-forbidden
    @Test func ordinaryForbiddenIsNotARateLimit() async throws {
        let underlying = ScenarioTransport(ScenarioFixtures.GithubApi.ordinaryForbidden)
        let transport = GitHubTransport(underlying: underlying)
        for _ in 0..<2 {
            do {
                _ = try await transport.send(HTTPRequest(method: "GET", path: "/repos/a/b"))
                Issue.record("expected forbidden")
            } catch GitHubError.forbidden {}
        }
        #expect(await underlying.requests.count == 2)
    }
}
