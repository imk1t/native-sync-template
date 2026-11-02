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

    public init(service: String) {
        self.service = service
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
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess, let data = result as? Data else { throw KeychainError(status: status) }
        return String(decoding: data, as: UTF8.self)
    }

    public func set(_ key: String, _ value: String?) async throws {
        let deleteStatus = SecItemDelete(query(key) as CFDictionary)
        guard deleteStatus == errSecSuccess || deleteStatus == errSecItemNotFound else {
            throw KeychainError(status: deleteStatus)
        }
        guard let value else { return }
        var attributes = query(key)
        attributes[kSecValueData as String] = Data(value.utf8)
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        let status = SecItemAdd(attributes as CFDictionary, nil)
        guard status == errSecSuccess else { throw KeychainError(status: status) }
    }
}

/// テストやプレビュー用。
public actor InMemorySecureStore: SecureStore {
    private var values: [String: String]

    public init(_ values: [String: String] = [:]) { self.values = values }

    public func get(_ key: String) async throws -> String? { values[key] }

    public func set(_ key: String, _ value: String?) async throws { values[key] = value }
}
