package com.example.sharedbookmarks

import com.example.corenetwork.RepositorySummary
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class Bookmark(
    val repository: RepositorySummary,
    @Required val note: String = "",
    @Required val savedAt: Double = System.currentTimeMillis() / 1000.0
) {
    val id: Long get() = repository.id
}
