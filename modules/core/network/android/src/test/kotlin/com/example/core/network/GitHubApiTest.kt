package com.example.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

private class StubTransport(private val response: HttpResponse) : HttpTransport {
    val requests = mutableListOf<HttpRequest>()
    override suspend fun send(request: HttpRequest): HttpResponse {
        requests.add(request)
        return response
    }
}

class GitHubApiTest {
    @Test
    fun encodesPathSegments() = runTest {
        val body = TestResponses.repository
        val stub = StubTransport(HttpResponse(200, body))
        DefaultGitHubApi(stub).repository("owner/name", "repo/name")
        assertEquals("/repos/owner%2Fname/repo%2Fname", stub.requests.first().path)
    }

    // @scenario github-network/nullable
    @Test fun nullableUnknownFieldsAndLargeIDs() = runTest {
        val body = Json.parseToJsonElement(
            TestResponses.search
        ).jsonObject.toMutableMap()
        val item = body.getValue("items").jsonArray.first().jsonObject.toMutableMap()
        item["description"] = JsonNull
        item["owner"] = JsonNull
        item["language"] = JsonNull
        item["future_field"] = JsonPrimitive(true)
        body["items"] = JsonArray(listOf(JsonObject(item)))
        val api = DefaultGitHubApi(StubTransport(HttpResponse(200, JsonObject(body).toString())))
        val page = api.searchRepositories("swift", null, 1)
        assertEquals(5_000_000_001, page.items.first().id)
        assertEquals("octocat", page.items.first().owner)
        assertNull(page.items.first().description)
        item.remove("description")
        body["items"] = JsonArray(listOf(JsonObject(item)))
        val missing = DefaultGitHubApi(StubTransport(HttpResponse(200, JsonObject(body).toString())))
        assertFailsWith<kotlinx.serialization.SerializationException> { missing.searchRepositories("swift", null, 1) }
    }

    // @scenario github-network/pull-requests
    @Test fun prOnlyPagePreservesNextLinkAndNullableIssue() = runTest {
        val issue = Json.parseToJsonElement(TestResponses.issue)
            .jsonObject.toMutableMap()
        issue["user"] = JsonNull
        issue.remove("body")
        val detail = DefaultGitHubApi(StubTransport(HttpResponse(200, JsonObject(issue).toString())))
            .issue("octocat", "Hello-World", 1)
        assertNull(detail.author)
        assertNull(detail.body)
        issue["pull_request"] =
            buildJsonObject { put("url", "https://api.github.com/repos/octocat/Hello-World/pulls/1") }
        val response = HttpResponse(
            200,
            JsonArray(listOf(JsonObject(issue))).toString(),
            mapOf("Link" to """<https://api.github.com/repos/octocat/Hello-World/issues?page=2>; rel="next"""")
        )
        val page = DefaultGitHubApi(StubTransport(response)).issues("octocat", "Hello-World", "open", 1)
        assertTrue(page.items.isEmpty())
        assertEquals(2, page.nextPage)
    }

    // @scenario github-network/search-limit
    @Test fun searchLimitAndResponseHeaders() = runTest {
        val text = TestResponses.search
            .replace("\"total_count\":1", "\"total_count\":1001")
            .replace("\"incomplete_results\":false", "\"incomplete_results\":true")
        val stub = StubTransport(
            HttpResponse(
                200,
                text,
                mapOf("LINK" to """<https://api.github.com/search/repositories?page=51>; rel="next"""")
            )
        )
        val result = DefaultGitHubApi(GitHubTransport(stub)).searchRepositories("a/b", "stars", 50)
        assertNull(result.nextPage)
        assertTrue(result.limitReached && result.incomplete)
        val headers = stub.requests.first().headers
        assertEquals("2026-03-10", headers["X-GitHub-Api-Version"])
        assertEquals("application/vnd.github+json", headers["Accept"])
        assertNotNull(headers["User-Agent"])
    }

    // @scenario github-network/rate-limit
    // @scenario github-network/ordinary-forbidden
    @Test fun distinguishesRateLimitsAndSuppressesRequests() = runTest {
        for (response in listOf(
            HttpResponse(403, "", mapOf("X-RateLimit-Remaining" to "0", "X-RateLimit-Reset" to "1120")),
            HttpResponse(429, "", mapOf("Retry-After" to "120")),
            HttpResponse(403, """{"message":"secondary rate limit"}""")
        )) {
            var now = 1_000_000L
            val stub = StubTransport(response)
            val transport = GitHubTransport(stub) { now }
            val request = HttpRequest("GET", "/search/repositories")
            repeat(2) {
                val error = assertFailsWith<GitHubError.RateLimited> { transport.send(request) }
                assertTrue(error.until >= 1_060_000)
            }
            assertEquals(1, stub.requests.size)
            now += 121_000
            assertFailsWith<GitHubError.RateLimited> { transport.send(request) }
            assertEquals(2, stub.requests.size)
        }
        val normal = StubTransport(HttpResponse(403, ""))
        val transport = GitHubTransport(normal)
        repeat(2) { assertFailsWith<GitHubError.Forbidden> { transport.send(HttpRequest("GET", "/repos/a/b")) } }
        assertEquals(2, normal.requests.size)
    }
}
