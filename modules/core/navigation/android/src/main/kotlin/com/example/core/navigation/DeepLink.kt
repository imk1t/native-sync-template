package com.example.core.navigation

import java.net.URI

/** エンコード済みパスを渡し、区切りごとのデコードを fromPath に一度だけ任せる。 */
fun Route.Companion.fromUrl(url: String): Route? = runCatching {
    URI(url).rawPath?.let { fromPath(it) }
}.getOrNull()
