import Foundation
import Testing
import CoreStorage

struct UserDefaultsStoreTests {
    @Test func setGetAndRemove() async throws {
        let suite = "CoreStorageTests.\(UUID().uuidString)"
        defer { UserDefaults().removePersistentDomain(forName: suite) }
        let store = UserDefaultsStore(suiteName: suite)
        #expect(try await store.get("theme") == nil)
        try await store.set("theme", "dark")
        #expect(try await store.get("theme") == "dark")
        try await store.set("theme", nil)
        #expect(try await store.get("theme") == nil)
    }
}

struct KeychainSecureStoreTests {
    @Test func setOverwriteAndRemove() async throws {
        let store = KeychainSecureStore(service: "CoreStorageTests.\(UUID().uuidString)")
        #expect(try await store.get("token") == nil)
        try await store.set("token", "a")
        try await store.set("token", "b")
        #expect(try await store.get("token") == "b")
        try await store.set("token", nil)
        #expect(try await store.get("token") == nil)
    }
}

struct InMemoryStoreTests {
    @Test func keyValueAndSecureStoresBehaveTheSame() async throws {
        let keyValue = InMemoryKeyValueStore(["a": "1"])
        let secure = InMemorySecureStore(["a": "1"])
        try await keyValue.set("a", nil)
        try await secure.set("a", nil)
        #expect(try await keyValue.get("a") == nil)
        #expect(try await secure.get("a") == nil)
    }
}
