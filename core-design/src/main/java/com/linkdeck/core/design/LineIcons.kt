package com.linkdeck.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom line-art icon set on a 64x64 grid, stroke 2, round caps and joins.
 * Each glyph draws what its feature contains — no generic filled shapes.
 */
enum class LineIcon(val data: String) {
    // Phone + tablet linked by a line (devices / launcher motif)
    DEVICES(
        "M10 12 h34 a4 4 0 0 1 4 4 v22 a4 4 0 0 1 -4 4 h-34 a4 4 0 0 1 -4 -4 v-22 a4 4 0 0 1 4 -4 z" +
            "M22 46 h2 M32 46 h2 M44 28 h6 a4 4 0 0 1 4 4 v18 a4 4 0 0 1 -4 4 h-12 a4 4 0 0 1 -4 -4 v-10" +
            "M56 36 h6 M56 44 h6 M8 32 h4"
    ),
    // Screen with a play arrow (mirroring)
    MIRROR(
        "M10 12 h44 a2 2 0 0 1 2 2 v26 a2 2 0 0 1 -2 2 h-44 a2 2 0 0 1 -2 -2 v-26 a2 2 0 0 1 2 -2 z" +
            "M27 20 l14 8 -14 8 z M32 42 v6 M22 52 h20"
    ),
    // Folder (files)
    FILES("M8 20 a4 4 0 0 1 4 -4 h14 l4 6 h26 a4 4 0 0 1 4 4 v22 a4 4 0 0 1 -4 4 h-44 a4 4 0 0 1 -4 -4 z"),
    // Bell (notifications)
    NOTIFICATIONS(
        "M32 10 c9 0 13 6 13 14 v9 l5 8 a2 2 0 0 1 -2 3 h-32 a2 2 0 0 1 -2 -3 l5 -8 v-9 c0 -8 4 -14 13 -14 z" +
            "M26 47 a6 6 0 0 0 12 0"
    ),
    // Chat bubble (messages)
    MESSAGES("M12 12 h40 a4 4 0 0 1 4 4 v20 a4 4 0 0 1 -4 4 h-24 l-10 8 v-8 h-6 a4 4 0 0 1 -4 -4 v-20 a4 4 0 0 1 4 -4 z M20 22 h24 M20 30 h16"),
    // Handset (calls)
    CALLS("M18 8 c5 0 9 7 9 11 0 4 -6 6 -6 11 0 9 13 22 22 22 5 0 7 -6 11 -6 4 0 11 4 11 9 0 9 -10 13 -18 13 -19 0 -40 -21 -40 -40 0 -8 4 -20 11 -20 z"),
    // App grid (app launcher)
    APPS("M12 12 h16 a2 2 0 0 1 2 2 v16 a2 2 0 0 1 -2 2 h-16 a2 2 0 0 1 -2 -2 v-16 a2 2 0 0 1 2 -2 z" +
        "M36 12 h16 a2 2 0 0 1 2 2 v16 a2 2 0 0 1 -2 2 h-16 a2 2 0 0 1 -2 -2 v-16 a2 2 0 0 1 2 -2 z" +
        "M12 36 h16 a2 2 0 0 1 2 2 v16 a2 2 0 0 1 -2 2 h-16 a2 2 0 0 1 -2 -2 v-16 a2 2 0 0 1 2 -2 z" +
        "M36 36 h16 a2 2 0 0 1 2 2 v16 a2 2 0 0 1 -2 2 h-16 a2 2 0 0 1 -2 -2 v-16 a2 2 0 0 1 2 -2 z"),
    // Gear (settings)
    SETTINGS(
        "M32 26 a6 6 0 1 0 0.01 0 z" +
            "M32 12 v6 M32 46 v6 M16 22 l4 4 M44 38 l4 4 M48 22 l-4 4 M20 38 l-4 4 M14 32 h6 M44 32 h6"
    ),
    // Plug (connection)
    CONNECTION("M24 10 v12 M40 10 v12 M24 22 h16 v8 a8 8 0 0 1 -16 0 z M32 38 v8 M22 50 h20 l-3 -6 h-14 z"),
    // QR code
    QR("M12 12 h14 v14 h-14 z M18 18 h2 M38 12 h14 v14 h-14 z M44 18 h2 M12 38 h14 v14 h-14 z M18 44 h2" +
        "M38 38 h4 v4 h-4 z M46 38 h4 v4 h-4 z M38 46 h4 v4 h-4 z M46 46 h4 v4 h-4 z"),
    // Pause
    PAUSE("M22 14 v36 M42 14 v36"),
    // Power / disconnect
    POWER("M32 12 v16 M22 20 a16 16 0 1 0 20 0"),
    // Home
    HOME("M10 30 L32 12 L54 30 M16 28 v22 h12 v-12 h8 v12 h12 v-22"),
    // Search
    SEARCH("M28 14 a14 14 0 1 0 0.01 0 z M39 39 l11 11"),
    // Info
    INFO("M32 12 a20 20 0 1 0 0.01 0 z M32 26 v14 M32 22 v0.01"),
    // Check
    CHECK("M12 34 l14 14 L54 18"),
}

/**
 * Draws a [LineIcon] with the active stroke setting (thin / regular / bold).
 */
@Composable
fun LineIconView(
    icon: LineIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val strokeSetting = LocalIconStroke.current
    val pathNodes: List<PathNode> = rememberIconNodes(icon)
    Canvas(modifier.size(size)) {
        val path = Path()
        pathNodes.forEach { it.addPath(path) }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeSetting.width * (this.size.minDimension / 64f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun rememberIconNodes(icon: LineIcon): List<PathNode> =
    androidx.compose.runtime.remember(icon) { addPathNodes(icon.data) }
