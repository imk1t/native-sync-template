package com.example.shared.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

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

private fun TextStyle.with(token: TextToken, lineHeight: TextUnit): TextStyle =
    copy(fontSize = token.size, fontWeight = token.weight, lineHeight = lineHeight)

/** 文字サイズと太さは Tokens の Typography（見出し h1〜h3、本文 body、補足 caption）に合わせる。 */
private fun typography(): Typography {
    val base = Typography()
    return base.copy(
        headlineLarge = base.headlineLarge.with(Tokens.Typography.h1, 44.sp),
        headlineMedium = base.headlineMedium.with(Tokens.Typography.h2, 40.sp),
        headlineSmall = base.headlineSmall.with(Tokens.Typography.h3, 34.sp),
        bodyLarge = base.bodyLarge.with(Tokens.Typography.body, 22.sp),
        bodyMedium = base.bodyMedium.with(Tokens.Typography.body, 22.sp),
        bodySmall = base.bodySmall.with(Tokens.Typography.caption, 18.sp)
    )
}

/** アプリ全体の配色と文字。OS のライト・ダーク設定に従い、色と文字サイズは Tokens から選ぶ。 */
@Composable
fun DesignTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) dark() else light(),
        typography = typography(),
        content = content
    )
}
