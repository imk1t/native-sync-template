import CoreNavigation
import Foundation
import SharedGitHubREST
import Synchronization
import Testing

@testable import FeatureSearch

@MainActor
struct RepositorySearchViewModelTests {
    private let blank = ScenarioStep(id: "github-search/blank", stubs: [])

    // @scenario github-search/load-succeeds
    @Test func searchOnSubmitAndKeepState() async throws {
        let transport = ScenarioTransport(ScenarioFixtures.GithubSearch.loadSucceeds)
        let navigator = Navigator()
        let model = RepositorySearchViewModel(dependencies: dependencies(transport), navigator: navigator)
        model.query = "swift"
        model.submit()
        await model.loadIfNeeded()
        #expect(model.content.state.items.map(\.id) == [5_000_000_001])
        #expect(model.content.state.hasLoaded)
        #expect(!model.content.state.isLoading)
        await model.loadIfNeeded()
        #expect(await transport.requests.count == 1)
        model.select(try #require(model.content.state.items.first))
        #expect(navigator.state.stacks[.home]?.last?.route == .repositoryDetail(owner: "octocat", repo: "Hello-World"))
    }

    // @scenario github-search/blank
    @Test func whitespaceOnlyInputIsNotSubmitted() async {
        let transport = ScenarioTransport(blank)
        let model = RepositorySearchViewModel(dependencies: dependencies(transport), navigator: Navigator())
        for query in ["   ", "\u{0085}\u{00A0}\u{3000}\n"] {
            model.query = query
            model.submit()
            await model.loadIfNeeded()
            #expect(await transport.requests.isEmpty)
        }
    }

    // @scenario github-search/change-conditions
    @Test func conditionsAreAppliedAndResultsReset() async throws {
        let transport = ScenarioTransport(ScenarioFixtures.GithubSearch.changeConditions)
        let model = RepositorySearchViewModel(dependencies: dependencies(transport), navigator: Navigator())
        model.query = " \u{0085}\u{3000}swift\u{00A0} "
        model.language = "Swift"
        model.sort = "stars"
        model.submit()
        await model.loadIfNeeded()
        let first = try #require(await transport.requests.first)
        #expect(first.query.contains { $0.name == "q" && $0.value == "swift is:public language:Swift" })
        #expect(first.query.contains { $0.name == "sort" && $0.value == "stars" })
        model.query = "kotlin"
        model.language = "Kotlin"
        model.submit()
        #expect(model.content.state.items.isEmpty)
        await model.loadIfNeeded()
        let requests = await transport.requests
        #expect(requests.count == 2)
        let last = try #require(requests.last)
        #expect(last.query.contains { $0.name == "q" && $0.value == "kotlin is:public language:Kotlin" })
        #expect(last.query.contains { $0.name == "page" && $0.value == "1" })
    }

    // @scenario github-search/load-fails
    @Test func failureCanBeRetried() async {
        let transport = ScenarioTransport(ScenarioFixtures.GithubSearch.loadFails)
        let model = RepositorySearchViewModel(dependencies: dependencies(transport), navigator: Navigator())
        model.query = "swift"
        model.submit()
        await model.loadIfNeeded()
        #expect(model.content.state.error != nil)
        #expect(!model.content.state.isLoading)
        model.retry()
        await model.loadIfNeeded()
        #expect(model.content.state.error == nil)
        #expect(model.content.state.items.count == 1)
    }

    // @scenario github-search/localized-failure
    @Test func failureKeepsMessageKeyAndStatus() async {
        let transport = ScenarioTransport(ScenarioFixtures.GithubSearch.localizedFailure)
        let model = RepositorySearchViewModel(dependencies: dependencies(transport), navigator: Navigator())
        model.query = "swift"
        model.submit()
        await model.loadIfNeeded()
        #expect(model.content.state.error == .message(.errorServer(status: 500)))
    }

    // @scenario github-search/empty
    @Test func zeroResultsIsALoadedEmptyState() async {
        let model = RepositorySearchViewModel(
            dependencies: dependencies(ScenarioTransport(ScenarioFixtures.GithubSearch.empty)), navigator: Navigator())
        model.query = "swift"
        model.submit()
        await model.loadIfNeeded()
        #expect(model.content.state.hasLoaded)
        #expect(model.content.state.items.isEmpty)
    }

    // @scenario github-search/pagination
    @Test func pagingKeepsItemsAfterFailureAndDropsDuplicates() async {
        let model = RepositorySearchViewModel(
            dependencies: dependencies(ScenarioTransport(ScenarioFixtures.GithubSearch.pagination)),
            navigator: Navigator())
        model.query = "swift"
        model.submit()
        await model.loadIfNeeded()
        #expect(model.content.state.items.map(\.id) == [1])
        #expect(model.content.state.nextPage == 2)
        model.nextPage()
        await model.loadIfNeeded()
        #expect(model.content.state.items.map(\.id) == [1])
        #expect(model.content.state.nextPage == 2)
        #expect(model.content.state.error != nil)
        model.retry()
        await model.loadIfNeeded()
        #expect(model.content.state.items.map(\.id) == [1, 2])
        #expect(model.content.state.nextPage == nil)
        #expect(model.content.state.error == nil)
        #expect(model.content.state.incomplete && model.content.state.limitReached)
    }

    // @scenario github-search/cache
    @Test func cacheExpiresAndRefreshBypassesIt() async throws {
        let clock = Mutex(Date(timeIntervalSince1970: 0))
        let transport = ScenarioTransport(ScenarioFixtures.GithubSearch.cache)
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
