import CoreStorage
import Foundation
import Observation

/// 書き込みを直列化し、成功した状態だけを全画面に公開する。
@MainActor
@Observable
public final class BookmarkRepository {
    public private(set) var items: [Bookmark] = []
    public private(set) var isLoaded = false
    private let store: any KeyValueStore
    private var loading: Task<[Bookmark], Error>?
    private var pending: Task<Void, Error>?
    static let key = "github.bookmarks.v1"

    public init(store: any KeyValueStore) { self.store = store }

    public func load() async throws {
        guard !isLoaded else { return }
        let task: Task<[Bookmark], Error>
        if let loading {
            task = loading
        } else {
            let store = store
            task = Task {
                guard let text = try await store.get(Self.key) else { return [] }
                let document = try JSONDecoder().decode(BookmarkDocument.self, from: Data(text.utf8))
                guard document.version == 1 else { throw BookmarkError.unsupportedVersion }
                return document.items
            }
            loading = task
        }
        do {
            let loaded = try await task.value
            if !isLoaded {
                items = loaded
                isLoaded = true
            }
            loading = nil
        } catch {
            loading = nil
            throw error
        }
    }

    public func contains(_ id: Int64) -> Bool { items.contains { $0.id == id } }

    public func toggle(_ repository: BookmarkedRepository) async throws {
        try await mutate { items in
            if items.contains(where: { $0.id == repository.id }) { return items.filter { $0.id != repository.id } }
            guard items.count < 100 else { throw BookmarkError.limit }
            return [Bookmark(repository: repository)] + items
        }
    }

    public func remove(_ id: Int64) async throws { try await mutate { $0.filter { $0.id != id } } }

    public func saveNote(id: Int64, note: String) async throws {
        guard BookmarkNoteRules.isValid(note) else { throw BookmarkError.noteTooLong }
        try await mutate { items in
            var items = items
            guard let index = items.firstIndex(where: { $0.id == id }) else { throw BookmarkError.notFound }
            items[index].note = note
            return items
        }
    }

    private func mutate(_ change: @escaping @MainActor ([Bookmark]) throws -> [Bookmark]) async throws {
        let previous = pending
        let task = Task { @MainActor in
            _ = try? await previous?.value
            try await self.load()
            let updated = try change(self.items)
            let data = try JSONEncoder().encode(BookmarkDocument(items: updated))
            guard let text = String(data: data, encoding: .utf8) else {
                throw CocoaError(.fileWriteInapplicableStringEncoding)
            }
            try await self.store.set(Self.key, text)
            self.items = updated
        }
        pending = task
        try await task.value
    }
}
