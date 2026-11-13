package com.example.core.network

/** 成功したレスポンスの値と、同じ呼び出しのヘッダー（小文字名）。 */
data class ApiResponse<T>(val value: T, val headers: Map<String, String>)
