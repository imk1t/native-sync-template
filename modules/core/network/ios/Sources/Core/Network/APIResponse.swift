/// 成功したレスポンスの値と、同じ呼び出しのヘッダー（小文字名）。
public struct APIResponse<Value: Sendable>: Sendable {
    public let value: Value
    public let headers: [String: String]

    public init(value: Value, headers: [String: String]) {
        self.value = value
        self.headers = headers
    }
}
