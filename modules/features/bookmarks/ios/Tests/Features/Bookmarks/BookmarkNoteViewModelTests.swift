import CoreNavigation
import SharedBookmarks
import Testing

@testable import FeatureBookmarks

@MainActor
struct BookmarkNoteViewModelTests {
    @Test func noteLengthMatchesRepositoryValidation() async throws {
        let bookmarks = BookmarkRepository(store: FailingStore())
        try await bookmarks.toggle(sample())
        let model = BookmarkNoteViewModel(
            repositoryId: String(sample().id), bookmarks: bookmarks, navigator: Navigator())
        #expect(!model.canSave)
        await model.load()
        #expect(model.canSave)
        model.note = String(repeating: "e\u{0301}", count: 500)
        #expect(model.characterCount == 1_000 && model.canSave)
        await model.save()
        #expect(bookmarks.items.first?.note == model.note)
        model.note += "😀"
        #expect(model.characterCount == 1_001 && !model.canSave)
        await model.save()
        #expect(bookmarks.items.first?.note != model.note)
        await #expect(throws: BookmarkError.self) {
            try await bookmarks.saveNote(id: sample().id, note: model.note)
        }
    }

    // @scenario bookmark-note/note-save-cancel
    // @scenario bookmark-note/note-fails
    @Test func noteSaveCancelAndFailure() async throws {
        let store = FailingStore()
        let bookmarks = BookmarkRepository(store: store)
        try await bookmarks.toggle(sample())
        let navigator = Navigator()
        navigator.present(.bookmarkNote(repositoryId: String(sample().id)))
        let model = BookmarkNoteViewModel(repositoryId: String(sample().id), bookmarks: bookmarks, navigator: navigator)
        await model.load()
        model.note = "保存"
        await model.save()
        #expect(bookmarks.items.first?.note == "保存")
        #expect(navigator.state.modals.isEmpty)
        navigator.present(.bookmarkNote(repositoryId: String(sample().id)))
        let editing = BookmarkNoteViewModel(
            repositoryId: String(sample().id), bookmarks: bookmarks, navigator: navigator)
        await editing.load()
        editing.note = "下書き"
        await store.failWrites()
        await editing.save()
        #expect(editing.error != nil && editing.note == "下書き")
        #expect(navigator.state.modals.count == 1)
        #expect(bookmarks.items.first?.note == "保存")
        editing.cancel()
        #expect(navigator.state.modals.isEmpty)
    }
}
