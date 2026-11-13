package com.example.shared.bookmarks

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

/** 保存時点のリポジトリ表示用スナップショット。GitHub API のモデルには依存しない。 */
@Serializable
data class BookmarkedRepository(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: String,
    val avatarURL: String? = null,
    val description: String? = null,
    val language: String? = null,
    @Required val stars: Int = 0,
    @Required val forks: Int = 0,
    @Required val topics: List<String> = emptyList(),
    @Required val htmlURL: String = ""
)
