import Foundation

struct CacheEntry<Value: Sendable>: Sendable {
    let value: Value
    let expires: Date
}
