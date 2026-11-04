package com.example.featurebookmarks

import com.example.corenavigation.Navigator
import com.example.corenavigation.Route
import com.example.sharedbookmarks.BookmarkRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class BookmarkNoteViewModelTest {
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
