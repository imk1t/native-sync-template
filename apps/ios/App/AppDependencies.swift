import CoreNetwork
import CoreStorage
import FeatureBookmarks
import FeatureSearch
import Foundation
import SharedBookmarks
import SharedGitHubREST

@MainActor
final class AppDependencies {
    let search: SearchDependencies
    let bookmarks: BookmarksDependencies
    let settings: DisplaySettings

    init() {
        let store = UserDefaultsStore()
        settings = DisplaySettings(store: store)
        let saved = BookmarkRepository(store: store)
        let images = ImageLoader()
        let transport = URLSessionTransport(baseURL: URL(string: "https://api.github.com")!)
        let api = DefaultGitHubAPI(transport: GitHubTransport(underlying: transport))
        search = SearchDependencies(
            repository: GitHubRepositoryImpl(api: api), bookmarks: saved, settings: settings, images: images)
        bookmarks = BookmarksDependencies(bookmarks: saved, settings: settings, images: images)
    }
}
