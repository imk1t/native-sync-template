public struct GitHubIssue: Equatable, Identifiable, Sendable {
    public let id: Int64
    public let number: Int
    public let title: String
    public let state: String
    public let body: String?
    public let author: String?
    public let updatedAt: String

    public init(
        id: Int64, number: Int, title: String, state: String, body: String? = nil,
        author: String? = nil, updatedAt: String = ""
    ) {
        self.id = id
        self.number = number
        self.title = title
        self.state = state
        self.body = body
        self.author = author
        self.updatedAt = updatedAt
    }
}
