package com.example.corenetwork

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** 生成された ApiClient が組み立てるリクエスト。ベース URL は持たず、Transport が解決する。 */
data class HttpRequest(
    val method: String,
    val path: String,
    val query: List<Pair<String, String>> = emptyList(),
    val body: String? = null,
    val headers: Map<String, String> = emptyMap()
)

class HttpResponse(val status: Int, val body: String = "", headers: Map<String, String> = emptyMap()) {
    val headers = headers.mapKeys { it.key.lowercase(java.util.Locale.ROOT) }
}

/** 2xx 以外のレスポンス。 */
class ApiException(val status: Int, val headers: Map<String, String> = emptyMap(), val body: String = "") :
    Exception("HTTP $status")

/** 通信の差し替え口。本番は UrlConnectionTransport、テストはスタブを渡す。 */
interface HttpTransport {
    suspend fun send(request: HttpRequest): HttpResponse
}

/** 未知のキーは無視する。必須 nullable のキー欠落を null とみなさず、両 OS で拒否する。 */
internal val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
}

internal suspend inline fun <reified T> HttpTransport.call(request: HttpRequest): ApiResponse<T> {
    val response = send(request)
    if (response.status !in 200..299) throw ApiException(response.status, response.headers, response.body)
    return ApiResponse(ApiJson.decodeFromString<T>(response.body), response.headers)
}

internal suspend fun HttpTransport.callUnit(request: HttpRequest) {
    val response = send(request)
    if (response.status !in 200..299) throw ApiException(response.status, response.headers, response.body)
}

/** パスの 1 区切りとして埋め込む値をエンコードする（"/" もエスケープする）。 */
internal fun pathSegment(value: String): String = encode(value)

private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

class UrlConnectionTransport internal constructor(
    private val baseUrl: String,
    private val connectTimeoutMillis: Int,
    private val readTimeoutMillis: Int,
    private val openConnection: (URL) -> HttpURLConnection
) : HttpTransport {
    constructor(baseUrl: String, connectTimeoutMillis: Int = 15_000, readTimeoutMillis: Int = 30_000) :
        this(baseUrl, connectTimeoutMillis, readTimeoutMillis, { it.openConnection() as HttpURLConnection })

    init {
        require(connectTimeoutMillis > 0 && readTimeoutMillis > 0) { "タイムアウトには正の値を指定してください" }
    }

    override suspend fun send(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        val query = request.query.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }
        val url = baseUrl.trimEnd('/') + request.path + if (query.isEmpty()) "" else "?$query"
        val connection = openConnection(URI(url).toURL())
        try {
            connection.connectTimeout = connectTimeoutMillis
            connection.readTimeout = readTimeoutMillis
            suspendCancellableCoroutine { continuation ->
                // IO への切り替えだけでは通信は止まらない。割り込みと接続の切断を両方行う。
                continuation.invokeOnCancellation { connection.disconnect() }
                launch {
                    try {
                        val response = runInterruptible { exchange(connection, request) }
                        continuation.resume(response)
                    } catch (error: Exception) {
                        continuation.resumeWithException(error)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun exchange(connection: HttpURLConnection, request: HttpRequest): HttpResponse {
        connection.requestMethod = request.method
        connection.setRequestProperty("Accept", "application/json")
        request.body?.let { body ->
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        request.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val headers = connection.headerFields.orEmpty().filterKeys { it != null }
            .mapValues { it.value.joinToString(", ") }
        return HttpResponse(status, stream?.bufferedReader()?.use { it.readText() }.orEmpty(), headers)
    }
}
