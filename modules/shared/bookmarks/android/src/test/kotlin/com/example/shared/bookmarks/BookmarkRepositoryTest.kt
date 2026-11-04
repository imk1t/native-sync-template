package com.example.shared.bookmarks

import com.example.core.storage.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest

class BookmarkRepositoryTest {
    @Test fun readsExistingVersionOneDocument() = runTest {
        val document = """
            {"version":1,"items":[{"repository":{"id":5000000001,"name":"repo","fullName":"owner/repo",
            "owner":"owner","stars":2,"forks":0,"topics":[],"htmlURL":"https://github.com/owner/repo"},
            "note":"移動前のメモ","savedAt":1730000000}]}
        """.trimIndent()
        val store = InMemoryKeyValueStore(mapOf("github.bookmarks.v1" to document))
        val bookmarks = BookmarkRepository(store)
        bookmarks.load()
        assertEquals(5_000_000_001, bookmarks.items.value.single().id)
        assertEquals("移動前のメモ", bookmarks.items.value.single().note)
        assertEquals(1_730_000_000.0, bookmarks.items.value.single().savedAt)
        assertEquals(document, store.get("github.bookmarks.v1"))
    }

    // @scenario github-bookmarks/save-restore
    @Test fun saveAndRestore() = runTest {
        val storage = InMemoryKeyValueStore()
        val bookmarks = BookmarkRepository(storage)
        bookmarks.toggle(sample())
        bookmarks.saveNote(sample().id, "メモ")
        val restored = BookmarkRepository(storage)
        restored.load()
        assertEquals("メモ", restored.items.value.first().note)
        assertTrue(restored.contains(sample().id))
        restored.toggle(sample())
        assertTrue(restored.items.value.isEmpty())
    }

    // @scenario github-bookmarks/limits
    @Test fun enforcesLimitsAndCountsCodePoints() = runTest {
        val bookmarks = BookmarkRepository(InMemoryKeyValueStore())
        for (id in 1L..100L) bookmarks.toggle(sample(id))
        assertFailsWith<BookmarkError.Limit> { bookmarks.toggle(sample(101)) }
        assertEquals(100, bookmarks.items.value.size)
        val note = "😀".repeat(1_000)
        bookmarks.saveNote(1, note)
        assertFailsWith<BookmarkError.NoteTooLong> { bookmarks.saveNote(1, note + "a") }
        assertEquals(note, bookmarks.items.value.first { it.id == 1L }.note)
    }

    // @scenario github-bookmarks/write-fails
    @Test fun failedWritesDoNotPublishChanges() = runTest {
        val storage = FailingStore()
        val bookmarks = BookmarkRepository(storage)
        bookmarks.toggle(sample())
        storage.fails = true
        assertFailsWith<Exception> { bookmarks.remove(sample().id) }
        assertTrue(bookmarks.contains(sample().id))
    }

    // @scenario github-bookmarks/unknown-version
    @Test fun invalidStorageIsNotOverwritten() = runTest {
        for (value in listOf("invalid json", """{"version":2,"items":[]}""", """{"items":[]}""")) {
            val store = InMemoryKeyValueStore(mapOf(BookmarkRepository.KEY to value))
            val bookmarks = BookmarkRepository(store)
            assertFailsWith<Exception> { bookmarks.toggle(sample()) }
            assertEquals(value, store.get(BookmarkRepository.KEY))
        }
    }

    // @scenario github-bookmarks/concurrent-writes
    @Test fun concurrentMutationsAreSerialized() = runTest {
        val bookmarks = BookmarkRepository(InMemoryKeyValueStore())
        val first = async { bookmarks.toggle(sample(1)) }
        val second = async { bookmarks.toggle(sample(2)) }
        first.await()
        second.await()
        assertEquals(setOf(1L, 2L), bookmarks.items.value.map { it.id }.toSet())
    }
}
