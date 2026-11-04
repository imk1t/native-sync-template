import CoreLocalization
import CoreNavigation
import CoreNetwork
import Foundation
import Observation

@MainActor
@Observable
final class RepositorySearchViewModel {
    var query = ""
    var language = ""
    var sort = ""
    private(set) var revision = 0
    private(set) var storageError: Message?
    let content = PageLoader<RepositorySummary>()
    let dependencies: SearchDependencies
    private let navigator: Navigator
    private var submitted: (query: String, sort: String?)?
    private var requestedPage = 1
    private var startedRevision: Int?

    init(dependencies: SearchDependencies, navigator: Navigator) {
        self.dependencies = dependencies
        self.navigator = navigator
    }

    func submit() {
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

    func example(_ query: String) {
        self.query = query
        submit()
    }
    func select(_ repository: RepositorySummary) {
        navigator.push(.repositoryDetail(owner: repository.owner, repo: repository.name))
    }
    func nextPage() {
        guard !content.state.isLoading, let page = content.state.nextPage else { return }
        requestedPage = page
        revision += 1
    }
    func retry() { revision += 1 }

    func loadIfNeeded() async {
        do {
            try await dependencies.loadLocalData()
            storageError = nil
        } catch { storageError = Message.errorStorageLoad }
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

    func refresh() async {
        guard let submitted, !content.state.isLoading else { return }
        requestedPage = 1
        let repository = dependencies.repository
        await content.load(page: 1) {
            try await repository.search(query: submitted.query, sort: submitted.sort, page: 1, refresh: true)
        }
    }
}
