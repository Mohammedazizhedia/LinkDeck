package com.linkdeck.core.design

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class AppFont(val fileName: String) {
    CAIRO("cairo.ttf"),
    TAJAWAL("tajawal.ttf"),
    AMIRI("amiri.ttf"),
    SYSTEM("");
}

enum class IconStroke(val width: Float) {
    THIN(1.5f), REGULAR(2f), BOLD(2.75f);
}

/**
 * Loads a bundled variable font from assets (or the system font for [AppFont.SYSTEM]).
 * Falls back to the default family gracefully when the TTF is not bundled yet —
 * drop cairo.ttf / tajawal.ttf / amiri.ttf into app/src/main/assets/fonts to enable them.
 */
fun loadAppFontFamily(context: Context, font: AppFont, weight: Int?): FontFamily {
    if (font == AppFont.SYSTEM) return FontFamily.Default
    val base: Typeface = runCatching {
        if (Build.VERSION.SDK_INT >= 26) {
            Typeface.Builder(context.assets, "fonts/${font.fileName}").build()
        } else {
            Typeface.createFromAsset(context.assets, "fonts/${font.fileName}")
        }
    }.getOrNull() ?: return FontFamily.Default

    val weighted: Typeface = if (weight != null && Build.VERSION.SDK_INT >= 28) {
        Typeface.create(base, weight, false)
    } else base
    return FontFamily(Font(weighted))
}

/**
 * Typography with the exact sizes from the design spec. [fontScale] multiplies
 * every size; [weight] drives variable-font weight on API 28+.
 */
fun linkDeckTypography(family: FontFamily, fontScale: Float, weight: Int?): Typography {
    val w = FontWeight(weight ?: 500)
    fun s(sp: Int) = (sp * fontScale).sp
    fun style(sizeSp: Int) = TextStyle(fontFamily = family, fontSize = s(sizeSp), fontWeight = w)
    return Typography(
        headlineMedium = style(22),
        headlineSmall = style(20),
        titleLarge = style(22),
        titleMedium = style(15),
        titleSmall = style(13),
        bodyLarge = style(15),
        bodyMedium = style(14),
        bodySmall = style(12),
        labelLarge = style(14),
        labelMedium = style(12),
        labelSmall = style(11),
    )
}
