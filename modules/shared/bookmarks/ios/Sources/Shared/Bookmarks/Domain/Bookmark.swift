import Foundation

public struct Bookmark: Codable, Equatable, Identifiable, Sendable {
    public var id: Int64 { repository.id }
    public let repository: BookmarkedRepository
    public var note: String
    public let savedAt: Double

    public init(repository: BookmarkedRepository, note: String = "", savedAt: Double = Date().timeIntervalSince1970) {
        self.repository = repository
        self.note = note
        self.savedAt = savedAt
    }
}
