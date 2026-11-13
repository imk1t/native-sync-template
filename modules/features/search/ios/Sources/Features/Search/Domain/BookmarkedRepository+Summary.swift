import SharedBookmarks
import SharedGitHubREST

extension BookmarkedRepository {
    /// 検索結果のリポジトリを、保存用のスナップショットに変換する。
    init(_ repository: RepositorySummary) {
        self.init(
            id: repository.id, name: repository.name, fullName: repository.fullName, owner: repository.owner,
            avatarURL: repository.avatarURL, description: repository.description, language: repository.language,
            stars: repository.stars, forks: repository.forks, topics: repository.topics, htmlURL: repository.htmlURL)
    }
}
