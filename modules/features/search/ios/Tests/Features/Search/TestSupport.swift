import CoreNetwork
import CoreStorage
import Foundation
import SharedBookmarks
import SharedGitHubREST

@testable import FeatureSearch

func sample(_ id: Int64 = 5_000_000_001) -> RepositorySummary {
    RepositorySummary(id: id, name: "repo", fullName: "owner/repo", owner: "owner")
}

@MainActor
func dependencies(_ transport: any HTTPTransport) -> SearchDependencies {
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
