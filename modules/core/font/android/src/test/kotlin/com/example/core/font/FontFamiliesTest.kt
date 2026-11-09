package com.example.core.font

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FontFamiliesTest {
    @Test
    fun `同梱の書体名から FontFamily を引ける`() {
        assertEquals(FontFamilies.interTight, FontFamilies.lookup("Inter Tight"))
        assertEquals(FontFamilies.jetBrainsMono, FontFamilies.lookup("JetBrains Mono"))
        assertNotNull(FontFamilies.lookup("Inter Tight"))
    }

    @Test
    fun `同梱していない書体名は null`() {
        assertNull(FontFamilies.lookup("Noto Sans JP"))
        assertNull(FontFamilies.lookup(""))
    }
}
