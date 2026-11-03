import Foundation

/// プレビュー・手動テスト用。同じ生成クライアントのデコードと変換を通す。
public struct DemoGitHubTransport: HTTPTransport {
    public init() {}
    public func send(_ request: HTTPRequest) async throws -> HTTPResponse {
        guard let body = DemoResponses.bodies[request.path] else { return HTTPResponse(status: 404) }
        return HTTPResponse(status: 200, body: Data(body.utf8))
    }
}
