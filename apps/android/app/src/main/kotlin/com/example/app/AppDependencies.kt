package com.example.app

import android.content.Context
import com.example.corenetwork.DefaultGitHubApi
import com.example.corenetwork.GitHubTransport
import com.example.corenetwork.ImageLoader
import com.example.corenetwork.UrlConnectionTransport
import com.example.corestorage.DataStoreKeyValueStore
import com.example.corestorage.DisplaySettings
import com.example.featurebookmarks.BookmarksDependencies
import com.example.featuresearch.GitHubRepositoryImpl
import com.example.featuresearch.SearchDependencies
import com.example.sharedbookmarks.BookmarkRepository

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
