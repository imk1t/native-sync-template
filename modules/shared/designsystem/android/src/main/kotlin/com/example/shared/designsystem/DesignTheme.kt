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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.core.font.FontFamilies

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

/** 書体名から同梱の書体を引く。ない書体は OS の書体（引数の既定）にする。 */
private fun fontFamily(name: String, fallback: FontFamily? = null): FontFamily? = FontFamilies.lookup(name) ?: fallback

private fun TextStyle.with(token: TextToken, lineHeight: TextUnit): TextStyle = copy(
    fontFamily = fontFamily(token.family, fontFamily),
    fontSize = token.size,
    fontWeight = token.weight,
    lineHeight = lineHeight
)

/** 全部の文字スタイルに、本文の書体を当てる（日本語など同梱の書体にない字は OS の書体になる）。 */
private fun Typography.withFamily(family: FontFamily?): Typography = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family)
)

/** 書体・文字サイズ・太さは Tokens の Typography（見出し h1〜h3、本文 body、補足 caption）に合わせる。 */
private fun typography(): Typography {
    val base = Typography().withFamily(fontFamily(Tokens.Typography.body.family))
    return base.copy(
        headlineLarge = base.headlineLarge.with(Tokens.Typography.h1, 44.sp),
        headlineMedium = base.headlineMedium.with(Tokens.Typography.h2, 40.sp),
        headlineSmall = base.headlineSmall.with(Tokens.Typography.h3, 34.sp),
        bodyLarge = base.bodyLarge.with(Tokens.Typography.body, 22.sp),
        bodyMedium = base.bodyMedium.with(Tokens.Typography.body, 22.sp),
        bodySmall = base.bodySmall.with(Tokens.Typography.caption, 18.sp)
    )
}

/** 識別子や数値に使う等幅の文字スタイル（Tokens の mono）。同梱の書体がなければ OS の等幅書体にする。 */
@Composable
fun monoTextStyle(): TextStyle = MaterialTheme.typography.bodyMedium.copy(
    fontFamily = fontFamily(Tokens.Typography.mono.family, FontFamily.Monospace),
    fontSize = Tokens.Typography.mono.size,
    fontWeight = Tokens.Typography.mono.weight
)

/** アプリ全体の配色と文字。OS のライト・ダーク設定に従い、色と文字サイズは Tokens から選ぶ。 */
@Composable
fun DesignTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) dark() else light(),
        typography = typography(),
        content = content
    )
}
