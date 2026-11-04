package com.example.corenetwork

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** 待機は disconnect まで続く。割り込みだけで終了しない接続も再現する。 */
private class TestConnection(private val pendingBody: Boolean? = null, private val status: Int = 200) :
    HttpURLConnection(URI("https://example.invalid/items").toURL()) {
    val entered = CountDownLatch(1)
    private val released = CountDownLatch(1)

    @Volatile var disconnected = false

    @Volatile var streamClosed = false

    override fun connect() = Unit
    override fun usingProxy() = false
    override fun disconnect() {
        disconnected = true
        released.countDown()
    }

    private fun awaitDisconnect(): Nothing {
        entered.countDown()
        while (!disconnected) {
            try {
                check(released.await(5, TimeUnit.SECONDS)) { "connection was not disconnected" }
            } catch (_: InterruptedException) {
                // 割り込みに反応しない通信でも disconnect が呼ばれなければテストは失敗する。
            }
        }
        throw IOException("connection closed")
    }

    override fun getResponseCode(): Int {
        if (pendingBody == false) awaitDisconnect()
        return status
    }

    private fun responseStream(): InputStream = if (pendingBody == true) {
        object : InputStream() {
            override fun read(): Int = awaitDisconnect()
            override fun close() {
                streamClosed = true
            }
        }
    } else {
        object : ByteArrayInputStream("[]".toByteArray()) {
            override fun close() {
                streamClosed = true
            }
        }
    }

    override fun getInputStream() = responseStream()
    override fun getErrorStream() = responseStream()
}

class UrlConnectionTransportTest {
    @Test
    fun cancellationDisconnectsPendingResponseAndBody() = runBlocking {
        for (pendingBody in listOf(false, true)) {
            val connection = TestConnection(pendingBody)
            val transport = UrlConnectionTransport("https://example.invalid", 15_000, 30_000) { connection }
            val request = async { transport.send(HttpRequest("GET", "/items")) }
            try {
                assertTrue(withContext(Dispatchers.IO) { connection.entered.await(2, TimeUnit.SECONDS) })
                withTimeout(2_000) { request.cancelAndJoin() }
                assertTrue(connection.disconnected)
                assertFailsWith<CancellationException> { request.await() }
                if (pendingBody) assertTrue(connection.streamClosed)
            } finally {
                connection.disconnect()
                request.cancelAndJoin()
            }
        }
    }

    @Test
    fun responseAndErrorStreamsAreClosedAndTimeoutsAreApplied() = runBlocking {
        for (status in listOf(200, 500)) {
            val connection = TestConnection(status = status)
            val transport = UrlConnectionTransport("https://example.invalid", 123, 456) { connection }
            val response = transport.send(HttpRequest("GET", "/search/repositories"))
            assertEquals(status, response.status)
            assertEquals("[]", response.body)
            assertEquals(123, connection.connectTimeout)
            assertEquals(456, connection.readTimeout)
            assertTrue(connection.streamClosed)
            assertTrue(connection.disconnected)
        }
    }

    @Test
    fun infiniteOrNegativeTimeoutsAreRejected() {
        assertFailsWith<IllegalArgumentException> { UrlConnectionTransport("https://example.invalid", 0) }
        assertFailsWith<IllegalArgumentException> {
            UrlConnectionTransport("https://example.invalid", readTimeoutMillis = 0)
        }
        assertFailsWith<IllegalArgumentException> { UrlConnectionTransport("https://example.invalid", -1) }
    }
}
