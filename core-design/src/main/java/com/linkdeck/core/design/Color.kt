package com.linkdeck.core.design

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Accent palettes. Each accent carries its light AND dark primary set so the
// theme can be resolved without branching in screens (no hardcoded colors).
// Order per spec: light primary / onPrimary / primaryContainer / onPrimaryContainer,
// light secondary, then the same five for dark.
// ---------------------------------------------------------------------------
data class AccentPalette(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val lightSecondary: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val darkSecondary: Color,
)

enum class AccentTheme { DEFAULT, TEAL, INDIGO, AMBER, ROSE, DYNAMIC }

object AccentPalettes {
    val DEFAULT = AccentPalette(
        lightPrimary = Color(0xFF185FA5), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFD6E7F9), lightOnPrimaryContainer = Color(0xFF0B3E6F),
        lightSecondary = Color(0xFF2F6DA3),
        darkPrimary = Color(0xFFB5D4F4), darkOnPrimary = Color(0xFF0C2C4E),
        darkPrimaryContainer = Color(0xFF14395C), darkOnPrimaryContainer = Color(0xFFCFE3F8),
        darkSecondary = Color(0xFF8FB8E0),
    )
    val TEAL = AccentPalette(
        lightPrimary = Color(0xFF0F766E), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFF99F6E4), lightOnPrimaryContainer = Color(0xFF134E4A),
        lightSecondary = Color(0xFF0F766E),
        darkPrimary = Color(0xFF5EEAD4), darkOnPrimary = Color(0xFF06302B),
        darkPrimaryContainer = Color(0xFF0B3B36), darkOnPrimaryContainer = Color(0xFF8CE8D4),
        darkSecondary = Color(0xFF5EEAD4),
    )
    val INDIGO = AccentPalette(
        lightPrimary = Color(0xFF4F46E5), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFE0E7FF), lightOnPrimaryContainer = Color(0xFF3730A3),
        lightSecondary = Color(0xFF4F46E5),
        darkPrimary = Color(0xFFA5B4FC), darkOnPrimary = Color(0xFF1E1B4B),
        darkPrimaryContainer = Color(0xFF2A2753), darkOnPrimaryContainer = Color(0xFFC7D2FE),
        darkSecondary = Color(0xFFA5B4FC),
    )
    val AMBER = AccentPalette(
        lightPrimary = Color(0xFFB45309), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFFDE68A), lightOnPrimaryContainer = Color(0xFF78350F),
        lightSecondary = Color(0xFFB45309),
        darkPrimary = Color(0xFFFBBF24), darkOnPrimary = Color(0xFF4A2B00),
        darkPrimaryContainer = Color(0xFF4A3410), darkOnPrimaryContainer = Color(0xFFFCD34D),
        darkSecondary = Color(0xFFFBBF24),
    )
    val ROSE = AccentPalette(
        lightPrimary = Color(0xFFBE123C), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFFECDD3), lightOnPrimaryContainer = Color(0xFF881337),
        lightSecondary = Color(0xFFBE123C),
        darkPrimary = Color(0xFFFDA4AF), darkOnPrimary = Color(0xFF590D22),
        darkPrimaryContainer = Color(0xFF571C28), darkOnPrimaryContainer = Color(0xFFFDA4AF),
        darkSecondary = Color(0xFFFDA4AF),
    )

    fun forAccent(accent: AccentTheme): AccentPalette = when (accent) {
        AccentTheme.DEFAULT, AccentTheme.DYNAMIC -> DEFAULT // DYNAMIC resolves via Material You
        AccentTheme.TEAL -> TEAL
        AccentTheme.INDIGO -> INDIGO
        AccentTheme.AMBER -> AMBER
        AccentTheme.ROSE -> ROSE
    }
}

// Neutral surfaces (shared by every accent), light & dark, per spec.
object NeutralColors {
    val LightBackground = Color(0xFFF8F7F3)
    val LightSurface = Color(0xFFFDFDFB)
    val LightSurfaceVariant = Color(0xFFEFEDE6)
    val LightOnSurface = Color(0xFF1B1C1A)
    val LightOnSurfaceVariant = Color(0xFF44463F)
    val LightOutline = Color(0xFF75776C)

    val DarkBackground = Color(0xFF0F100E)
    val DarkSurface = Color(0xFF131412)
    val DarkSurfaceVariant = Color(0xFF2C2C2A)
    val DarkOnSurface = Color(0xFFE4E3DD)
    val DarkOnSurfaceVariant = Color(0xFFC6C7BF)
    val DarkOutline = Color(0xFF8F9186)
}

// Semantic tints (bg/fg) for status chips and category tiles. The accent tint
// follows the ACTIVE scheme's primaryContainer / onPrimaryContainer, the rest
// are fixed. Every pair keeps contrast >= 4.5:1 per spec.
data class SemanticTints(
    val accentBg: Color, val accentFg: Color,
    val successBg: Color, val successFg: Color,
    val warningBg: Color, val warningFg: Color,
    val proBg: Color, val proFg: Color,
    val dangerBg: Color, val dangerFg: Color,
)

object SemanticTintsPalette {
    fun light(schemeAccentBg: Color, schemeAccentFg: Color) = SemanticTints(
        accentBg = schemeAccentBg, accentFg = schemeAccentFg,
        successBg = Color(0xFFEAF3DE), successFg = Color(0xFF3B6D11),
        warningBg = Color(0xFFFAEEDA), warningFg = Color(0xFF854F0B),
        proBg = Color(0xFFEEEDFE), proFg = Color(0xFF534AB7),
        dangerBg = Color(0xFFFCEBEB), dangerFg = Color(0xFFA32D2D),
    )

    fun dark(schemeAccentBg: Color, schemeAccentFg: Color) = SemanticTints(
        accentBg = schemeAccentBg, accentFg = schemeAccentFg,
        successBg = Color(0xFF27500A), successFg = Color(0xFFC0DD97),
        warningBg = Color(0xFF633806), warningFg = Color(0xFFFAC775),
        proBg = Color(0xFF3C3489), proFg = Color(0xFFCECBF6),
        dangerBg = Color(0xFF791F1F), dangerFg = Color(0xFFF7C1C1),
    )
}
