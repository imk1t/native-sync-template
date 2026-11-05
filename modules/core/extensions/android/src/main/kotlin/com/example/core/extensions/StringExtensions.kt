package com.example.core.extensions

val String.isBlankOrEmpty: Boolean get() = all { it.isUtilityWhitespace }

val String.codePointCount: Int get() = codePointCount(0, length)

fun String.trimmedWhitespace(): String = trim { it.isUtilityWhitespace }

private val Char.isUtilityWhitespace: Boolean
    get() = when (code) {
        in 0x0009..0x000D, 0x0020, 0x0085, 0x00A0, 0x1680, in 0x2000..0x200A,
        0x2028, 0x2029, 0x202F, 0x205F, 0x3000 -> true
        else -> false
    }
