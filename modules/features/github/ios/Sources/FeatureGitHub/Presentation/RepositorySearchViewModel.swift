import CoreNavigation
import CoreNetwork
import Foundation
import Observation

@MainActor
@Observable
public final class RepositorySearchViewModel {
    public var query = ""
    public var language = ""
    public var sort = ""
    public private(set) var revision = 0
    public private(set) var storageError: String?
    public let content = PageLoader<RepositorySummary>()
    public let dependencies: GitHubDependencies
    private let navigator: Navigator
    private var submitted: (query: String, sort: String?)?
    private var requestedPage = 1
    private var startedRevision: Int?

    public init(dependencies: GitHubDependencies, navigator: Navigator) {
        self.dependencies = dependencies
        self.navigator = navigator
    }

    public func submit() {
        content.reset()
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        submitted =
            trimmed.isEmpty
            ? nil
            : (
                trimmed + " is:public" + (language.isEmpty ? "" : " language:\(language)"),
                sort.isEmpty ? nil : sort
            )
        requestedPage = 1
        revision += 1
    }

    public func example(_ query: String) {
        self.query = query
        submit()
    }
    public func select(_ repository: RepositorySummary) {
        navigator.push(.repositoryDetail(owner: repository.owner, repo: repository.name))
    }
    public func nextPage() {
        guard !content.state.isLoading, let page = content.state.nextPage else { return }
        requestedPage = page
        revision += 1
    }
    public func retry() { revision += 1 }

    public func loadIfNeeded() async {
        do {
            try await dependencies.loadLocalData()
            storageError = nil
        } catch { storageError = "保存データの読み込みに失敗しました。再試行してください。" }
        guard startedRevision != revision, let submitted else { return }
        let current = revision
        startedRevision = current
        let page = requestedPage
        let repository = dependencies.repository
        await content.load(page: page) {
            try await repository.search(query: submitted.query, sort: submitted.sort, page: page, refresh: false)
        }
        if Task.isCancelled && current == revision { startedRevision = nil }
    }

    public func refresh() async {
        guard let submitted, !content.state.isLoading else { return }
        requestedPage = 1
        let repository = dependencies.repository
        await content.load(page: 1) {
            try await repository.search(query: submitted.query, sort: submitted.sort, page: 1, refresh: true)
        }
    }
}
