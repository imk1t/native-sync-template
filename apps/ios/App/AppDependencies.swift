import CoreNetwork
import CoreStorage
import FeatureGitHub
import Foundation

@MainActor
final class AppDependencies {
    let github: GitHubDependencies

    init() {
        let demo = ProcessInfo.processInfo.arguments.contains("--demo")
        let store: any KeyValueStore = demo ? InMemoryKeyValueStore() : UserDefaultsStore()
        let transport: any HTTPTransport =
            demo ? DemoGitHubTransport() : URLSessionTransport(baseURL: URL(string: "https://api.github.com")!)
        let api = DefaultGitHubAPI(transport: GitHubTransport(underlying: transport))
        github = GitHubDependencies(
            repository: GitHubRepositoryImpl(api: api), bookmarks: BookmarkRepository(store: store),
            settings: DisplaySettings(store: store))
    }
}
