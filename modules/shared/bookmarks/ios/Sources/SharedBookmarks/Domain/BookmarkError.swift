import Foundation

public enum BookmarkError: Error {
    case limit
    case noteTooLong
    case notFound
    case unsupportedVersion
}
