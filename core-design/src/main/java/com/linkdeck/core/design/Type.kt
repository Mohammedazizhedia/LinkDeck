package com.linkdeck.core.design

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
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

@Suppress("DEPRECATION")
fun loadAppTypeface(context: Context, font: AppFont, weight: Int?): Typeface? {
    if (font == AppFont.SYSTEM) return null
    val base: Typeface = runCatching {
        if (Build.VERSION.SDK_INT >= 26) {
            Typeface.Builder(context.assets, "fonts/${font.fileName}").build()
        } else {
            Typeface.createFromAsset(context.assets, "fonts/${font.fileName}")
        }
    }.getOrNull() ?: return null
    return if (weight != null && Build.VERSION.SDK_INT >= 28) {
        Typeface.create(base, weight, false)
    } else base
}

@Suppress("DEPRECATION")
fun linkDeckTypography(typeface: Typeface?, fontScale: Float, weight: Int?): Typography {
    val platformStyle = typeface?.let { PlatformTextStyle(includeFontPadding = null, typeface = it) }
    val w = FontWeight(weight ?: 500)
    fun s(sp: Int) = (sp * fontScale).sp
    fun style(sizeSp: Int) = TextStyle(
        fontSize = s(sizeSp),
        fontWeight = w,
        platformStyle = platformStyle,
    )
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
