package com.example.shared.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** プログラミング言語を示す点の色。未知の言語は muted（onSurfaceVariant）。 */
@Composable
fun languageTint(language: String?): Color = when (language) {
    "Swift" -> Color(0xFFE8452C)
    "Kotlin" -> Color(0xFF7F52FF)
    "TypeScript" -> Color(0xFF3178C6)
    "Rust" -> Color(0xFFB7410E)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
