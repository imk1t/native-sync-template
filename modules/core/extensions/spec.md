# core/extensions

## Overview

Provide general extensions of standard types without feature-specific knowledge. Depend on no other modules or third-party libraries. The standard library, Foundation, and JDK are allowed.

## Navigation

None.

## Screens and state

No screens or state. Compute values from input without mutating the original string.

## User actions and outcomes

| Name | Behavior |
| --- | --- |
| `String.isBlankOrEmpty` | Returns true for an empty string or one containing only the defined whitespace characters |
| `String.codePointCount` | Counts Unicode code points, distinct from visible characters or UTF-16 length |
| `String.trimmedWhitespace()` | Removes leading and trailing whitespace only; preserves internal whitespace and newlines |

Blank checks and trimming use the same set: U+0009–000D, U+0020, U+0085, U+00A0, U+1680, U+2000–200A, U+2028, U+2029, U+202F, U+205F, and U+3000. U+001C–001F, U+200B, and U+FEFF are not whitespace. Results do not depend on OS or standard API whitespace definitions.

Do not normalize or truncate combining characters or emoji. For example, `😀` is one code point, `e` + U+0301 is two, and `👨‍👩‍👧‍👦` is seven.

## APIs and tokens

None. Use properties for queries and predicates, and functions for transformations. Do not duplicate these operations through Utils APIs. Extensions with module-specific knowledge, such as Message, Route, or business models, stay in their owning modules.

## Platform differences

The iOS module is `CoreExtensions`, with implementation in `Sources/Core/Extensions` and tests in `Tests/Core/Extensions`. The Android package is `com.example.core.extensions`. Swift exposes String extensions; Kotlin exposes top-level extension functions and properties. Code point counts use Swift Unicode scalars and Kotlin/JVM codePointCount.

## Scenarios

Unit tests on both platforms verify the whitespace set, preservation of internal whitespace, and combining-character/emoji counts. Consumer scenarios are `specs/scenarios/github-search.yaml` and `specs/scenarios/bookmark-note.yaml`.
