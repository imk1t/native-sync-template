package com.example.features.search

import com.example.corelocalization.Message
data class PagedState<T>(
    val items: List<T> = emptyList(),
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: SearchFailure? = null,
    val nextPage: Int? = null,
    val incomplete: Boolean = false,
    val limitReached: Boolean = false
)
