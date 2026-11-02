import Foundation

/// 設定などの普通の値を保存する。秘密情報は SecureStore に保存する。
public protocol KeyValueStore: Sendable {
    func get(_ key: String) async throws -> String?
    /// value が nil なら削除する。
    func set(_ key: String, _ value: String?) async throws
}

/// UserDefaults を使う KeyValueStore。
public struct UserDefaultsStore: KeyValueStore {
    private let suiteName: String?

    /// suiteName が nil なら standard を使う。
    public init(suiteName: String? = nil) {
        self.suiteName = suiteName
    }

    // UserDefaults は Sendable ではないため保持せず、呼び出しごとに取得する（スレッドセーフな API）。
    private var defaults: UserDefaults {
        suiteName.flatMap(UserDefaults.init(suiteName:)) ?? .standard
    }

    public func get(_ key: String) async throws -> String? {
        defaults.string(forKey: key)
    }

    public func set(_ key: String, _ value: String?) async throws {
        if let value {
            defaults.set(value, forKey: key)
        } else {
            defaults.removeObject(forKey: key)
        }
    }
}

/// テストやプレビュー用。
public actor InMemoryKeyValueStore: KeyValueStore {
    private var values: [String: String]

    public init(_ values: [String: String] = [:]) { self.values = values }

    public func get(_ key: String) async throws -> String? { values[key] }

    public func set(_ key: String, _ value: String?) async throws { values[key] = value }
}
