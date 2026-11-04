package com.example.features.bookmarks

import com.example.core.network.ImageLoader
import com.example.core.storage.DisplaySettings
import com.example.shared.bookmarks.BookmarkRepository

class BookmarksDependencies(
    val bookmarks: BookmarkRepository,
    val settings: DisplaySettings,
    val images: ImageLoader = ImageLoader()
) {
    suspend fun loadLocalData() {
        bookmarks.load()
        settings.load()
    }
}
