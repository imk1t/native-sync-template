package com.example.features.search

import com.example.core.network.ImageLoader
import com.example.core.storage.DisplaySettings
import com.example.shared.bookmarks.BookmarkRepository

class SearchDependencies(
    val repository: GitHubRepository,
    val bookmarks: BookmarkRepository,
    val settings: DisplaySettings,
    val images: ImageLoader = ImageLoader()
) {
    suspend fun loadLocalData() {
        bookmarks.load()
        settings.load()
    }
}
