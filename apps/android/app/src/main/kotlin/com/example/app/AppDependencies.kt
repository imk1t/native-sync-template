package com.example.app

import android.content.Context
import com.example.core.network.ImageLoader
import com.example.core.network.UrlConnectionTransport
import com.example.core.storage.DataStoreKeyValueStore
import com.example.core.storage.DisplaySettings
import com.example.features.bookmarks.BookmarksDependencies
import com.example.features.search.GitHubRepositoryImpl
import com.example.features.search.SearchDependencies
import com.example.shared.bookmarks.BookmarkRepository
import com.example.shared.githubrest.DefaultGitHubApi
import com.example.shared.githubrest.GitHubTransport

/** アプリ全体で共有する保存状態・設定・画像キャッシュを組み立てる。 */
internal class AppDependencies(context: Context) {
    private val store = DataStoreKeyValueStore.persistent(context, "github-explorer")
    val settings = DisplaySettings(store)
    private val saved = BookmarkRepository(store)
    private val images = ImageLoader()
    val search = SearchDependencies(
        GitHubRepositoryImpl(DefaultGitHubApi(GitHubTransport(UrlConnectionTransport("https://api.github.com")))),
        saved,
        settings,
        images
    )
    val bookmarks = BookmarksDependencies(saved, settings, images)
}
