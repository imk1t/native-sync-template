package com.example.featuregithub

import com.example.corenetwork.DefaultGitHubApi
import com.example.corenetwork.DemoGitHubTransport
import com.example.corenetwork.GitHubTransport
import com.example.corenetwork.HttpRequest
import com.example.corenetwork.HttpResponse
import com.example.corenetwork.HttpTransport
import com.example.corenetwork.RepositorySummary
import com.example.corestorage.DisplaySettings
import com.example.corestorage.InMemoryKeyValueStore
import com.example.corestorage.KeyValueStore
import java.io.IOException

class RecordingTransport : HttpTransport {
    val requests = mutableListOf<HttpRequest>()
    var failure: Int? = null
    override suspend fun send(request: HttpRequest): HttpResponse {
        requests.add(request)
        failure?.let {
            failure = null
            return HttpResponse(it, "")
        }
        return DemoGitHubTransport().send(request)
    }
}

class FailingStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()
    var fails = false
    override suspend fun get(key: String) = values[key]
    override suspend fun set(key: String, value: String?) {
        if (fails) throw IOException("write failed")
        if (value == null) values.remove(key) else values[key] = value
    }
}

fun sample(id: Long = 5_000_000_001) = RepositorySummary(id, "repo", "owner/repo", "owner")
fun dependencies(transport: HttpTransport = DemoGitHubTransport()): GitHubDependencies {
    val store = InMemoryKeyValueStore()
    return GitHubDependencies(
        GitHubRepositoryImpl(DefaultGitHubApi(GitHubTransport(transport))),
        BookmarkRepository(store),
        DisplaySettings(store)
    )
}
