package com.example.shared.githubrest

data class Page<T>(
    val items: List<T>,
    val nextPage: Int? = null,
    val incomplete: Boolean = false,
    val limitReached: Boolean = false
)
