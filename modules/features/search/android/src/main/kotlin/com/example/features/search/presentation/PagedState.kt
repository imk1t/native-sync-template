package com.example.features.search

data class PagedState<T>(
    val items: List<T> = emptyList(),
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val nextPage: Int? = null,
    val incomplete: Boolean = false,
    val limitReached: Boolean = false
)
