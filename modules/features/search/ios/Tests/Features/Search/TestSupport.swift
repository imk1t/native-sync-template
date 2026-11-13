import CoreNetwork
import CoreStorage
import Foundation
import SharedBookmarks
import SharedGitHubREST

@testable import FeatureSearch

actor RecordingTransport: HTTPTransport {
    var requests: [HTTPRequest] = []
    var failure: Int?
    func failNext(_ status: Int) { failure = status }
    func send(_ request: HTTPRequest) async throws -> HTTPResponse {
        requests.append(request)
        if let failure {
            self.failure = nil
            return HTTPResponse(status: failure, body: Data())
        }
        let body: String
        switch request.path {
        case "/search/repositories": body = TestResponses.search
        case "/repos/octocat/Hello-World": body = TestResponses.repository
        case "/repos/octocat/Hello-World/issues": body = "[\(TestResponses.issue)]"
        case "/repos/octocat/Hello-World/issues/1": body = TestResponses.issue
        default: return HTTPResponse(status: 404)
        }
        return HTTPResponse(status: 200, body: Data(body.utf8))
    }
}

func sample(_ id: Int64 = 5_000_000_001) -> RepositorySummary {
    RepositorySummary(id: id, name: "repo", fullName: "owner/repo", owner: "owner")
}

@MainActor
func dependencies(_ transport: any HTTPTransport = RecordingTransport()) -> SearchDependencies {
    let store = InMemoryKeyValueStore()
    return SearchDependencies(
        repository: GitHubRepositoryImpl(
            api: DefaultGitHubAPI(
                transport: GitHubTransport(underlying: transport)
            )), bookmarks: BookmarkRepository(store: store), settings: DisplaySettings(store: store))
}

actor ResponseGate {
    var continuation: CheckedContinuation<Page<RepositorySummary>, Never>?
    func wait() async -> Page<RepositorySummary> {
        await withCheckedContinuation { continuation = $0 }
    }
    func started() -> Bool { continuation != nil }
    func finish() {
        continuation?.resume(returning: Page(items: [sample(1)]))
        continuation = nil
    }
}
