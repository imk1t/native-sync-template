import CoreNetwork
import Foundation
import Synchronization
import Testing

@testable import SharedGitHub

private actor StubTransport: HTTPTransport {
    let response: HTTPResponse
    var requests: [HTTPRequest] = []
    init(_ response: HTTPResponse) { self.response = response }
    func send(_ request: HTTPRequest) -> HTTPResponse {
        requests.append(request)
        return response
    }
}

struct GitHubAPITests {
    @Test func encodesPathSegments() async throws {
        let body = TestResponses.repository
        let stub = StubTransport(HTTPResponse(status: 200, body: Data(body.utf8)))
        _ = try await DefaultGitHubAPI(transport: stub).repository(owner: "owner/name", repo: "repo/name")
        #expect(await stub.requests.first?.path == "/repos/owner%2Fname/repo%2Fname")
    }

    // @scenario github-api/nullable
    @Test func nullableUnknownFieldsAndLargeIDs() async throws {
        let searchJSON = TestResponses.search
        var body = try #require(JSONSerialization.jsonObject(with: Data(searchJSON.utf8)) as? [String: Any])
        var item = try #require((body["items"] as? [[String: Any]])?.first)
        item["description"] = NSNull()
        item["owner"] = NSNull()
        item["language"] = NSNull()
        item["future_field"] = true
        body["items"] = [item]
        let api = DefaultGitHubAPI(
            transport: StubTransport(
                HTTPResponse(
                    status: 200,
                    body: try JSONSerialization.data(withJSONObject: body))))
        let page = try await api.searchRepositories(query: "swift", sort: nil, page: 1)
        #expect(page.items.first?.id == 5_000_000_001)
        #expect(page.items.first?.owner == "octocat")
        #expect(page.items.first?.description == nil)
        item.removeValue(forKey: "description")
        body["items"] = [item]
        let missing = DefaultGitHubAPI(
            transport: StubTransport(
                HTTPResponse(
                    status: 200,
                    body: try JSONSerialization.data(withJSONObject: body))))
        await #expect(throws: DecodingError.self) {
            try await missing.searchRepositories(query: "swift", sort: nil, page: 1)
        }
    }

    // @scenario github-api/pull-requests
    @Test func prOnlyPagePreservesNextLinkAndNullableIssue() async throws {
        let issueJSON = TestResponses.issue
        var issue = try #require(JSONSerialization.jsonObject(with: Data(issueJSON.utf8)) as? [String: Any])
        issue["user"] = NSNull()
        issue.removeValue(forKey: "body")
        let data = try JSONSerialization.data(withJSONObject: issue)
        let detail = try await DefaultGitHubAPI(transport: StubTransport(HTTPResponse(status: 200, body: data)))
            .issue(owner: "octocat", repo: "Hello-World", number: 1)
        #expect(detail.author == nil && detail.body == nil)
        issue["pull_request"] = ["url": "https://api.github.com/repos/octocat/Hello-World/pulls/1"]
        let response = HTTPResponse(
            status: 200, body: try JSONSerialization.data(withJSONObject: [issue]),
            headers: ["Link": #"<https://api.github.com/repos/octocat/Hello-World/issues?page=2>; rel="next""#])
        let page = try await DefaultGitHubAPI(transport: StubTransport(response))
            .issues(owner: "octocat", repo: "Hello-World", state: "open", page: 1)
        #expect(page.items.isEmpty && page.nextPage == 2)
    }

    // @scenario github-api/search-limit
    @Test func searchLimitAndResponseHeaders() async throws {
        let text = TestResponses.search
            .replacingOccurrences(of: "\"total_count\":1", with: "\"total_count\":1001")
            .replacingOccurrences(of: "\"incomplete_results\":false", with: "\"incomplete_results\":true")
        let transport = StubTransport(
            HTTPResponse(
                status: 200, body: Data(text.utf8),
                headers: ["LINK": #"<https://api.github.com/search/repositories?page=51>; rel="next""#]))
        let api = DefaultGitHubAPI(transport: GitHubTransport(underlying: transport))
        let result = try await api.searchRepositories(query: "a/b", sort: "stars", page: 50)
        #expect(result.nextPage == nil && result.limitReached && result.incomplete)
        let headers = try #require(await transport.requests.first?.headers)
        #expect(headers["X-GitHub-Api-Version"] == "2026-03-10")
        #expect(headers["Accept"] == "application/vnd.github+json")
        #expect(headers["User-Agent"] != nil)
    }

    // @scenario github-api/rate-limit
    // @scenario github-api/ordinary-forbidden
    @Test func distinguishesRateLimitsAndSuppressesRequests() async throws {
        for response in [
            HTTPResponse(status: 403, headers: ["X-RateLimit-Remaining": "0", "X-RateLimit-Reset": "1120"]),
            HTTPResponse(status: 429, headers: ["Retry-After": "120"]),
            HTTPResponse(status: 403, body: Data(#"{"message":"secondary rate limit"}"#.utf8))
        ] {
            let clock = Mutex(Date(timeIntervalSince1970: 1000))
            let stub = StubTransport(response)
            let transport = GitHubTransport(underlying: stub, now: { clock.withLock { $0 } })
            let request = HTTPRequest(method: "GET", path: "/search/repositories")
            for _ in 0..<2 {
                do {
                    _ = try await transport.send(request)
                    Issue.record("expected rate limit")
                } catch GitHubError.rateLimited(let until) { #expect(until.timeIntervalSince1970 >= 1060) }
            }
            #expect(await stub.requests.count == 1)
            clock.withLock { $0 += 121 }
            await #expect(throws: GitHubError.self) { try await transport.send(request) }
            #expect(await stub.requests.count == 2)
        }
        let normal = StubTransport(HTTPResponse(status: 403))
        let transport = GitHubTransport(underlying: normal)
        for _ in 0..<2 {
            do {
                _ = try await transport.send(HTTPRequest(method: "GET", path: "/repos/a/b"))
                Issue.record("expected forbidden")
            } catch GitHubError.forbidden {}
        }
        #expect(await normal.requests.count == 2)
    }
}
