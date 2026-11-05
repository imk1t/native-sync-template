import CoreExtensions

public enum BookmarkNoteRules {
    public static let maxCodePoints = 1_000

    public static func characterCount(_ note: String) -> Int { note.codePointCount }

    public static func isValid(_ note: String) -> Bool { characterCount(note) <= maxCodePoints }
}
