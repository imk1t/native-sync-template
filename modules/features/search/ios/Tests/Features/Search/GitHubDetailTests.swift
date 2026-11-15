import CoreNavigation
import Testing

@testable import FeatureSearch

@MainActor
struct GitHubDetailTests {
    // @scenario github-detail/repository
    @Test func repositoryIsLoadedOnce() async {
        let transport = ScenarioTransport(ScenarioFixtures.GithubDetail.repository)
        let model = RepositoryDetailViewModel(
            owner: "octocat", repo: "Hello-World",
            dependencies: dependencies(transport), navigator: Navigator())
        await model.loadIfNeeded()
        #expect(model.content.state.items.first?.fullName == "octocat/Hello-World")
        await model.loadIfNeeded()
        #expect(await transport.requests.count == 1)
    }

    // @scenario github-detail/not-found
    @Test func notFoundIsRetryable() async {
        let transport = ScenarioTransport(ScenarioFixtures.GithubDetail.notFound)
        let model = RepositoryDetailViewModel(
            owner: "octocat", repo: "Hello-World",
            dependencies: dependencies(transport), navigator: Navigator())
        await model.loadIfNeeded()
        #expect(model.content.state.error != nil)
        await model.refresh()
        #expect(model.content.state.error == nil)
        #expect(model.content.state.items.first?.fullName == "octocat/Hello-World")
        await model.loadIfNeeded()
        #expect(await transport.requests.count == 2)
    }

    // @scenario github-detail/issue-filter
    @Test func issueFilterFetchesOpenAndClosedSeparately() async throws {
        let transport = ScenarioTransport(ScenarioFixtures.GithubDetail.issueFilter)
        let issues = RepositoryIssuesViewModel(
            owner: "octocat", repo: "Hello-World",
            repository: dependencies(transport).repository, navigator: Navigator())
        await issues.loadIfNeeded()
        issues.issueState = "closed"
        issues.changeState()
        await issues.loadIfNeeded()
        let last = try #require(await transport.requests.last)
        #expect(last.query.contains { $0.name == "state" && $0.value == "closed" })
        #expect(last.query.contains { $0.name == "page" && $0.value == "1" })
    }

    // @scenario github-detail/issue-detail
    @Test func selectedIssueNumberOpensItsDetails() async throws {
        let transport = ScenarioTransport(ScenarioFixtures.GithubDetail.issueDetail)
        let repository = dependencies(transport).repository
        let navigator = Navigator()
        let issues = RepositoryIssuesViewModel(
            owner: "octocat", repo: "Hello-World", repository: repository, navigator: navigator)
        await issues.loadIfNeeded()
        issues.select(try #require(issues.content.state.items.first))
        #expect(
            navigator.state.stacks[.home]?.last?.route == .issueDetail(owner: "octocat", repo: "Hello-World", number: 1)
        )
        let detail = IssueDetailViewModel(owner: "octocat", repo: "Hello-World", number: 1, repository: repository)
        await detail.loadIfNeeded()
        #expect(detail.content.state.items.first?.number == 1)
    }
}
