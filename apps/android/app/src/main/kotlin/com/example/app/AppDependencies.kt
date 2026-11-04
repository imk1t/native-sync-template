package com.example.app

import android.content.Context
import com.example.corenetwork.DefaultGitHubApi
import com.example.corenetwork.GitHubTransport
import com.example.corenetwork.UrlConnectionTransport
import com.example.corestorage.DataStoreKeyValueStore
import com.example.corestorage.DisplaySettings
import com.example.featuregithub.BookmarkRepository
import com.example.featuregithub.GitHubDependencies
import com.example.featuregithub.GitHubRepositoryImpl

/** アプリ全体で共有する依存。設定は GitHub と設定画面に同じインスタンスを渡す。 */
internal class AppDependencies(context: Context) {
    private val store = DataStoreKeyValueStore.persistent(context, "github-explorer")
    val settings = DisplaySettings(store)
    val github = GitHubDependencies(
        GitHubRepositoryImpl(DefaultGitHubApi(GitHubTransport(UrlConnectionTransport("https://api.github.com")))),
        BookmarkRepository(store),
        settings
    )
}
