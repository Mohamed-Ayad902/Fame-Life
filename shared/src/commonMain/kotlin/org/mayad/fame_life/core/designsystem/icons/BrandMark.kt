package org.mayad.fame_life.core.designsystem.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.mayad.fame_life.app.AppConfig

/** Blue into violet: the two identity colors on one tile. */
val BrandGloss = Gloss(Color(0xFFB4C6FF), Color(0xFF6A6CFF), Color(0xFF4B23A6), Color(0xFF7B6BFF))

/**
 * The app mark: a glossy tile carrying a heartbeat line. [lineProgress] draws the line from left to right (the
 * splash animates it; everywhere else it is 1). The Android splash icon (`ic_splash_mark.xml`) uses the same path.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, lineProgress: Float = 1f) {
    Canvas(modifier.semantics { contentDescription = AppConfig.APP_NAME }) {
        drawGlossTile(BrandGloss)
        val w = size.width
        val h = size.height * 0.94f
        // Points in a 100 x 100 box, matching the vector drawable.
        val points = listOf(22f to 52f, 38f to 52f, 45f to 34f, 55f to 70f, 62f to 44f, 67f to 52f, 78f to 52f)
        val line = Path().apply {
            points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(w * x / 100f, h * y / 100f) else lineTo(w * x / 100f, h * y / 100f) }
        }
        val drawn = if (lineProgress >= 1f) {
            line
        } else {
            val measure = PathMeasure().apply { setPath(line, false) }
            Path().also { measure.getSegment(0f, measure.length * lineProgress.coerceIn(0f, 1f), it, true) }
        }
        val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // A soft drop under the line, like the glyph shadow on GlossyIcon.
        translate(top = w * 0.015f) { drawPath(drawn, Color.Black.copy(alpha = 0.22f), style = stroke) }
        drawPath(drawn, Color.White, style = stroke)
    }
}
