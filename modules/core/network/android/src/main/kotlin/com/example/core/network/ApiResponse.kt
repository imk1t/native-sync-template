package com.example.core.network

internal data class ApiResponse<T>(val value: T, val headers: Map<String, String>)
