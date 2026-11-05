package com.example.shared.bookmarks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookmarkNoteRulesTest {
    @Test fun acceptsEmptyAndCountsUnicodeAtTheLimit() {
        assertTrue(BookmarkNoteRules.isValid(""))
        val emoji = "😀".repeat(1_000)
        val combining = "e\u0301".repeat(500)
        for (note in listOf(emoji, combining)) {
            assertEquals(1_000, BookmarkNoteRules.characterCount(note))
            assertTrue(BookmarkNoteRules.isValid(note))
            assertFalse(BookmarkNoteRules.isValid(note + "a"))
        }
    }
}
