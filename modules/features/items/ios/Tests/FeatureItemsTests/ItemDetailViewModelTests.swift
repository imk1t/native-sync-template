import CoreNetwork
import Foundation
import Testing
@testable import FeatureItems

private struct DetailStubAPI: ItemsAPI {
    var result: Result<Item, Error>
    func listItems() async throws -> [Item] { [] }
    func getItem(id: String) async throws -> Item { try result.get() }
}

// shared/scenarios/item-detail.yaml の各 step を実装する
@MainActor
struct ItemDetailViewModelTests {
    // @scenario item-detail/load-succeeds
    @Test func loadSucceeds() async {
        let api = DetailStubAPI(result: .success(Item(id: "1", title: "First")))
        let viewModel = ItemDetailViewModel(id: "1", api: api)
        await viewModel.load()
        #expect(viewModel.state == .loaded(Item(id: "1", title: "First")))
    }

    // @scenario item-detail/load-fails
    @Test func loadFails() async {
        struct Boom: Error {}
        let viewModel = ItemDetailViewModel(id: "1", api: DetailStubAPI(result: .failure(Boom())))
        await viewModel.load()
        #expect(viewModel.state == .error)
    }
}
