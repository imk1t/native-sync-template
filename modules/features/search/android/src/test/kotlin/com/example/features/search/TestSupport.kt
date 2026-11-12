package com.example.features.search

import com.example.core.network.HttpRequest
import com.example.core.network.HttpResponse
import com.example.core.network.HttpTransport
import com.example.core.storage.DisplaySettings
import com.example.core.storage.InMemoryKeyValueStore
import com.example.shared.bookmarks.BookmarkRepository
import com.example.shared.github.DefaultGitHubApi
import com.example.shared.github.GitHubTransport
import com.example.shared.github.RepositorySummary

class RecordingTransport : HttpTransport {
    val requests = mutableListOf<HttpRequest>()
    var failure: Int? = null
    override suspend fun send(request: HttpRequest): HttpResponse {
        requests.add(request)
        failure?.let {
            failure = null
            return HttpResponse(it, "")
        }
        val body = when (request.path) {
            "/search/repositories" -> TestResponses.search
            "/repos/octocat/Hello-World" -> TestResponses.repository
            "/repos/octocat/Hello-World/issues" -> "[${TestResponses.issue}]"
            "/repos/octocat/Hello-World/issues/1" -> TestResponses.issue
            else -> return HttpResponse(404)
        }
        return HttpResponse(200, body)
    }
}

fun sample(id: Long = 5_000_000_001) = RepositorySummary(id, "repo", "owner/repo", "owner")
fun dependencies(transport: HttpTransport = RecordingTransport()): SearchDependencies {
    val store = InMemoryKeyValueStore()
    return SearchDependencies(
        GitHubRepositoryImpl(DefaultGitHubApi(GitHubTransport(transport))),
        BookmarkRepository(store),
        DisplaySettings(store)
    )
}
