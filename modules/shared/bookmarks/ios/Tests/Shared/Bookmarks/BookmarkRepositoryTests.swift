import CoreStorage
import Foundation
import Testing

@testable import SharedBookmarks

@MainActor
struct BookmarkRepositoryTests {
    @Test func readsExistingVersionOneDocument() async throws {
        let document = """
            {"version":1,"items":[{"repository":{"id":5000000001,"name":"repo","fullName":"owner/repo",
            "owner":"owner","stars":2,"forks":0,"topics":[],"htmlURL":"https://github.com/owner/repo"},
            "note":"移動前のメモ","savedAt":1730000000}]}
            """
        let store = InMemoryKeyValueStore(["github.bookmarks.v1": document])
        let bookmarks = BookmarkRepository(store: store)
        try await bookmarks.load()
        #expect(bookmarks.items.first?.id == 5_000_000_001)
        #expect(bookmarks.items.first?.note == "移動前のメモ")
        #expect(bookmarks.items.first?.savedAt == 1_730_000_000)
        #expect(try await store.get("github.bookmarks.v1") == document)
    }

    // @scenario github-bookmarks/save-restore
    @Test func saveAndRestore() async throws {
        let suite = "GitHubBookmarksTests.\(UUID().uuidString)"
        defer { UserDefaults().removePersistentDomain(forName: suite) }
        let storage = UserDefaultsStore(suiteName: suite)
        let bookmarks = BookmarkRepository(store: storage)
        try await bookmarks.toggle(sample())
        try await bookmarks.saveNote(id: sample().id, note: "メモ")
        let restored = BookmarkRepository(store: UserDefaultsStore(suiteName: suite))
        try await restored.load()
        #expect(restored.items.first?.note == "メモ")
        #expect(restored.contains(sample().id))
        try await restored.toggle(sample())
        #expect(restored.items.isEmpty)
    }

    // @scenario github-bookmarks/limits
    @Test func enforcesLimitsAndCountsCodePoints() async throws {
        let bookmarks = BookmarkRepository(store: InMemoryKeyValueStore())
        for id in 1...100 { try await bookmarks.toggle(sample(Int64(id))) }
        await #expect(throws: BookmarkError.self) { try await bookmarks.toggle(sample(101)) }
        #expect(bookmarks.items.count == 100)
        let note = String(repeating: "😀", count: 1_000)
        try await bookmarks.saveNote(id: 1, note: note)
        await #expect(throws: BookmarkError.self) { try await bookmarks.saveNote(id: 1, note: note + "a") }
        #expect(bookmarks.items.first { $0.id == 1 }?.note == note)
    }

    // @scenario github-bookmarks/write-fails
    @Test func failedWritesDoNotPublishChanges() async throws {
        let storage = FailingStore()
        let bookmarks = BookmarkRepository(store: storage)
        try await bookmarks.toggle(sample())
        await storage.failWrites()
        await #expect(throws: (any Error).self) { try await bookmarks.remove(sample().id) }
        #expect(bookmarks.contains(sample().id))
    }

    // @scenario github-bookmarks/unknown-version
    @Test func invalidStorageIsNotOverwritten() async throws {
        for value in ["invalid json", #"{"version":2,"items":[]}"#, #"{"items":[]}"#] {
            let store = InMemoryKeyValueStore([BookmarkRepository.key: value])
            let bookmarks = BookmarkRepository(store: store)
            await #expect(throws: (any Error).self) { try await bookmarks.toggle(sample()) }
            #expect(try await store.get(BookmarkRepository.key) == value)
        }
    }

    // @scenario github-bookmarks/concurrent-writes
    @Test func concurrentMutationsAreSerialized() async throws {
        let bookmarks = BookmarkRepository(store: InMemoryKeyValueStore())
        async let first: Void = bookmarks.toggle(sample(1))
        async let second: Void = bookmarks.toggle(sample(2))
        _ = try await (first, second)
        #expect(Set(bookmarks.items.map(\.id)) == [1, 2])
    }

}
