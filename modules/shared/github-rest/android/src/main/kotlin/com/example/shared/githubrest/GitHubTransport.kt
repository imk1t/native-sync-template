package com.example.shared.githubrest

import com.example.core.network.HttpRequest
import com.example.core.network.HttpResponse
import com.example.core.network.HttpTransport
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GitHubTransport(private val underlying: HttpTransport, private val now: () -> Long = System::currentTimeMillis) :
    HttpTransport {
    private val mutex = Mutex()
    private val blockedUntil = mutableMapOf<String, Long>()

    override suspend fun send(request: HttpRequest): HttpResponse {
        val resource = if (request.path.startsWith("/search/")) "search" else "core"
        val deadline = mutex.withLock { maxOf(blockedUntil[resource] ?: 0, blockedUntil["all"] ?: 0) }
        if (deadline > now()) throw GitHubError.RateLimited(deadline)
        val response = underlying.send(
            request.copy(
                headers = request.headers + mapOf(
                    "Accept" to "application/vnd.github+json",
                    "X-GitHub-Api-Version" to "2026-03-10",
                    "User-Agent" to "native-sync-template"
                )
            )
        )
        currentCoroutineContext().ensureActive()
        if (response.status == 403 || response.status == 429) {
            val primary = response.headers["x-ratelimit-remaining"] == "0"
            if (response.status == 429 || primary || response.headers.containsKey("retry-after") ||
                response.body.contains("rate limit", ignoreCase = true)
            ) {
                val retry = response.headers["retry-after"]?.toDoubleOrNull()?.let { now() + (it * 1000).toLong() }
                val reset = if (primary) response.headers["x-ratelimit-reset"]?.toLongOrNull()?.times(1000) else null
                val until = maxOf(retry ?: reset ?: (now() + 60_000), now() + 1000)
                mutex.withLock { blockedUntil[if (primary) resource else "all"] = until }
                throw GitHubError.RateLimited(until)
            }
        }
        return when (response.status) {
            in 200..299 -> response
            404 -> throw GitHubError.NotFound()
            422 -> throw GitHubError.InvalidQuery()
            403 -> throw GitHubError.Forbidden()
            else -> throw GitHubError.Server(response.status)
        }
    }
}
