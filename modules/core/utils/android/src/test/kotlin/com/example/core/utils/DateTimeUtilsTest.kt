package com.example.core.utils

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateTimeUtilsTest {
    @Test fun usesExplicitLocaleAndTimeZone() {
        val date = Instant.ofEpochSecond(54_245)
        val utc = ZoneId.of("UTC")
        val tokyo = ZoneId.of("Asia/Tokyo")
        assertEquals("15:04:05", DateTimeUtils.formatTime(date, Locale.UK, utc))
        assertEquals("00:04:05", DateTimeUtils.formatTime(date, Locale.UK, tokyo))
        assertEquals("15:04:05", DateTimeUtils.formatTime(date, Locale.JAPAN, utc))
        val american = DateTimeUtils.formatTime(date, Locale.US, utc)
        assertTrue(american.startsWith("3:04:05") && american.endsWith("PM"))
        assertEquals("15:04:05", DateTimeUtils.formatTime(date, Locale.UK, utc))
    }

    @Test fun observesDaylightSavingTransition() {
        val zone = ZoneId.of("America/New_York")
        val before = Instant.ofEpochSecond(1_710_053_999)
        val after = Instant.ofEpochSecond(1_710_054_000)
        assertEquals("01:59:59", DateTimeUtils.formatTime(before, Locale.UK, zone))
        assertEquals("03:00:00", DateTimeUtils.formatTime(after, Locale.UK, zone))
    }
}
