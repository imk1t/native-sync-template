import CoreNavigation
import CoreNetwork
import Foundation
import Synchronization
import Testing

@testable import FeatureSearch

@MainActor
struct RepositorySearchViewModelTests {
    // @scenario github-search/load-succeeds
    // @scenario github-search/blank
    // @scenario github-search/change-conditions
    @Test func searchOnSubmitAndKeepState() async throws {
        let transport = RecordingTransport()
        let navigator = Navigator()
        let model = RepositorySearchViewModel(dependencies: dependencies(transport), navigator: navigator)
        for query in ["   ", "\u{0085}\u{00A0}\u{3000}\n"] {
            model.query = query
            model.submit()
            await model.loadIfNeeded()
            #expect(await transport.requests.isEmpty)
        }
        model.query = " \u{0085}\u{3000}swift\u{00A0} "
        model.language = "Swift"
        model.sort = "stars"
        model.submit()
        await model.loadIfNeeded()
        #expect(model.content.state.items.first?.id == 5_000_000_001)
        #expect(model.content.state.hasLoaded)
        #expect(!model.content.state.isLoading)
        await model.loadIfNeeded()
        #expect(await transport.requests.count == 1)
        let request = try #require(await transport.requests.first)
        #expect(request.query.contains { $0.name == "q" && $0.value == "swift is:public language:Swift" })
        #expect(request.query.contains { $0.name == "sort" && $0.value == "stars" })
        model.select(try #require(model.content.state.items.first))
        #expect(navigator.state.stacks[.home]?.last?.route == .repositoryDetail(owner: "octocat", repo: "Hello-World"))
        model.query = "kotlin"
        model.language = "Kotlin"
        model.submit()
        #expect(model.content.state.items.isEmpty)
        await model.loadIfNeeded()
        #expect(await transport.requests.count == 2)
    }

    // @scenario github-search/load-fails
    // @scenario github-search/localized-failure
    @Test func failureCanBeRetried() async {
        let transport = RecordingTransport()
        await transport.failNext(500)
        let model = RepositorySearchViewModel(dependencies: dependencies(transport), navigator: Navigator())
        model.query = "swift"
        model.submit()
        await model.loadIfNeeded()
        #expect(model.content.state.error == .message(.errorServer(status: 500)))
        #expect(!model.content.state.isLoading)
        model.retry()
        await model.loadIfNeeded()
        #expect(model.content.state.error == nil)
        #expect(model.content.state.items.count == 1)
    }

    // @scenario github-search/cache
    @Test func cacheExpiresAndRefreshBypassesIt() async throws {
        let clock = Mutex(Date(timeIntervalSince1970: 0))
        let transport = RecordingTransport()
        let repo = GitHubRepositoryImpl(api: DefaultGitHubAPI(transport: transport), now: { clock.withLock { $0 } })
        _ = try await repo.search(query: "swift", sort: nil, page: 1, refresh: false)
        _ = try await repo.search(query: "swift", sort: nil, page: 1, refresh: false)
        #expect(await transport.requests.count == 1)
        _ = try await repo.search(query: "swift", sort: nil, page: 1, refresh: true)
        #expect(await transport.requests.count == 2)
        clock.withLock { $0 += 301 }
        _ = try await repo.search(query: "swift", sort: nil, page: 1, refresh: false)
        _ = try await repo.search(query: "swift", sort: "stars", page: 1, refresh: false)
        #expect(await transport.requests.count == 4)
    }
}
