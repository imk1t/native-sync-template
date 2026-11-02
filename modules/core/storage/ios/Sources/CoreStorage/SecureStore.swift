import Foundation
import Security

/// 認証トークンなどの秘密情報を保存する。KeyValueStore と取り違えないよう型を分ける。
public protocol SecureStore: Sendable {
    func get(_ key: String) async throws -> String?
    /// value が nil なら削除する。
    func set(_ key: String, _ value: String?) async throws
}

public struct KeychainError: Error, Equatable, Sendable {
    public let status: OSStatus
    public init(status: OSStatus) { self.status = status }
}

/// Keychain（汎用パスワード）を使う SecureStore。端末のロック解除後だけ読め、バックアップで他の端末に移らない。
public struct KeychainSecureStore: SecureStore {
    private let service: String
    private let items: any KeychainItems

    public init(service: String) {
        self.init(service: service, items: SystemKeychain())
    }

    init(service: String, items: any KeychainItems) {
        self.service = service
        self.items = items
    }

    private func query(_ key: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
    }

    public func get(_ key: String) async throws -> String? {
        var query = query(key)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        let (status, result) = items.copy(query)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess, let data = result as? Data else { throw KeychainError(status: status) }
        return String(decoding: data, as: UTF8.self)
    }

    public func set(_ key: String, _ value: String?) async throws {
        let deleteStatus = items.delete(query(key))
        guard deleteStatus == errSecSuccess || deleteStatus == errSecItemNotFound else {
            throw KeychainError(status: deleteStatus)
        }
        guard let value else { return }
        var attributes = query(key)
        attributes[kSecValueData as String] = Data(value.utf8)
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        let status = items.add(attributes)
        guard status == errSecSuccess else { throw KeychainError(status: status) }
    }
}

/// SecItem API の差し替え口。ホストアプリの無いパッケージのテストでは Keychain の entitlement が無く
/// （errSecMissingEntitlement）、本物の Keychain を使えないため、テストではここを差し替える。
protocol KeychainItems: Sendable {
    func copy(_ query: [String: Any]) -> (OSStatus, AnyObject?)
    func add(_ attributes: [String: Any]) -> OSStatus
    func delete(_ query: [String: Any]) -> OSStatus
}

struct SystemKeychain: KeychainItems {
    func copy(_ query: [String: Any]) -> (OSStatus, AnyObject?) {
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        return (status, result)
    }

    func add(_ attributes: [String: Any]) -> OSStatus {
        SecItemAdd(attributes as CFDictionary, nil)
    }

    func delete(_ query: [String: Any]) -> OSStatus {
        SecItemDelete(query as CFDictionary)
    }
}

/// テストやプレビュー用。
public actor InMemorySecureStore: SecureStore {
    private var values: [String: String]

    public init(_ values: [String: String] = [:]) { self.values = values }

    public func get(_ key: String) async throws -> String? { values[key] }

    public func set(_ key: String, _ value: String?) async throws { values[key] = value }
}
