import CoreNetwork
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

    public init(api: ItemsAPI) { self.api = api }

    public func load() async {
        state = .loading
        do {
            state = .loaded(try await api.listItems())
        } catch {
            state = .error
        }
    }
}
