/// リポジトリ行の表示値。API のモデルや保存処理には依存しない。
public struct RepositoryRowData {
    public let fullName: String
    public let owner: String
    public let avatarURL: String?
    public let description: String?
    public let language: String?
    public let stars: Int

    public init(
        fullName: String, owner: String, avatarURL: String? = nil,
        description: String? = nil, language: String? = nil, stars: Int = 0
    ) {
        self.fullName = fullName
        self.owner = owner
        self.avatarURL = avatarURL
        self.description = description
        self.language = language
        self.stars = stars
    }
}
