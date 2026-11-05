package com.example.shared.bookmarks

import com.example.core.extensions.codePointCount

object BookmarkNoteRules {
    val maxCodePoints = 1_000

    fun characterCount(note: String): Int = note.codePointCount

    fun isValid(note: String): Boolean = characterCount(note) <= maxCodePoints
}
