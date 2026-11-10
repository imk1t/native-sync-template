package com.example.core.font

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** DESIGN.md の書体をアプリに同梱して使うための FontFamily。ファイルと書体の対応は spec.md に書く。 */
object FontFamilies {
    val interTight = FontFamily(
        Font(R.font.inter_tight_regular, FontWeight.Normal),
        Font(R.font.inter_tight_medium, FontWeight.Medium),
        Font(R.font.inter_tight_bold, FontWeight.Bold)
    )

    val jetBrainsMono = FontFamily(
        Font(R.font.jetbrains_mono_medium, FontWeight.Medium)
    )

    /** 書体名（DESIGN.md の fontFamily）に対応する同梱の FontFamily。同梱していなければ null。 */
    fun lookup(name: String): FontFamily? = when (name) {
        "Inter Tight" -> interTight
        "JetBrains Mono" -> jetBrainsMono
        else -> null
    }
}
