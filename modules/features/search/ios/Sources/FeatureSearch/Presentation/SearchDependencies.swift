import CoreNetwork
import CoreStorage
import SharedBookmarks

@MainActor
public final class SearchDependencies {
    public let repository: any GitHubRepository
    public let bookmarks: BookmarkRepository
    public let settings: DisplaySettings
    public let images: ImageLoader

    public init(
        repository: any GitHubRepository, bookmarks: BookmarkRepository,
        settings: DisplaySettings, images: ImageLoader = ImageLoader()
    ) {
        self.repository = repository
        self.bookmarks = bookmarks
        self.settings = settings
        self.images = images
    }

    public func loadLocalData() async throws {
        try await bookmarks.load()
        try await settings.load()
    }
}
