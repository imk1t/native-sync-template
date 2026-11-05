package com.example.features.bookmarks

import com.example.core.navigation.Navigator
import com.example.core.navigation.Route
import com.example.shared.bookmarks.BookmarkError
import com.example.shared.bookmarks.BookmarkRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class BookmarkNoteViewModelTest {
    @Test fun noteLengthMatchesRepositoryValidation() = runTest {
        val bookmarks = BookmarkRepository(FailingStore())
        bookmarks.toggle(sample())
        val model = BookmarkNoteViewModel(sample().id.toString(), bookmarks, Navigator())
        assertFalse(model.state.value.canSave)
        model.loadNow()
        assertTrue(model.state.value.canSave)
        model.edit("e\u0301".repeat(500))
        assertEquals(1_000, model.state.value.characterCount)
        assertTrue(model.state.value.canSave)
        model.saveNow()
        assertEquals(model.state.value.note, bookmarks.items.value.first().note)
        model.edit(model.state.value.note + "😀")
        assertEquals(1_001, model.state.value.characterCount)
        assertFalse(model.state.value.canSave)
        model.saveNow()
        assertNotEquals(model.state.value.note, bookmarks.items.value.first().note)
        assertFailsWith<BookmarkError.NoteTooLong> { bookmarks.saveNote(sample().id, model.state.value.note) }
    }

    // @scenario bookmark-note/note-save-cancel
    // @scenario bookmark-note/note-fails
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
