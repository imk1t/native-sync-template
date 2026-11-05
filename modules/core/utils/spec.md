# core/utils

## Overview

Provide general operations through dedicated types, without feature-specific knowledge. The only permitted internal dependency is `core/extensions`, when needed. Do not depend on third-party libraries. The standard library, Foundation, and JDK are allowed.

## Public API

| Name | Behavior |
| --- | --- |
| `DateTimeUtils.formatTime(date, locale, timeZone)` | Formats a date as a time including seconds for the supplied locale and time zone |

Use the OS standard medium time format. The locale determines 12/24-hour notation and AM/PM labels. Callers supply the locale and time zone at display time. Do not show the date, time-zone name, or milliseconds. Do not depend on translation resources, UI, or the current time, and do not share mutable formatters.

General string extensions belong in `core/extensions`. Translated text containing numbers and plurals uses Message in `core/localization`, outside this module.

## Platform differences

The iOS module is `CoreUtils`, with implementation in `Sources/Core/Utils` and tests in `Tests/Core/Utils`. The Android package is `com.example.core.utils`.

Date/time types are Swift Date and Kotlin Instant; time-zone types are TimeZone and ZoneId. Formatting uses Foundation DateFormatter and JDK DateTimeFormatter. Localized whitespace, punctuation, and similar details follow the platform's format.

## Verification

Format the same instant using Japanese and English locales and different time zones. Verify date-boundary offsets and daylight saving time with fixed instants.
