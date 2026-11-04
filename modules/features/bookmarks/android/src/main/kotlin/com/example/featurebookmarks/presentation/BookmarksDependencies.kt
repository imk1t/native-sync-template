package com.example.featurebookmarks

import com.example.corenetwork.ImageLoader
import com.example.corestorage.DisplaySettings
import com.example.sharedbookmarks.BookmarkRepository

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
