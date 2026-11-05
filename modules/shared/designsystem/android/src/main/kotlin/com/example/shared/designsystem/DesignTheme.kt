package com.example.shared.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun light(): ColorScheme = lightColorScheme(
    primary = Tokens.Colors.primary,
    onPrimary = Color.White,
    background = Tokens.Colors.background,
    onBackground = Tokens.Colors.onBackground,
    surface = Tokens.Colors.surface,
    onSurface = Tokens.Colors.onBackground,
    onSurfaceVariant = Tokens.Colors.muted,
    outlineVariant = Tokens.Colors.line
)

private fun dark(): ColorScheme = darkColorScheme(
    primary = Tokens.DarkColors.primary,
    onPrimary = Tokens.DarkColors.background,
    background = Tokens.DarkColors.background,
    onBackground = Tokens.DarkColors.onBackground,
    surface = Tokens.DarkColors.surface,
    onSurface = Tokens.DarkColors.onBackground,
    onSurfaceVariant = Tokens.DarkColors.muted,
    outlineVariant = Tokens.DarkColors.line
)

/** アプリ全体の配色。OS のライト・ダーク設定に従い、色は Tokens から選ぶ。 */
@Composable
fun DesignTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) dark() else light(), content = content)
}
