import CoreNavigation
import CoreNetwork
import Foundation
import Testing
@testable import FeatureItems

private struct StubAPI: ItemsAPI {
    var result: Result<[Item], Error>
    func listItems() async throws -> [Item] { try result.get() }
    func getItem(id: String) async throws -> Item { throw CancellationError() }
}

private actor CountingAPI: ItemsAPI {
    var calls = 0
    let suspendFirstCall: Bool

    init(suspendFirstCall: Bool = false) { self.suspendFirstCall = suspendFirstCall }

    func listItems() async throws -> [Item] {
        calls += 1
        if suspendFirstCall && calls == 1 {
            try await Task.sleep(for: .seconds(60))
        }
        return [Item(id: "1", title: "First")]
    }

    func getItem(id: String) async throws -> Item { throw CancellationError() }
}

// shared/scenarios/items-list.yaml の各 step を実装する
@MainActor
struct ItemsViewModelTests {
    // @scenario items-list/load-succeeds
    @Test func loadSucceeds() async {
        let items = [Item(id: "1", title: "First"), Item(id: "2", title: "Second")]
        let viewModel = ItemsViewModel(api: StubAPI(result: .success(items)), navigator: Navigator())
        await viewModel.load()
        #expect(viewModel.state == .loaded(items))
    }

    // @scenario items-list/load-fails
    @Test func loadFails() async {
        struct Boom: Error {}
        let viewModel = ItemsViewModel(api: StubAPI(result: .failure(Boom())), navigator: Navigator())
        await viewModel.load()
        #expect(viewModel.state == .error)
    }

    // @scenario items-list/reappear-in-same-scope
    // @scenario items-list/explicit-reload
    @Test func repeatedAppearanceKeepsStateUntilExplicitReload() async {
        let api = CountingAPI()
        let viewModel = ItemsViewModel(api: api, navigator: Navigator())
        await viewModel.loadIfNeeded()
        await viewModel.loadIfNeeded()
        #expect(await api.calls == 1)
        #expect(viewModel.state == .loaded([Item(id: "1", title: "First")]))
        await viewModel.load()
        #expect(await api.calls == 2)
    }

    // @scenario items-list/select-opens-detail
    @Test func selectOpensDetail() {
        let navigator = Navigator()
        let viewModel = ItemsViewModel(api: StubAPI(result: .success([])), navigator: navigator)
        viewModel.select(Item(id: "1", title: "First"))
        #expect(navigator.state.stacks[.home] == [.itemDetail(id: "1")])
    }

    // @scenario items-list/cancel-initial-load
    @Test func cancelledInitialLoadCanBeRetried() async {
        let api = CountingAPI(suspendFirstCall: true)
        let viewModel = ItemsViewModel(api: api, navigator: Navigator())
        let task = Task { await viewModel.loadIfNeeded() }
        while await api.calls == 0 { await Task.yield() }
        task.cancel()
        await task.value
        #expect(viewModel.state == .loading)
        await viewModel.loadIfNeeded()
        #expect(await api.calls == 2)
        #expect(viewModel.state == .loaded([Item(id: "1", title: "First")]))
    }
}
