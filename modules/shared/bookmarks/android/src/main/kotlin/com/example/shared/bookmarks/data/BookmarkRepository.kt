package com.example.shared.bookmarks

import com.example.core.network.RepositorySummary
import com.example.core.storage.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BookmarkRepository(private val store: KeyValueStore) {
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val mutableItems = MutableStateFlow<List<Bookmark>>(emptyList())
    val items: StateFlow<List<Bookmark>> = mutableItems
    private var isLoaded = false

    suspend fun load() = mutex.withLock { loadLocked() }

    private suspend fun loadLocked() {
        if (isLoaded) return
        val text = store.get(KEY)
        val document = text?.let { json.decodeFromString<BookmarkDocument>(it) }
        if (document != null && document.version != 1) throw BookmarkError.UnsupportedVersion()
        mutableItems.value = document?.items.orEmpty()
        isLoaded = true
    }

    fun contains(id: Long) = items.value.any { it.id == id }

    suspend fun toggle(repository: RepositorySummary) = mutate { items ->
        if (items.any { it.id == repository.id }) {
            items.filter { it.id != repository.id }
        } else {
            if (items.size >= 100) throw BookmarkError.Limit()
            listOf(Bookmark(repository)) + items
        }
    }

    suspend fun remove(id: Long) = mutate { it.filter { bookmark -> bookmark.id != id } }

    suspend fun saveNote(id: Long, note: String) {
        if (!BookmarkNoteRules.isValid(note)) throw BookmarkError.NoteTooLong()
        mutate { items ->
            if (items.none { it.id == id }) throw BookmarkError.NotFound()
            items.map { if (it.id == id) it.copy(note = note) else it }
        }
    }

    private suspend fun mutate(change: (List<Bookmark>) -> List<Bookmark>) = mutex.withLock {
        loadLocked()
        val updated = change(items.value)
        store.set(KEY, json.encodeToString(BookmarkDocument(items = updated)))
        mutableItems.value = updated
    }

    companion object {
        internal const val KEY = "github.bookmarks.v1"
    }
}
