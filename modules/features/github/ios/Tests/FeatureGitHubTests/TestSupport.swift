import CoreNetwork
import CoreStorage
import Foundation

@testable import FeatureGitHub

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
        return try await DemoGitHubTransport().send(request)
    }
}

actor FailingStore: KeyValueStore {
    var values: [String: String] = [:]
    var fails = false
    func failWrites() { fails = true }
    func get(_ key: String) -> String? { values[key] }
    func set(_ key: String, _ value: String?) throws {
        if fails { throw CocoaError(.fileWriteNoPermission) }
        values[key] = value
    }
}

func sample(_ id: Int64 = 5_000_000_001) -> RepositorySummary {
    RepositorySummary(id: id, name: "repo", fullName: "owner/repo", owner: "owner")
}

@MainActor
func dependencies(_ transport: any HTTPTransport = DemoGitHubTransport()) -> GitHubDependencies {
    let store = InMemoryKeyValueStore()
    return GitHubDependencies(
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
