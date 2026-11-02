package com.example.corenetwork

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

private class StubTransport(private val response: HttpResponse) : HttpTransport {
    val requests = mutableListOf<HttpRequest>()

    override suspend fun send(request: HttpRequest): HttpResponse {
        requests.add(request)
        return response
    }
}

class DefaultItemsApiTest {
    @Test
    fun listItemsDecodesResponseIntoDomainModels() = runTest {
        val json = """[{"id":"1","title":"First","extra":true},{"id":"2","title":"Second"}]"""
        val transport = StubTransport(HttpResponse(200, json))
        val items = DefaultItemsApi(transport).listItems()
        assertEquals(listOf(Item("1", "First"), Item("2", "Second")), items)
        assertEquals(listOf(HttpRequest(method = "GET", path = "/items")), transport.requests)
    }

    @Test
    fun getItemRequestsEscapedPath() = runTest {
        val transport = StubTransport(HttpResponse(200, """{"id":"a/1","title":"First"}"""))
        val item = DefaultItemsApi(transport).getItem("a/1")
        assertEquals(Item("a/1", "First"), item)
        assertEquals(listOf(HttpRequest(method = "GET", path = "/items/a%2F1")), transport.requests)
    }

    @Test
    fun nonSuccessStatusThrowsApiException() = runTest {
        val transport = StubTransport(HttpResponse(500))
        val error = assertFailsWith<ApiException> { DefaultItemsApi(transport).listItems() }
        assertEquals(500, error.status)
    }

    @Test
    fun pathSegmentEscapesSlash() {
        assertEquals("a%2Fb%20c", pathSegment("a/b c"))
    }
}
