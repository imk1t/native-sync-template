package com.example.corenetwork

internal data class ApiResponse<T>(val value: T, val headers: Map<String, String>)
