public struct Item: Equatable, Sendable {
    public let id: String
    public let title: String
    public init(id: String, title: String) { self.id = id; self.title = title }
}

public protocol ItemsAPI: Sendable {
    func listItems() async throws -> [Item]
}
