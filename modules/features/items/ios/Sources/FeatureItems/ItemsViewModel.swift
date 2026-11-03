import CoreNavigation
import CoreNetwork
import Foundation
import Observation

public enum ItemsState: Equatable {
    case loading
    case loaded([Item])
    case error
}

@MainActor
@Observable
public final class ItemsViewModel {
    public private(set) var state: ItemsState = .loading
    private let api: ItemsAPI
    private let navigator: Navigator
    private var initialLoadStarted = false

    public init(api: ItemsAPI, navigator: Navigator) {
        self.api = api
        self.navigator = navigator
    }

    /// 詳細へ遷移する
    public func select(_ item: Item) {
        navigator.push(.itemDetail(id: item.id))
    }

    public func loadIfNeeded() async {
        guard !initialLoadStarted else { return }
        initialLoadStarted = true
        await load()
    }

    public func load() async {
        state = .loading
        do {
            let items = try await api.listItems()
            try Task.checkCancellation()
            state = .loaded(items)
        } catch {
            if error is CancellationError || (error as? URLError)?.code == .cancelled || Task.isCancelled {
                initialLoadStarted = false
            } else {
                state = .error
            }
        }
    }
}
