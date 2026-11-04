import CoreNavigation
import CoreStorage
import SharedBookmarks
import Testing

@testable import FeatureBookmarks

@MainActor
struct BookmarksViewModelTests {
    @Test func sharesChangesAndOpensDetailWithoutDependingOnSearch() async throws {
        let store = InMemoryKeyValueStore()
        let repository = BookmarkRepository(store: store)
        let navigator = Navigator()
        navigator.selectTab(.saved)
        let model = BookmarksViewModel(
            dependencies: BookmarksDependencies(bookmarks: repository, settings: DisplaySettings(store: store)),
            navigator: navigator)
        await model.load()
        #expect(model.dependencies.bookmarks.items.isEmpty)

        try await repository.toggle(sample())
        try await repository.saveNote(id: sample().id, note: "共有メモ")
        let bookmark = try #require(model.dependencies.bookmarks.items.first)
        #expect(bookmark.note == "共有メモ")
        model.select(bookmark)
        #expect(navigator.state.stacks[.saved]?.last?.route == .repositoryDetail(owner: "owner", repo: "repo"))
        model.edit(bookmark)
        #expect(navigator.state.modals.last?.stack.first?.route == .bookmarkNote(repositoryId: String(bookmark.id)))

        try await repository.remove(bookmark.id)
        #expect(model.dependencies.bookmarks.items.isEmpty)
    }
}
