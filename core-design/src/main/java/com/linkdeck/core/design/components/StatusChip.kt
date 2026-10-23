package com.linkdeck.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.linkdeck.core.design.LinkDeckTheme

/** Pill-shaped status chip with semantic tint background/foreground and a dot. */
@Composable
fun StatusChip(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = false,
    dotColor: Color = foreground,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        if (showDot) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = foreground)
    }
}

/** Convenience semantic variants using the active theme tints. */
object StatusChips {
    @Composable fun Accent(text: String, modifier: Modifier = Modifier, dot: Boolean = false) =
        StatusChip(text, LinkDeckTheme.tints.accentBg, LinkDeckTheme.tints.accentFg, modifier, dot, LinkDeckTheme.tints.accentFg)

    @Composable fun Success(text: String, modifier: Modifier = Modifier) =
        StatusChip(text, LinkDeckTheme.tints.successBg, LinkDeckTheme.tints.successFg, modifier)

    @Composable fun Warning(text: String, modifier: Modifier = Modifier) =
        StatusChip(text, LinkDeckTheme.tints.warningBg, LinkDeckTheme.tints.warningFg, modifier)

    @Composable fun Pro(text: String, modifier: Modifier = Modifier) =
        StatusChip(text, LinkDeckTheme.tints.proBg, LinkDeckTheme.tints.proFg, modifier)

    @Composable fun Danger(text: String, modifier: Modifier = Modifier, dot: Boolean = false) =
        StatusChip(text, LinkDeckTheme.tints.dangerBg, LinkDeckTheme.tints.dangerFg, modifier, dot, LinkDeckTheme.tints.dangerFg)
}
