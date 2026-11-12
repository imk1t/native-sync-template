import Foundation

/// 生成された APIClient が組み立てるリクエスト。ベース URL は持たず、Transport が解決する。
public struct HTTPRequest: Equatable, Sendable {
    public var method: String
    public var path: String
    public var query: [URLQueryItem]
    public var body: Data?
    public var headers: [String: String]

    public init(
        method: String, path: String, query: [URLQueryItem] = [], body: Data? = nil, headers: [String: String] = [:]
    ) {
        self.method = method
        self.path = path
        self.query = query
        self.body = body
        self.headers = headers
    }
}

public struct HTTPResponse: Equatable, Sendable {
    public var status: Int
    public var body: Data
    public var headers: [String: String]

    public init(status: Int, body: Data = Data(), headers: [String: String] = [:]) {
        self.status = status
        self.body = body
        self.headers = Dictionary(headers.map { ($0.key.lowercased(), $0.value) }, uniquingKeysWith: { _, new in new })
    }
}

/// 2xx 以外のレスポンス。
public struct APIError: Error, Equatable, Sendable {
    public let status: Int
    public let headers: [String: String]
    public let body: Data
    public init(status: Int, headers: [String: String] = [:], body: Data = Data()) {
        self.status = status
        self.headers = headers
        self.body = body
    }
}

/// 通信の差し替え口。本番は URLSessionTransport、テストはスタブを渡す。
public protocol HTTPTransport: Sendable {
    func send(_ request: HTTPRequest) async throws -> HTTPResponse
}

extension HTTPTransport {
    public func call<T: Decodable & Sendable>(
        _ request: HTTPRequest, decoding type: T.Type
    ) async throws -> APIResponse<T> {
        let response = try await send(request)
        guard (200..<300).contains(response.status) else {
            throw APIError(status: response.status, headers: response.headers, body: response.body)
        }
        return APIResponse(value: try JSONDecoder().decode(T.self, from: response.body), headers: response.headers)
    }

    public func call(_ request: HTTPRequest) async throws {
        let response = try await send(request)
        guard (200..<300).contains(response.status) else {
            throw APIError(status: response.status, headers: response.headers, body: response.body)
        }
    }
}

/// パスの 1 区切りとして埋め込む値をエンコードする（"/" もエスケープする）。
public func pathSegment(_ value: some LosslessStringConvertible) -> String {
    var allowed = CharacterSet.urlPathAllowed
    allowed.remove("/")
    return String(value).addingPercentEncoding(withAllowedCharacters: allowed) ?? String(value)
}

public struct URLSessionTransport: HTTPTransport {
    private let baseURL: URL
    private let session: URLSession

    public init(baseURL: URL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session
    }

    public func send(_ request: HTTPRequest) async throws -> HTTPResponse {
        var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false)!
        components.percentEncodedPath = components.percentEncodedPath.trimmingSuffix("/") + request.path
        if !request.query.isEmpty { components.queryItems = request.query }
        var urlRequest = URLRequest(url: components.url!)
        urlRequest.httpMethod = request.method
        urlRequest.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body = request.body {
            urlRequest.httpBody = body
            urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        for (key, value) in request.headers { urlRequest.setValue(value, forHTTPHeaderField: key) }
        let (data, response) = try await session.data(for: urlRequest)
        guard let http = response as? HTTPURLResponse else { throw URLError(.badServerResponse) }
        let headers = http.allHeaderFields.reduce(into: [String: String]()) { result, entry in
            result[String(describing: entry.key)] = String(describing: entry.value)
        }
        return HTTPResponse(status: http.statusCode, body: data, headers: headers)
    }
}

extension String {
    fileprivate func trimmingSuffix(_ suffix: String) -> String {
        hasSuffix(suffix) ? String(dropLast(suffix.count)) : self
    }
}
