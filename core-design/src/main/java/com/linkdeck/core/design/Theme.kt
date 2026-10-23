package com.linkdeck.core.design

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

enum class ThemeMode { SYSTEM, LIGHT, DARK }

val LocalTints = compositionLocalOf { SemanticTintsPalette.light(Color(0xFFD6E7F9), Color(0xFF0B3E6F)) }
val LocalCornerScale = compositionLocalOf { 1f }
val LocalIconStroke = compositionLocalOf { IconStroke.REGULAR }

object LinkDeckTheme {
    val tints: SemanticTints @Composable get() = LocalTints.current
    val spacing: Spacing get() = Spacing
    val cornerScale: Float @Composable get() = LocalCornerScale.current
    val iconStroke: IconStroke @Composable get() = LocalIconStroke.current
}

private fun AccentPalette.toLightScheme() = lightColorScheme(
    primary = lightPrimary,
    onPrimary = lightOnPrimary,
    primaryContainer = lightPrimaryContainer,
    onPrimaryContainer = lightOnPrimaryContainer,
    secondary = lightSecondary,
    background = NeutralColors.LightBackground,
    surface = NeutralColors.LightSurface,
    surfaceVariant = NeutralColors.LightSurfaceVariant,
    onBackground = NeutralColors.LightOnSurface,
    onSurface = NeutralColors.LightOnSurface,
    onSurfaceVariant = NeutralColors.LightOnSurfaceVariant,
    outline = NeutralColors.LightOutline,
)

private fun AccentPalette.toDarkScheme() = darkColorScheme(
    primary = darkPrimary,
    onPrimary = darkOnPrimary,
    primaryContainer = darkPrimaryContainer,
    onPrimaryContainer = darkOnPrimaryContainer,
    secondary = darkSecondary,
    background = NeutralColors.DarkBackground,
    surface = NeutralColors.DarkSurface,
    surfaceVariant = NeutralColors.DarkSurfaceVariant,
    onBackground = NeutralColors.DarkOnSurface,
    onSurface = NeutralColors.DarkOnSurface,
    onSurfaceVariant = NeutralColors.DarkOnSurfaceVariant,
    outline = NeutralColors.DarkOutline,
)

@Composable
fun LinkDeckTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentTheme = AccentTheme.DEFAULT,
    fontFamily: FontFamily = FontFamily.Default,
    fontScale: Float = 1f,
    fontWeight: Int? = null,
    cornerScale: Float = 1f,
    iconStroke: IconStroke = IconStroke.REGULAR,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current

    val colorScheme = when {
        accent == AccentTheme.DYNAMIC && Build.VERSION.SDK_INT >= 31 ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> AccentPalettes.forAccent(accent).let { if (darkTheme) it.toDarkScheme() else it.toLightScheme() }
    }

    val tints = if (darkTheme) {
        SemanticTintsPalette.dark(colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
    } else {
        SemanticTintsPalette.light(colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
    }

    CompositionLocalProvider(
        LocalTints provides tints,
        LocalCornerScale provides cornerScale,
        LocalIconStroke provides iconStroke,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = linkDeckTypography(fontFamily, fontScale, fontWeight),
            content = content,
        )
    }
}
