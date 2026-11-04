package com.example.shared.bookmarks

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
internal data class BookmarkDocument(@Required val version: Int = 1, val items: List<Bookmark>)
