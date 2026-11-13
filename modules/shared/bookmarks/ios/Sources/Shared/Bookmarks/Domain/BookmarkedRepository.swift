/// 保存時点のリポジトリ表示用スナップショット。GitHub API のモデルには依存しない。
public struct BookmarkedRepository: Codable, Equatable, Identifiable, Sendable {
    public let id: Int64
    public let name: String
    public let fullName: String
    public let owner: String
    public let avatarURL: String?
    public let description: String?
    public let language: String?
    public let stars: Int
    public let forks: Int
    public let topics: [String]
    public let htmlURL: String

    public init(
        id: Int64, name: String, fullName: String, owner: String, avatarURL: String? = nil,
        description: String? = nil, language: String? = nil, stars: Int = 0, forks: Int = 0,
        topics: [String] = [], htmlURL: String = ""
    ) {
        self.id = id
        self.name = name
        self.fullName = fullName
        self.owner = owner
        self.avatarURL = avatarURL
        self.description = description
        self.language = language
        self.stars = stars
        self.forks = forks
        self.topics = topics
        self.htmlURL = htmlURL
    }
}
