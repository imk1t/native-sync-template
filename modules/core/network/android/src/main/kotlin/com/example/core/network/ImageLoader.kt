package com.example.core.network

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class ImageLoader {
    private val cache = object : LinkedHashMap<String, ByteArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?) = size > 50
    }

    suspend fun load(url: String): ByteArray = withContext(Dispatchers.IO) {
        synchronized(cache) { cache[url] }?.let { return@withContext it }
        val target = URI(url)
        require(target.scheme == "https")
        val connection = target.toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            check(connection.responseCode == 200)
            val data = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= 2_000_000)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            check(data.size <= 2_000_000)
            currentCoroutineContext().ensureActive()
            synchronized(cache) { cache[url] = data }
            data
        } finally {
            connection.disconnect()
        }
    }
}
