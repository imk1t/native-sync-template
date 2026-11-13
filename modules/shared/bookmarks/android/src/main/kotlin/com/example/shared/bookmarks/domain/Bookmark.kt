package com.example.shared.bookmarks

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class Bookmark(
    val repository: BookmarkedRepository,
    @Required val note: String = "",
    @Required val savedAt: Double = System.currentTimeMillis() / 1000.0
) {
    val id: Long get() = repository.id
}
