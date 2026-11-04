import CoreNavigation
import Observation
import SharedBookmarks

@MainActor
@Observable
final class BookmarkNoteViewModel {
    var note = ""
    private(set) var isLoaded = false
    private(set) var isSaving = false
    private(set) var error: String?
    private let id: Int64?
    private let bookmarks: BookmarkRepository
    private let navigator: Navigator
    private let entryID: String?
    var characterCount: Int { note.unicodeScalars.count }
    var canSave: Bool { isLoaded && !isSaving && characterCount <= 1_000 }

    init(repositoryId: String, bookmarks: BookmarkRepository, navigator: Navigator) {
        id = Int64(repositoryId)
        self.bookmarks = bookmarks
        self.navigator = navigator
        entryID = navigator.state.modals.last?.stack.first?.id
    }
    func load() async {
        guard !isLoaded else { return }
        do {
            try await bookmarks.load()
            guard let bookmark = bookmarks.items.first(where: { $0.id == id }) else { throw BookmarkError.notFound }
            note = bookmark.note
            isLoaded = true
            error = nil
        } catch { self.error = error.localizedDescription }
    }
    func save() async {
        guard canSave, let id else { return }
        isSaving = true
        defer { isSaving = false }
        do {
            try await bookmarks.saveNote(id: id, note: note)
            close()
        } catch { self.error = error.localizedDescription }
    }
    func cancel() { if !isSaving { close() } }
    private func close() {
        if entryID != nil && navigator.state.modals.last?.stack.first?.id == entryID { navigator.dismiss() }
    }
}
