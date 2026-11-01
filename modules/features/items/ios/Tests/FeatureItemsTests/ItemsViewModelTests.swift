import CoreNetwork
import Foundation
import Testing
@testable import FeatureItems

private struct StubAPI: ItemsAPI {
    var result: Result<[Item], Error>
    func listItems() async throws -> [Item] { try result.get() }
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
}

// shared/scenarios/items-list.yaml と同じ内容
@MainActor
struct ItemsViewModelTests {
    @Test func loadSucceeds() async {
        let vm = ItemsViewModel(api: StubAPI(result: .success([Item(id: "1", title: "First"), Item(id: "2", title: "Second")])))
        await vm.load()
        #expect(vm.state == .loaded([Item(id: "1", title: "First"), Item(id: "2", title: "Second")]))
    }

    @Test func loadFails() async {
        struct Boom: Error {}
        let vm = ItemsViewModel(api: StubAPI(result: .failure(Boom())))
        await vm.load()
        #expect(vm.state == .error)
    }

    @Test func repeatedAppearanceKeepsStateUntilExplicitReload() async {
        let api = CountingAPI()
        let vm = ItemsViewModel(api: api)
        await vm.loadIfNeeded()
        await vm.loadIfNeeded()
        #expect(await api.calls == 1)
        #expect(vm.state == .loaded([Item(id: "1", title: "First")]))
        await vm.load()
        #expect(await api.calls == 2)
    }

    @Test func cancelledInitialLoadCanBeRetried() async {
        let api = CountingAPI(suspendFirstCall: true)
        let vm = ItemsViewModel(api: api)
        let task = Task { await vm.loadIfNeeded() }
        while await api.calls == 0 { await Task.yield() }
        task.cancel()
        await task.value
        #expect(vm.state == .loading)
        await vm.loadIfNeeded()
        #expect(await api.calls == 2)
        #expect(vm.state == .loaded([Item(id: "1", title: "First")]))
    }
}
