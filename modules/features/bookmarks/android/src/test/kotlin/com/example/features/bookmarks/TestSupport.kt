package com.example.features.bookmarks

import com.example.core.storage.KeyValueStore
import com.example.shared.bookmarks.BookmarkedRepository
import java.io.IOException

class FailingStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()
    var fails = false
    override suspend fun get(key: String) = values[key]
    override suspend fun set(key: String, value: String?) {
        if (fails) throw IOException("write failed")
        if (value == null) values.remove(key) else values[key] = value
    }
}

fun sample(id: Long = 5_000_000_001) = BookmarkedRepository(id, "repo", "owner/repo", "owner")
