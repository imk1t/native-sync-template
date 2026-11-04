package com.example.featuresearch

import com.example.corenetwork.DefaultGitHubApi
import com.example.corenetwork.GitHubTransport
import com.example.corenetwork.HttpRequest
import com.example.corenetwork.HttpResponse
import com.example.corenetwork.HttpTransport
import com.example.corenetwork.RepositorySummary
import com.example.corestorage.DisplaySettings
import com.example.corestorage.InMemoryKeyValueStore
import com.example.sharedbookmarks.BookmarkRepository

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
