import Foundation
import Observation

@MainActor
@Observable
public final class DisplaySettings {
    public private(set) var showDescriptions = true
    public private(set) var isLoaded = false
    private let store: any KeyValueStore
    private var loading: Task<Bool, Error>?
    private var pending: Task<Void, Error>?
    public static let key = "display.showDescriptions"

    public init(store: any KeyValueStore) { self.store = store }

    public func load() async throws {
        guard !isLoaded else { return }
        let task: Task<Bool, Error>
        if let loading {
            task = loading
        } else {
            let store = store
            task = Task { try await store.get(Self.key) != "false" }
            loading = task
        }
        do {
            let value = try await task.value
            if !isLoaded {
                showDescriptions = value
                isLoaded = true
            }
            loading = nil
        } catch {
            loading = nil
            throw error
        }
    }

    public func setShowDescriptions(_ value: Bool) async throws {
        let previous = pending
        let task = Task { @MainActor in
            _ = try? await previous?.value
            try await self.load()
            try await self.store.set(Self.key, value ? "true" : "false")
            self.showDescriptions = value
        }
        pending = task
        try await task.value
    }
}
