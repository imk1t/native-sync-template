import SharedBookmarks
import Testing

struct BookmarkNoteRulesTests {
    @Test func acceptsEmptyAndCountsUnicodeAtTheLimit() {
        #expect(BookmarkNoteRules.isValid(""))
        let emoji = String(repeating: "😀", count: 1_000)
        let combining = String(repeating: "e\u{0301}", count: 500)
        for note in [emoji, combining] {
            #expect(BookmarkNoteRules.characterCount(note) == 1_000)
            #expect(BookmarkNoteRules.isValid(note))
            #expect(!BookmarkNoteRules.isValid(note + "a"))
        }
    }
}
