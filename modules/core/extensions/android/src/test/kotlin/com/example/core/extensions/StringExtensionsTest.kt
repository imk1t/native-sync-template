package com.example.core.extensions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringExtensionsTest {
    @Test fun whitespaceContract() {
        val whitespace = "\u0009\u000A\u000B\u000C\u000D\u0020\u0085\u00A0\u1680" +
            "\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A" +
            "\u2028\u2029\u202F\u205F\u3000"
        assertTrue("".isBlankOrEmpty)
        assertEquals("", "".trimmedWhitespace())
        assertTrue(whitespace.isBlankOrEmpty)
        assertEquals("", whitespace.trimmedWhitespace())
        assertEquals("swift  kotlin\n日本語", (whitespace + "swift  kotlin\n日本語" + whitespace).trimmedWhitespace())
        for (text in listOf("\u001C", "\u001D", "\u001E", "\u001F", "\u200B", "\uFEFF")) {
            assertFalse(text.isBlankOrEmpty)
            assertEquals(text, (whitespace + text + whitespace).trimmedWhitespace())
        }
        assertEquals(0x0301, " \u0301x ".trimmedWhitespace().first().code)
    }

    @Test fun countsCodePointsWithoutNormalizing() {
        for ((text, count) in listOf(
            "" to 0,
            "abc" to 3,
            "日本語" to 3,
            "😀" to 1,
            "é" to 1,
            "e\u0301" to 2,
            "👨‍👩‍👧‍👦" to 7,
            "🇯🇵" to 2
        )) {
            assertEquals(count, text.codePointCount)
            assertEquals(count, (" $text ").trimmedWhitespace().codePointCount)
        }
    }
}
