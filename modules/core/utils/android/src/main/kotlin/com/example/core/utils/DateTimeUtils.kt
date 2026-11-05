package com.example.core.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateTimeUtils {
    fun formatTime(date: Instant, locale: Locale, timeZone: ZoneId): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
            .withLocale(locale).withZone(timeZone).format(date)
}
