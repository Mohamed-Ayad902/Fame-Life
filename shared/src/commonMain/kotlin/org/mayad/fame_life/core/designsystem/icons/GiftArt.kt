package org.mayad.fame_life.core.designsystem.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rendered, dimensional artwork for each simulated gift: lit from the upper left with a specular highlight and
 * a colored glow, so gifts read as collectible objects rather than flat stickers. Drawn in code, so it scales
 * cleanly and needs no image assets.
 */
@Composable
fun GiftArt(giftId: String, modifier: Modifier = Modifier, size: Dp = 56.dp, name: String? = null) {
    Canvas(
        modifier
            .size(size)
            .semantics { if (name != null) contentDescription = name },
    ) {
        when (giftId) {
            "spark" -> drawSpark()
            "heart" -> drawHeart()
            "mic" -> drawMic()
            "comet" -> drawComet()
            "crown" -> drawCrown()
            "galaxy" -> drawGalaxy()
            else -> drawSpark()
        }
    }
}

private fun DrawScope.glow(color: Color, center: Offset = this.center, radius: Float = size.minDimension * 0.55f) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), center, radius), radius, center)
}

private fun DrawScope.highlight(center: Offset, w: Float, h: Float, alpha: Float = 0.7f) {
    drawOval(
        Brush.radialGradient(listOf(Color.White.copy(alpha = alpha), Color.Transparent), center, maxOf(w, h) / 2),
        topLeft = Offset(center.x - w / 2, center.y - h / 2),
        size = Size(w, h),
    )
}

private fun DrawScope.drawSpark() {
    val s = size.minDimension
    glow(Color(0xFF55E5F2))
    val path = Path().apply {
        val c = center
        val outer = s * 0.44f
        val inner = s * 0.12f
        for (i in 0 until 8) {
            val angle = -PI / 2 + i * PI / 4
            val r = if (i % 2 == 0) outer else inner
            val point = Offset(c.x + (r * cos(angle)).toFloat(), c.y + (r * sin(angle)).toFloat())
            if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
        close()
    }
    drawPath(path, Brush.radialGradient(listOf(Color.White, Color(0xFF9CF6FF), Color(0xFF3B7BFF)), center, s * 0.45f))
    highlight(Offset(center.x - s * 0.04f, center.y - s * 0.08f), s * 0.16f, s * 0.12f, 0.9f)
}

private fun heartPath(s: Float, ox: Float, oy: Float) = Path().apply {
    moveTo(ox + s * 0.5f, oy + s * 0.86f)
    cubicTo(ox + s * 0.1f, oy + s * 0.6f, ox + s * 0.02f, oy + s * 0.38f, ox + s * 0.12f, oy + s * 0.24f)
    cubicTo(ox + s * 0.24f, oy + s * 0.08f, ox + s * 0.44f, oy + s * 0.12f, ox + s * 0.5f, oy + s * 0.28f)
    cubicTo(ox + s * 0.56f, oy + s * 0.12f, ox + s * 0.76f, oy + s * 0.08f, ox + s * 0.88f, oy + s * 0.24f)
    cubicTo(ox + s * 0.98f, oy + s * 0.38f, ox + s * 0.9f, oy + s * 0.6f, ox + s * 0.5f, oy + s * 0.86f)
    close()
}

private fun DrawScope.drawHeart() {
    val s = size.minDimension
    glow(Color(0xFFFF4D6D))
    drawPath(
        heartPath(s, 0f, s * 0.02f),
        Brush.radialGradient(listOf(Color(0xFFFFB3C4), Color(0xFFFF4D6D), Color(0xFF9C1236)), Offset(s * 0.36f, s * 0.3f), s * 0.7f),
    )
    highlight(Offset(s * 0.3f, s * 0.3f), s * 0.2f, s * 0.13f)
}

private fun DrawScope.drawMic() {
    val s = size.minDimension
    glow(Color(0xFFFFC66D))
    // Handle
    drawRoundRect(
        Brush.horizontalGradient(listOf(Color(0xFF2B2F45), Color(0xFF6B7393), Color(0xFF1B1E2E)), s * 0.42f, s * 0.58f),
        topLeft = Offset(s * 0.42f, s * 0.5f),
        size = Size(s * 0.16f, s * 0.4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f),
    )
    // Gold collar
    drawRoundRect(
        Brush.horizontalGradient(listOf(Color(0xFFB86B12), Color(0xFFFFE29A), Color(0xFFB86B12)), s * 0.38f, s * 0.62f),
        topLeft = Offset(s * 0.38f, s * 0.48f),
        size = Size(s * 0.24f, s * 0.08f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.03f),
    )
    // Head
    val head = Offset(s * 0.5f, s * 0.3f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4C9), Color(0xFFFFC14D), Color(0xFF9A5A0E)), Offset(s * 0.42f, s * 0.22f), s * 0.3f), s * 0.22f, head)
    for (i in -2..2) {
        drawLine(Color(0x55704008), Offset(head.x + i * s * 0.07f, head.y - s * 0.18f), Offset(head.x + i * s * 0.07f, head.y + s * 0.18f), strokeWidth = s * 0.012f)
    }
    highlight(Offset(s * 0.42f, s * 0.2f), s * 0.16f, s * 0.1f)
}

private fun DrawScope.drawComet() {
    val s = size.minDimension
    glow(Color(0xFF945CFF), Offset(s * 0.62f, s * 0.4f))
    val tail = Path().apply {
        moveTo(s * 0.08f, s * 0.92f)
        lineTo(s * 0.52f, s * 0.3f)
        lineTo(s * 0.72f, s * 0.5f)
        close()
    }
    drawPath(tail, Brush.linearGradient(listOf(Color.Transparent, Color(0xAA945CFF), Color(0xFFB9FBFF)), Offset(s * 0.08f, s * 0.92f), Offset(s * 0.62f, s * 0.4f)))
    val core = Offset(s * 0.64f, s * 0.38f)
    drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFB9FBFF), Color(0xFF4C7DFF)), Offset(s * 0.6f, s * 0.33f), s * 0.24f), s * 0.18f, core)
    highlight(Offset(s * 0.58f, s * 0.31f), s * 0.12f, s * 0.08f, 0.95f)
}

private fun DrawScope.drawCrown() {
    val s = size.minDimension
    glow(Color(0xFFFFC66D))
    val crown = Path().apply {
        moveTo(s * 0.12f, s * 0.36f)
        lineTo(s * 0.32f, s * 0.56f)
        lineTo(s * 0.5f, s * 0.22f)
        lineTo(s * 0.68f, s * 0.56f)
        lineTo(s * 0.88f, s * 0.36f)
        lineTo(s * 0.8f, s * 0.8f)
        lineTo(s * 0.2f, s * 0.8f)
        close()
    }
    drawPath(crown, Brush.verticalGradient(listOf(Color(0xFFFFF0B8), Color(0xFFFFC14D), Color(0xFFB86B12)), s * 0.22f, s * 0.8f))
    drawPath(crown, Color(0x66FFFFFF), style = Stroke(s * 0.015f))
    listOf(
        Offset(s * 0.5f, s * 0.66f) to Color(0xFFFF4D6D),
        Offset(s * 0.32f, s * 0.68f) to Color(0xFF4C7DFF),
        Offset(s * 0.68f, s * 0.68f) to Color(0xFF55E5F2),
    ).forEach { (c, color) ->
        drawCircle(Brush.radialGradient(listOf(Color.White, color, color.copy(alpha = 0.8f)), Offset(c.x - s * 0.015f, c.y - s * 0.015f), s * 0.06f), s * 0.045f, c)
    }
    listOf(Offset(s * 0.12f, s * 0.34f), Offset(s * 0.5f, s * 0.2f), Offset(s * 0.88f, s * 0.34f)).forEach {
        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFFFC14D)), it, s * 0.05f), s * 0.04f, it)
    }
    highlight(Offset(s * 0.4f, s * 0.48f), s * 0.22f, s * 0.08f, 0.55f)
}

private fun DrawScope.drawGalaxy() {
    val s = size.minDimension
    glow(Color(0xFF945CFF), radius = s * 0.6f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE6FF), Color(0xFFB07CFF), Color(0xFF3A1A8C), Color(0xFF120A33)), Offset(s * 0.44f, s * 0.42f), s * 0.42f), s * 0.36f, center)
    rotate(-24f) {
        drawOval(
            Brush.horizontalGradient(listOf(Color.Transparent, Color(0xCC55E5F2), Color(0xFFFFFFFF), Color(0xCC945CFF), Color.Transparent)),
            topLeft = Offset(s * 0.02f, s * 0.44f),
            size = Size(s * 0.96f, s * 0.14f),
            style = Stroke(s * 0.025f),
        )
    }
    listOf(0.3f to 0.3f, 0.66f to 0.36f, 0.4f to 0.64f, 0.58f to 0.58f, 0.5f to 0.46f).forEachIndexed { i, (x, y) ->
        drawCircle(Color.White.copy(alpha = 0.6f + 0.08f * i), s * (0.012f + 0.004f * i), Offset(s * x, s * y))
    }
    highlight(Offset(s * 0.38f, s * 0.3f), s * 0.2f, s * 0.12f, 0.5f)
}
