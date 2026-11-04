import Foundation

/// GitHub 固有のヘッダーと待機時間を管理する。時計を差し替えて制限のテストができる。
public actor GitHubTransport: HTTPTransport {
    private let underlying: any HTTPTransport
    private let now: @Sendable () -> Date
    private var blockedUntil: [String: Date] = [:]

    public init(underlying: any HTTPTransport, now: @escaping @Sendable () -> Date = { Date() }) {
        self.underlying = underlying
        self.now = now
    }

    public func send(_ request: HTTPRequest) async throws -> HTTPResponse {
        let resource = request.path.hasPrefix("/search/") ? "search" : "core"
        let deadline = max(blockedUntil[resource] ?? .distantPast, blockedUntil["all"] ?? .distantPast)
        if deadline > now() { throw GitHubError.rateLimited(until: deadline) }
        var request = request
        request.headers["Accept"] = "application/vnd.github+json"
        request.headers["X-GitHub-Api-Version"] = "2026-03-10"
        request.headers["User-Agent"] = "native-sync-template"
        let response = try await underlying.send(request)
        try Task.checkCancellation()
        if response.status == 403 || response.status == 429 {
            let body = String(data: response.body, encoding: .utf8)?.lowercased() ?? ""
            let primary = response.headers["x-ratelimit-remaining"] == "0"
            if response.status == 429 || primary || response.headers["retry-after"] != nil
                || body.contains("rate limit") {
                let retry = response.headers["retry-after"].flatMap(Double.init).map { now().addingTimeInterval($0) }
                let reset =
                    primary
                    ? response.headers["x-ratelimit-reset"].flatMap(Double.init)
                        .map { Date(timeIntervalSince1970: $0) } : nil
                let until = max(retry ?? reset ?? now().addingTimeInterval(60), now().addingTimeInterval(1))
                blockedUntil[primary ? resource : "all"] = until
                throw GitHubError.rateLimited(until: until)
            }
        }
        switch response.status {
        case 200..<300: return response
        case 404: throw GitHubError.notFound
        case 422: throw GitHubError.invalidQuery
        case 403: throw GitHubError.forbidden
        default: throw GitHubError.server(response.status)
        }
    }
}
