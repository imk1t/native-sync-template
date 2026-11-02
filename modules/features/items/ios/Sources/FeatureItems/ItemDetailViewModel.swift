import CoreNetwork
import Foundation
import Observation

public enum ItemDetailState: Equatable {
    case loading
    case loaded(Item)
    case error
}

@MainActor
@Observable
public final class ItemDetailViewModel {
    public private(set) var state: ItemDetailState = .loading
    private let id: String
    private let api: ItemsAPI
    private var initialLoadStarted = false

    public init(id: String, api: ItemsAPI) {
        self.id = id
        self.api = api
    }

    public func loadIfNeeded() async {
        guard !initialLoadStarted else { return }
        initialLoadStarted = true
        await load()
    }

    public func load() async {
        state = .loading
        do {
            state = .loaded(try await api.getItem(id: id))
        } catch is CancellationError {
            initialLoadStarted = false
        } catch {
            state = .error
        }
    }
}
