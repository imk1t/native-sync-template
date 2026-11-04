import CoreLocalization
import SharedBookmarks

func bookmarkMessage(for error: Error, fallback: Message) -> Message {
    switch error as? BookmarkError {
    case .limit: .errorBookmarkLimit
    case .noteTooLong: .errorNoteTooLong
    case .notFound: .errorBookmarkNotFound
    case .unsupportedVersion: .errorStorageVersion
    case nil: fallback
    }
}
