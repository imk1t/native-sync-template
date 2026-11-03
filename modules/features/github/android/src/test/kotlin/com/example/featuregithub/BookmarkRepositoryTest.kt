package com.example.featuregithub

import com.example.corenavigation.Navigator
import com.example.corenavigation.Route
import com.example.corestorage.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest

class BookmarkRepositoryTest {
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

    // @scenario github-bookmarks/note-save-cancel
    // @scenario github-bookmarks/note-fails
    @Test fun noteSaveCancelAndFailure() = runTest {
        val store = FailingStore()
        val bookmarks = BookmarkRepository(store)
        bookmarks.toggle(sample())
        val navigator = Navigator()
        navigator.present(Route.BookmarkNote(sample().id.toString()))
        val model = BookmarkNoteViewModel(sample().id.toString(), bookmarks, navigator)
        model.loadNow()
        model.edit("保存")
        model.saveNow()
        assertEquals("保存", bookmarks.items.value.first().note)
        assertTrue(navigator.state.value.modals.isEmpty())
        navigator.present(Route.BookmarkNote(sample().id.toString()))
        val editing = BookmarkNoteViewModel(sample().id.toString(), bookmarks, navigator)
        editing.loadNow()
        editing.edit("下書き")
        store.fails = true
        editing.saveNow()
        assertNotNull(editing.state.value.error)
        assertEquals("下書き", editing.state.value.note)
        assertEquals(1, navigator.state.value.modals.size)
        assertEquals("保存", bookmarks.items.value.first().note)
        editing.cancel()
        assertTrue(navigator.state.value.modals.isEmpty())
    }
}
