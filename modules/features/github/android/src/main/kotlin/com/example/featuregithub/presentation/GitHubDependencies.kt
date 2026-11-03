package com.example.featuregithub

import com.example.corenetwork.ImageLoader
import com.example.corestorage.DisplaySettings

class GitHubDependencies(
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
