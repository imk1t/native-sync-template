import Foundation
import Testing
import Security
@testable import CoreStorage

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

/// SecItem を辞書で模したもの。service と account の組で 1 件を持つ。
private final class FakeKeychain: KeychainItems, @unchecked Sendable {
    private let lock = NSLock()
    private var items: [String: [String: Any]] = [:]
    var failAddWith: OSStatus?

    private func id(_ query: [String: Any]) -> String {
        "\(query[kSecAttrService as String] ?? "")/\(query[kSecAttrAccount as String] ?? "")"
    }

    func attributes(service: String, account: String) -> [String: Any]? {
        lock.withLock { items["\(service)/\(account)"] }
    }

    func copy(_ query: [String: Any]) -> (OSStatus, AnyObject?) {
        lock.withLock {
            guard let item = items[id(query)] else { return (errSecItemNotFound, nil) }
            return (errSecSuccess, item[kSecValueData as String] as AnyObject)
        }
    }

    func add(_ attributes: [String: Any]) -> OSStatus {
        lock.withLock {
            if let failAddWith { return failAddWith }
            guard items[id(attributes)] == nil else { return errSecDuplicateItem }
            items[id(attributes)] = attributes
            return errSecSuccess
        }
    }

    func delete(_ query: [String: Any]) -> OSStatus {
        lock.withLock { items.removeValue(forKey: id(query)) == nil ? errSecItemNotFound : errSecSuccess }
    }
}

struct KeychainSecureStoreTests {
    @Test func setOverwriteAndRemove() async throws {
        let keychain = FakeKeychain()
        let store = KeychainSecureStore(service: "app", items: keychain)
        #expect(try await store.get("token") == nil)
        try await store.set("token", "a")
        try await store.set("token", "b")
        #expect(try await store.get("token") == "b")
        try await store.set("token", nil)
        #expect(try await store.get("token") == nil)
    }

    @Test func savesAsThisDeviceOnlyGenericPassword() async throws {
        let keychain = FakeKeychain()
        try await KeychainSecureStore(service: "app", items: keychain).set("token", "secret")
        let item = try #require(keychain.attributes(service: "app", account: "token"))
        #expect(item[kSecClass as String] as? String == kSecClassGenericPassword as String)
        #expect(item[kSecAttrAccessible as String] as? String == kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly as String)
    }

    @Test func keychainFailureIsThrown() async {
        let keychain = FakeKeychain()
        keychain.failAddWith = errSecInteractionNotAllowed
        await #expect(throws: KeychainError(status: errSecInteractionNotAllowed)) {
            try await KeychainSecureStore(service: "app", items: keychain).set("token", "secret")
        }
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
