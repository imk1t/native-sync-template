import CoreNetwork
import Testing
@testable import FeatureItems

private struct StubAPI: ItemsAPI {
    var result: Result<[Item], Error>
    func listItems() async throws -> [Item] { try result.get() }
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
}
