package com.example.featuresearch

import com.example.corenetwork.GitHubApi
import com.example.corenetwork.GitHubIssue
import com.example.corenetwork.Page
import com.example.corenetwork.RepositorySummary
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class GitHubRepositoryImpl(private val api: GitHubApi, private val now: () -> Long = System::currentTimeMillis) :
    GitHubRepository {
    private val searches = mutableMapOf<String, CacheEntry<Page<RepositorySummary>>>()
    private val repositories = mutableMapOf<String, CacheEntry<RepositorySummary>>()
    private val issuePages = mutableMapOf<String, CacheEntry<Page<GitHubIssue>>>()
    private val issues = mutableMapOf<String, CacheEntry<GitHubIssue>>()

    override suspend fun search(query: String, sort: String?, page: Int, refresh: Boolean) =
        cached(searches, "$query\u0000${sort.orEmpty()}\u0000$page", refresh) {
            api.searchRepositories(query, sort, page)
        }

    override suspend fun repository(owner: String, repo: String, refresh: Boolean) =
        cached(repositories, "$owner/$repo", refresh) { api.repository(owner, repo) }

    override suspend fun issues(owner: String, repo: String, state: String, page: Int, refresh: Boolean) =
        cached(issuePages, "$owner/$repo/$state/$page", refresh) { api.issues(owner, repo, state, page) }

    override suspend fun issue(owner: String, repo: String, number: Long, refresh: Boolean) =
        cached(issues, "$owner/$repo/$number", refresh) { api.issue(owner, repo, number) }

    private suspend fun <T> cached(
        cache: MutableMap<String, CacheEntry<T>>,
        key: String,
        refresh: Boolean,
        fetch: suspend () -> T
    ): T {
        if (!refresh) synchronized(cache) { cache[key] }?.takeIf { it.expires > now() }?.let { return it.value }
        val value = fetch()
        currentCoroutineContext().ensureActive()
        synchronized(cache) {
            if (cache.size >= 100) cache.clear()
            cache[key] = CacheEntry(value, now() + 300_000)
        }
        return value
    }
}
