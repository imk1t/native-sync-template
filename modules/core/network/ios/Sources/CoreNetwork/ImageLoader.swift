import Foundation

public actor ImageLoader {
    private let session: URLSession
    private var cache: [String: Data] = [:]
    private var order: [String] = []

    public init(session: URLSession = .shared) { self.session = session }

    public func load(_ url: String) async throws -> Data {
        if let data = cache[url] { return data }
        guard let target = URL(string: url), target.scheme == "https" else { throw URLError(.badURL) }
        let (data, response) = try await session.data(from: target)
        try Task.checkCancellation()
        guard let response = response as? HTTPURLResponse, response.statusCode == 200,
            data.count <= 2_000_000
        else { throw URLError(.badServerResponse) }
        cache[url] = data
        order.removeAll { $0 == url }
        order.append(url)
        while order.count > 50 { cache.removeValue(forKey: order.removeFirst()) }
        return data
    }
}
