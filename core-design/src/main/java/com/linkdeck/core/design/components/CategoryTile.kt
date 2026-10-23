package com.linkdeck.core.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.linkdeck.core.design.LineIcon as LineIconGlyph
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme

/**
 * Category tile per spec: 18dp rounded corners, 12dp padding, tinted background,
 * line icon in the tint foreground color, short label and an optional counter.
 */
@Composable
fun CategoryTile(
    icon: LineIconGlyph,
    label: String,
    count: Int?,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val corner = (18 * LinkDeckTheme.cornerScale).dp
    Surface(
        color = background,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(corner),
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp)) {
            LineIconView(icon = icon, tint = foreground, size = 28.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = foreground.copy(alpha = 0.8f),
                )
            }
        }
    }
}
