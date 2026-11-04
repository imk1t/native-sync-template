struct BookmarkDocument: Codable {
    var version: Int = 1
    var items: [Bookmark]
}
