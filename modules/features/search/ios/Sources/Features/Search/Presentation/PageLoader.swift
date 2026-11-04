import CoreLocalization
import CoreNetwork
import Foundation
import Observation

@MainActor
@Observable
public final class PageLoader<Value: Equatable & Identifiable & Sendable> where Value.ID: Sendable {
    public private(set) var state = PagedState<Value>()
    private var generation = 0
    private var task: Task<Page<Value>, Error>?

    public init() {}

    public func reset() {
        generation += 1
        task?.cancel()
        state = PagedState()
    }

    public func load(page: Int, fetch: @escaping @Sendable () async throws -> Page<Value>) async {
        guard !state.isLoading else { return }
        generation += 1
        let current = generation
        state.isLoading = true
        state.error = nil
        let work = Task { try await fetch() }
        task = work
        defer {
            if current == generation {
                state.isLoading = false
                task = nil
            }
        }
        do {
            let result = try await withTaskCancellationHandler {
                try await work.value
            } onCancel: {
                work.cancel()
            }
            try Task.checkCancellation()
            guard current == generation else { return }
            let combined = (page == 1 ? [] : state.items) + result.items
            var seen = Set<Value.ID>()
            state.items = combined.filter { seen.insert($0.id).inserted }
            state.hasLoaded = true
            state.nextPage = result.nextPage
            state.incomplete = result.incomplete
            state.limitReached = result.limitReached
        } catch {
            guard current == generation else { return }
            if !(error is CancellationError) && (error as? URLError)?.code != .cancelled && !Task.isCancelled {
                state.error = SearchFailure(error)
            }
        }
    }
}
