package com.linkdeck.core.design

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.io.File

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
 * Loads a bundled font from assets into app storage once, then builds a
 * Compose FontFamily from the file (official API for runtime-loaded fonts).
 * Falls back to the system font when the TTF is not bundled yet.
 */
fun loadAppFontFamily(context: Context, font: AppFont, weight: Int?): FontFamily {
    if (font == AppFont.SYSTEM) return FontFamily.Default
    val target = File(context.filesDir, "fonts/${font.fileName}")
    if (!target.exists()) {
        val copied = runCatching {
            target.parentFile?.mkdirs()
            context.assets.open("fonts/${font.fileName}").use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
        }.isSuccess
        if (!copied) return FontFamily.Default
    }
    return runCatching {
        FontFamily(Font(file = target, weight = FontWeight(weight ?: 500)))
    }.getOrDefault(FontFamily.Default)
}

/** Typography with the exact sizes from the design spec; [fontScale] multiplies every size. */
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
