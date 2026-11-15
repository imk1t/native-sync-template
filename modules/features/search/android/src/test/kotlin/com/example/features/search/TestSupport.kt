package com.example.features.search

import com.example.core.network.HttpTransport
import com.example.core.storage.DisplaySettings
import com.example.core.storage.InMemoryKeyValueStore
import com.example.shared.bookmarks.BookmarkRepository
import com.example.shared.githubrest.DefaultGitHubApi
import com.example.shared.githubrest.GitHubTransport
import com.example.shared.githubrest.RepositorySummary

fun sample(id: Long = 5_000_000_001) = RepositorySummary(id, "repo", "owner/repo", "owner")
fun dependencies(transport: HttpTransport): SearchDependencies {
    val store = InMemoryKeyValueStore()
    return SearchDependencies(
        GitHubRepositoryImpl(DefaultGitHubApi(GitHubTransport(transport))),
        BookmarkRepository(store),
        DisplaySettings(store)
    )
}
