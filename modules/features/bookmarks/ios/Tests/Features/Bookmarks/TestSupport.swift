import CoreStorage
import Foundation
import SharedBookmarks

actor FailingStore: KeyValueStore {
    var values: [String: String] = [:]
    var fails = false
    func failWrites() { fails = true }
    func get(_ key: String) -> String? { values[key] }
    func set(_ key: String, _ value: String?) throws {
        if fails { throw CocoaError(.fileWriteNoPermission) }
        values[key] = value
    }
}

func sample(_ id: Int64 = 5_000_000_001) -> BookmarkedRepository {
    BookmarkedRepository(id: id, name: "repo", fullName: "owner/repo", owner: "owner")
}
