import Foundation

struct APIResponse<Value: Sendable>: Sendable {
    let value: Value
    let headers: [String: String]
}
