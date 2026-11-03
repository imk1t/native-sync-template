import CoreNavigation
import Testing

@testable import FeatureGitHub

@MainActor
struct GitHubDetailTests {
    // @scenario github-detail/repository
    // @scenario github-detail/not-found
    @Test func repositoryLoadsOnceAndNotFoundIsRetryable() async {
        let transport = RecordingTransport()
        let model = RepositoryDetailViewModel(
            owner: "octocat", repo: "Hello-World",
            dependencies: dependencies(transport), navigator: Navigator())
        await transport.failNext(404)
        await model.loadIfNeeded()
        #expect(model.content.state.error != nil)
        await model.refresh()
        #expect(model.content.state.items.first?.fullName == "octocat/Hello-World")
        await model.loadIfNeeded()
        #expect(await transport.requests.count == 2)
    }

    // @scenario github-detail/issue-filter
    // @scenario github-detail/issue-detail
    @Test func issueFilterAndDetail() async throws {
        let transport = RecordingTransport()
        let deps = dependencies(transport)
        let navigator = Navigator()
        let issues = RepositoryIssuesViewModel(
            owner: "octocat", repo: "Hello-World",
            repository: deps.repository, navigator: navigator)
        await issues.loadIfNeeded()
        issues.select(try #require(issues.content.state.items.first))
        #expect(
            navigator.state.stacks[.home]?.last?.route == .issueDetail(owner: "octocat", repo: "Hello-World", number: 1)
        )
        issues.issueState = "closed"
        issues.changeState()
        await issues.loadIfNeeded()
        #expect(await transport.requests.last?.query.contains { $0.name == "state" && $0.value == "closed" } == true)
        let detail = IssueDetailViewModel(owner: "octocat", repo: "Hello-World", number: 1, repository: deps.repository)
        await detail.loadIfNeeded()
        #expect(detail.content.state.items.first?.number == 1)
    }
}
