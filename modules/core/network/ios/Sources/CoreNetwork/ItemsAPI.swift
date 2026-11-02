public struct Item: Equatable, Sendable {
    public let id: String
    public let title: String
    public init(id: String, title: String) { self.id = id; self.title = title }
}

public protocol ItemsAPI: Sendable {
    func listItems() async throws -> [Item]
}

/// 生成された APIClient を包み、DTO をドメインモデルに変換する。
public struct DefaultItemsAPI: ItemsAPI {
    private let client: APIClient

    public init(transport: any HTTPTransport) {
        client = APIClient(transport: transport)
    }

    public func listItems() async throws -> [Item] {
        try await client.listItems().map { Item(id: $0.id, title: $0.title) }
    }
}
