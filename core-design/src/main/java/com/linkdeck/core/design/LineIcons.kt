package com.linkdeck.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Line-art icon set on a 64x64 grid; data uses absolute M/L/H/V/C/Z only. */
enum class LineIcon(val data: String) {
    DEVICES("M6 10 H46 V40 H6 Z M40 30 H58 V56 H40 Z M26 40 V46 H40 V30 M46 52 H52"),
    MIRROR("M8 10 H56 V40 H8 Z M26 18 L42 26 L26 34 Z M32 40 V48 M22 54 H42"),
    FILES("M8 20 H22 L28 28 H56 V48 H8 Z"),
    NOTIFICATIONS("M32 10 C42 10 46 18 46 26 V34 L52 44 H12 L18 34 V26 C18 18 22 10 32 10 Z M26 46 C26 52 38 52 38 46"),
    MESSAGES("M10 12 H54 V36 H28 L16 46 V36 H10 Z"),
    CALLS("M14 8 C22 8 28 18 28 24 C28 28 22 30 22 34 C22 44 32 52 42 52 C46 52 48 46 52 46 C58 46 62 52 62 58 C62 62 54 64 46 64 C28 64 8 46 8 26 C8 16 10 8 14 8 Z"),
    APPS("M12 12 H30 V30 H12 Z M34 12 H52 V30 H34 Z M12 34 H30 V52 H12 Z M34 34 H52 V52 H34 Z"),
    SETTINGS("M32 24 C36.4 24 40 27.6 40 32 C40 36.4 36.4 40 32 40 C27.6 40 24 36.4 24 32 C24 27.6 27.6 24 32 24 Z M32 10 V18 M32 46 V54 M10 32 H18 M46 32 H54 M16 16 L22 22 M42 42 L48 48 M48 16 L42 22 M22 42 L16 48"),
    CONNECTION("M22 10 V20 M40 10 V20 M22 20 H40 V28 C40 34 36 38 31 38 C26 38 22 34 22 28 Z M31 38 V48 M21 56 H41 L38 48 H24 Z"),
    QR("M12 12 H28 V28 H12 Z M18 18 H23 V23 H18 Z M36 12 H52 V28 H36 Z M42 18 H47 V23 H42 Z M12 36 H28 V52 H12 Z M18 42 H23 V47 H18 Z M36 36 H40 V40 H36 Z M44 36 H48 V40 H44 Z M36 44 H40 V48 H36 Z M44 44 H48 V48 H44 Z"),
    PAUSE("M22 14 V50 M42 14 V50"),
    POWER("M32 14 C41 14 50 22 50 32 C50 42 41 50 32 50 C22 50 14 42 14 32 C14 22 22 14 32 14 Z M32 8 V28"),
    HOME("M8 30 L32 10 L56 30 M14 26 V54 H26 V42 H38 V54 H50 V26"),
    SEARCH("M28 14 C36 14 42 20 42 28 C42 36 36 42 28 42 C20 42 14 36 14 28 C14 20 20 14 28 14 Z M38 38 L52 52"),
    INFO("M32 12 C43 12 52 21 52 32 C52 43 43 52 32 52 C21 52 12 43 12 32 C12 21 21 12 32 12 Z M32 26 V40 M32 20 V21"),
    CHECK("M12 34 L26 48 L52 20"),
}

@Composable
fun LineIconView(
    icon: LineIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val strokeSetting = LocalIconStroke.current
    val path = remember(icon) { parseLinePath(icon.data) }
    Canvas(modifier.size(size)) {
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

private fun parseLinePath(data: String): Path {
    val path = Path()
    val tokens = data.split(Regex("\\s+")).filter { it.isNotEmpty() }
    var i = 0
    var x = 0f
    var y = 0f
    var startX = 0f
    var startY = 0f
    fun num(): Float = tokens[++i].toFloat()
    while (i < tokens.size) {
        when (tokens[i]) {
            "M" -> { x = num(); y = num(); path.moveTo(x, y); startX = x; startY = y }
            "L" -> { x = num(); y = num(); path.lineTo(x, y) }
            "H" -> { x = num(); path.lineTo(x, y) }
            "V" -> { y = num(); path.lineTo(x, y) }
            "C" -> {
                val x1 = num(); val y1 = num(); val x2 = num(); val y2 = num()
                x = num(); y = num()
                path.cubicTo(x1, y1, x2, y2, x, y)
            }
            "Z" -> { path.close(); x = startX; y = startY }
        }
        i++
    }
    return path
}
