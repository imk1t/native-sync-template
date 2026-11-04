package com.example.featuresearch

import com.example.corenetwork.ImageLoader
import com.example.corestorage.DisplaySettings
import com.example.sharedbookmarks.BookmarkRepository

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
