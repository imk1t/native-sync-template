import Foundation
import SharedGitHubREST
import Testing

@testable import FeatureSearch

@MainActor
struct PageLoaderTests {
    // @scenario github-search/empty
    // @scenario github-search/pagination
    @Test func emptyAndPagingRetainContentAfterFailure() async {
        let loader = PageLoader<RepositorySummary>()
        await loader.load(page: 1) { Page(items: []) }
        #expect(loader.state.hasLoaded && loader.state.items.isEmpty)
        await loader.load(page: 1) { Page(items: [sample(1)], nextPage: 2, incomplete: true, limitReached: true) }
        #expect(loader.state.incomplete && loader.state.limitReached)
        await loader.load(page: 2) { throw GitHubError.server(500) }
        #expect(loader.state.items.map(\.id) == [1])
        #expect(loader.state.nextPage == 2)
        #expect(loader.state.error != nil)
        await loader.load(page: 2) { Page(items: [sample(1), sample(2)]) }
        #expect(loader.state.items.map(\.id) == [1, 2])
        #expect(loader.state.nextPage == nil)
        #expect(loader.state.error == nil)
    }

    // @scenario github-search/stale-response
    @Test func resetRejectsLateResponse() async throws {
        let gate = ResponseGate()
        let loader = PageLoader<RepositorySummary>()
        let old = Task { await loader.load(page: 1) { await gate.wait() } }
        for _ in 0..<10_000 {
            if await gate.started() { break }
            await Task.yield()
        }
        let started = await gate.started()
        guard started else {
            old.cancel()
            Issue.record("request did not start")
            return
        }
        loader.reset()
        await loader.load(page: 1) { Page(items: [sample(2)]) }
        await gate.finish()
        await old.value
        #expect(loader.state.items.map(\.id) == [2])
        #expect(loader.state.error == nil)
    }

    // @scenario github-search/cancellation
    @Test func cancellationIsNotAnErrorAndCanReload() async {
        let loader = PageLoader<RepositorySummary>()
        let pending = Task {
            await loader.load(page: 1) {
                try await Task.sleep(for: .seconds(60))
                return Page(items: [sample(1)])
            }
        }
        while !loader.state.isLoading { await Task.yield() }
        pending.cancel()
        await pending.value
        #expect(!loader.state.isLoading && loader.state.error == nil)
        await loader.load(page: 1) { Page(items: [sample(2)]) }
        #expect(loader.state.items.map(\.id) == [2])
    }
}
