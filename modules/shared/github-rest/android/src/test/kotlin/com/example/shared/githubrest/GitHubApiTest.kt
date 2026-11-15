package com.example.shared.githubrest

import com.example.core.network.HttpRequest
import com.example.core.network.HttpResponse
import com.example.core.network.HttpTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** 決まったレスポンスを返す。本文を加工した応答を試すときに使う。 */
private class StubTransport(private val response: HttpResponse) : HttpTransport {
    override suspend fun send(request: HttpRequest): HttpResponse = response
}

class GitHubApiTest {
    // @scenario github-api/path-encoding
    @Test
    fun encodesPathSegments() = runTest {
        val transport = ScenarioTransport(ScenarioFixtures.GithubApi.pathEncoding)
        DefaultGitHubApi(transport).repository("owner/name", "repo/name")
        assertEquals("/repos/owner%2Fname/repo%2Fname", transport.requests.first().path)
    }

    // @scenario github-api/nullable
    @Test fun nullableUnknownFieldsAndLargeIDs() = runTest {
        val fixture = ScenarioFixtures.GithubApi.nullable.stubs.first()
        val api = DefaultGitHubApi(ScenarioTransport(ScenarioFixtures.GithubApi.nullable))
        val page = api.searchRepositories("swift", null, 1)
        assertEquals(5_000_000_001, page.items.first().id)
        assertEquals("octocat", page.items.first().owner)
        assertNull(page.items.first().description)

        val body = Json.parseToJsonElement(fixture.body).jsonObject.toMutableMap()
        val item = body.getValue("items").jsonArray.first().jsonObject.toMutableMap()
        item["future_field"] = JsonPrimitive(true)
        body["items"] = JsonArray(listOf(JsonObject(item)))
        DefaultGitHubApi(StubTransport(HttpResponse(200, JsonObject(body).toString())))
            .searchRepositories("swift", null, 1)

        item.remove("description")
        body["items"] = JsonArray(listOf(JsonObject(item)))
        val missing = DefaultGitHubApi(StubTransport(HttpResponse(200, JsonObject(body).toString())))
        assertFailsWith<kotlinx.serialization.SerializationException> { missing.searchRepositories("swift", null, 1) }
    }

    // @scenario github-api/pull-requests
    @Test fun prOnlyPagePreservesNextLinkAndNullableIssue() = runTest {
        val api = DefaultGitHubApi(ScenarioTransport(ScenarioFixtures.GithubApi.pullRequests))
        val detail = api.issue("octocat", "Hello-World", 1)
        assertNull(detail.author)
        assertNull(detail.body)
        val page = api.issues("octocat", "Hello-World", "open", 1)
        assertTrue(page.items.isEmpty())
        assertEquals(2, page.nextPage)
    }

    // @scenario github-api/search-limit
    @Test fun searchLimitAndResponseHeaders() = runTest {
        val transport = ScenarioTransport(ScenarioFixtures.GithubApi.searchLimit)
        val result = DefaultGitHubApi(GitHubTransport(transport)).searchRepositories("a/b", "stars", 50)
        assertNull(result.nextPage)
        assertTrue(result.limitReached && result.incomplete)
        val headers = transport.requests.first().headers
        assertEquals("2026-03-10", headers["X-GitHub-Api-Version"])
        assertEquals("application/vnd.github+json", headers["Accept"])
        assertNotNull(headers["User-Agent"])
    }

    // @scenario github-api/rate-limit
    @Test fun suppressesRequestsUntilTheRetryTime() = runTest {
        val step = ScenarioFixtures.GithubApi.rateLimit
        for (stub in step.stubs) {
            var now = 1_000_000L
            val underlying = ScenarioTransport(ScenarioStep(step.id, listOf(stub)))
            val transport = GitHubTransport(underlying) { now }
            val request = HttpRequest("GET", "/search/repositories")
            repeat(2) {
                val error = assertFailsWith<GitHubError.RateLimited> { transport.send(request) }
                assertTrue(error.until >= 1_060_000)
            }
            assertEquals(1, underlying.requests.size)
            now += 121_000
            assertFailsWith<GitHubError.RateLimited> { transport.send(request) }
            assertEquals(2, underlying.requests.size)
        }
    }

    // @scenario github-api/ordinary-forbidden
    @Test fun ordinaryForbiddenIsNotARateLimit() = runTest {
        val underlying = ScenarioTransport(ScenarioFixtures.GithubApi.ordinaryForbidden)
        val transport = GitHubTransport(underlying)
        repeat(2) { assertFailsWith<GitHubError.Forbidden> { transport.send(HttpRequest("GET", "/repos/a/b")) } }
        assertEquals(2, underlying.requests.size)
    }
}
