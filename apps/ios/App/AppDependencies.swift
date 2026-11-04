import CoreNetwork
import CoreStorage
import FeatureGitHub
import Foundation

@MainActor
final class AppDependencies {
    let github: GitHubDependencies
    let settings: DisplaySettings

    init() {
        let store = UserDefaultsStore()
        settings = DisplaySettings(store: store)
        let transport = URLSessionTransport(baseURL: URL(string: "https://api.github.com")!)
        let api = DefaultGitHubAPI(transport: GitHubTransport(underlying: transport))
        github = GitHubDependencies(
            repository: GitHubRepositoryImpl(api: api), bookmarks: BookmarkRepository(store: store),
            settings: settings)
    }
}
