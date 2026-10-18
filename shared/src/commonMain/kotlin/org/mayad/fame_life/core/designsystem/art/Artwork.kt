package org.mayad.fame_life.core.designsystem.art

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.layer.drawLayer
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Immutable
data class ArtPalette(val deep: Color, val base: Color, val a: Color, val b: Color, val light: Color)

/**
 * Eight palettes tuned to the brand: every one sits on the same midnight base, so mixed feeds stay coherent
 * while still giving each creator a recognisable color.
 */
val ArtPalettes = listOf(
    ArtPalette(Color(0xFF070A18), Color(0xFF151D3F), Color(0xFF4C7DFF), Color(0xFF945CFF), Color(0xFFBFD0FF)),
    ArtPalette(Color(0xFF0E0712), Color(0xFF2A1230), Color(0xFFFF4D6D), Color(0xFFFF8A5B), Color(0xFFFFD3B8)),
    ArtPalette(Color(0xFF06111A), Color(0xFF0F2A3A), Color(0xFF55E5F2), Color(0xFF4C7DFF), Color(0xFFC8FAFF)),
    ArtPalette(Color(0xFF0D0716), Color(0xFF261244), Color(0xFF945CFF), Color(0xFFFF5FA8), Color(0xFFE8D1FF)),
    ArtPalette(Color(0xFF05110F), Color(0xFF0F2E2A), Color(0xFF52D6A0), Color(0xFF55E5F2), Color(0xFFCFFFEA)),
    ArtPalette(Color(0xFF120806), Color(0xFF341710), Color(0xFFFFC66D), Color(0xFFFF6A3D), Color(0xFFFFEBC2)),
    ArtPalette(Color(0xFF080B14), Color(0xFF1A2440), Color(0xFF8FB2FF), Color(0xFF55E5F2), Color(0xFFF2F6FF)),
    ArtPalette(Color(0xFF0F0618), Color(0xFF2E0F3F), Color(0xFFC06BFF), Color(0xFF4C7DFF), Color(0xFFF0DBFF)),
)

/**
 * Procedural cover art. Each [ArtSpec] always renders the same image, cached per size, so feeds look rich and
 * work fully offline. When licensed photography is added it can replace this per item without layout changes.
 */
@Composable
fun Artwork(spec: ArtSpec, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Box(
        modifier.drawWithCache {
            val palette = ArtPalettes[spec.palette.mod(ArtPalettes.size)]
            // Recorded once per size: scrolling a feed replays the layer instead of re-running every shape.
            val layer = obtainGraphicsLayer()
            layer.record {
                val random = Random(spec.seed * 7919 + spec.motif.ordinal)
                drawRect(Brush.verticalGradient(listOf(palette.base, palette.deep)))
                when (spec.motif) {
                    ArtMotif.AURORA -> aurora(palette, random)
                    ArtMotif.STAGE -> stage(palette, random)
                    ArtMotif.HORIZON -> horizon(palette, random)
                    ArtMotif.WAVES -> waves(palette, random)
                    ArtMotif.BLOOM -> bloom(palette, random)
                    ArtMotif.PRISM -> prism(palette, random)
                    ArtMotif.SKYLINE -> skyline(palette, random)
                    ArtMotif.PORTRAIT -> portrait(palette, random)
                }
                // Vignette pulls focus to the middle and keeps overlaid text readable.
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.Transparent, palette.deep.copy(alpha = 0.55f)),
                        center = center,
                        radius = size.maxDimension * 0.75f,
                    ),
                )
            }
            onDrawBehind { drawLayer(layer) }
        },
    ) { content() }
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float, alpha: Float = 0.75f) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), center, radius), radius, center)
}

private fun DrawScope.aurora(p: ArtPalette, r: Random) {
    val w = size.width
    val h = size.height
    blob(p.a, Offset(w * r.nextFloat(), h * (0.15f + r.nextFloat() * 0.3f)), w * 0.9f)
    blob(p.b, Offset(w * r.nextFloat(), h * (0.45f + r.nextFloat() * 0.3f)), w * 0.8f, 0.6f)
    blob(p.light, Offset(w * 0.5f, h * 0.35f), w * 0.35f, 0.25f)
    repeat(2) { band ->
        val ribbon = Path().apply {
            val baseY = h * (0.45f + band * 0.18f)
            moveTo(0f, baseY)
            for (x in 0..20) {
                val fx = w * x / 20f
                lineTo(fx, baseY + sin(x / 20f * PI * 2 + band + r.nextFloat()).toFloat() * h * 0.06f)
            }
            lineTo(w, baseY + h * 0.14f)
            lineTo(0f, baseY + h * 0.14f)
            close()
        }
        drawPath(ribbon, Brush.verticalGradient(listOf((if (band == 0) p.a else p.b).copy(alpha = 0.35f), Color.Transparent)))
    }
}

private fun DrawScope.stage(p: ArtPalette, r: Random) {
    val w = size.width
    val h = size.height
    listOf(0.2f, 0.5f, 0.8f).forEachIndexed { i, x ->
        val beam = Path().apply {
            moveTo(w * x - w * 0.02f, 0f)
            lineTo(w * x + w * 0.02f, 0f)
            lineTo(w * (x + (r.nextFloat() - 0.5f) * 0.5f) + w * 0.22f, h * 0.85f)
            lineTo(w * (x + (r.nextFloat() - 0.5f) * 0.5f) - w * 0.22f, h * 0.85f)
            close()
        }
        drawPath(beam, Brush.verticalGradient(listOf((if (i == 1) p.light else p.a).copy(alpha = 0.55f), Color.Transparent)))
    }
    blob(p.b, Offset(w * 0.5f, h * 0.82f), w * 0.7f, 0.7f)
    // Crowd silhouettes with a rim of stage light.
    for (row in 0..1) {
        var x = -w * 0.05f + r.nextFloat() * w * 0.05f
        while (x < w * 1.05f) {
            val headR = w * (0.035f + r.nextFloat() * 0.02f) * (1 + row * 0.25f)
            val y = h * (0.88f + row * 0.07f)
            drawCircle(p.deep, headR, Offset(x, y - headR * 1.6f))
            drawOval(p.deep, Offset(x - headR * 1.7f, y - headR * 0.4f), Size(headR * 3.4f, headR * 4f))
            drawCircle(p.light.copy(alpha = 0.25f), headR, Offset(x, y - headR * 1.6f), style = Stroke(headR * 0.18f))
            x += headR * (2.6f + r.nextFloat() * 1.4f)
        }
    }
}

private fun DrawScope.horizon(p: ArtPalette, r: Random) {
    val w = size.width
    val h = size.height
    val horizonY = h * 0.58f
    val sunCenter = Offset(w * (0.35f + r.nextFloat() * 0.3f), horizonY)
    val sunR = w * 0.3f
    blob(p.b, sunCenter, sunR * 2.2f, 0.5f)
    drawCircle(Brush.verticalGradient(listOf(p.light, p.a, p.b), sunCenter.y - sunR, sunCenter.y), sunR, sunCenter)
    for (i in 1..5) {
        val y = sunCenter.y - sunR * 0.5f + i * sunR * 0.1f
        drawRect(p.base, Offset(sunCenter.x - sunR, y), Size(sunR * 2, sunR * 0.025f * i))
    }
    drawRect(Brush.verticalGradient(listOf(p.deep, p.base), horizonY, h), Offset(0f, horizonY), Size(w, h - horizonY))
    val grid = p.a.copy(alpha = 0.55f)
    for (i in -8..8) {
        drawLine(grid, Offset(w / 2 + i * w * 0.06f, horizonY), Offset(w / 2 + i * w * 0.32f, h), strokeWidth = 1.5f)
    }
    var y = horizonY
    var step = h * 0.015f
    while (y < h) {
        drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
        y += step
        step *= 1.45f
    }
}

private fun DrawScope.waves(p: ArtPalette, r: Random) {
    val w = size.width
    val h = size.height
    val c = Offset(w * 0.5f, h * (0.55f + r.nextFloat() * 0.15f))
    blob(p.a, c, w * 0.8f, 0.55f)
    for (i in 1..9) {
        drawCircle(
            (if (i % 2 == 0) p.a else p.b).copy(alpha = 0.5f - i * 0.045f),
            radius = w * 0.08f * i,
            center = c,
            style = Stroke(width = w * 0.012f),
        )
    }
    val bars = 17
    val barW = w / (bars * 1.8f)
    for (i in 0 until bars) {
        val amp = (0.08f + r.nextFloat() * 0.22f) * (1f - kotlin.math.abs(i - bars / 2) / (bars / 1.6f))
        val bh = h * amp.coerceAtLeast(0.03f)
        drawRoundRect(
            Brush.verticalGradient(listOf(p.light, p.a), c.y - bh, c.y + bh),
            topLeft = Offset(w * 0.1f + i * barW * 1.8f * 0.88f, c.y - bh),
            size = Size(barW, bh * 2),
            cornerRadius = CornerRadius(barW / 2),
        )
    }
}

private fun DrawScope.bloom(p: ArtPalette, r: Random) {
    val w = size.width
    val h = size.height
    val c = Offset(w * (0.4f + r.nextFloat() * 0.2f), h * (0.4f + r.nextFloat() * 0.2f))
    blob(p.b, c, w * 0.9f, 0.5f)
    val petals = 9
    for (i in 0 until petals) {
        rotate(degrees = i * 360f / petals + r.nextFloat() * 6f, pivot = c) {
            drawOval(
                Brush.verticalGradient(listOf(p.light.copy(alpha = 0.85f), p.a.copy(alpha = 0.6f), Color.Transparent), c.y - w * 0.42f, c.y),
                topLeft = Offset(c.x - w * 0.08f, c.y - w * 0.42f),
                size = Size(w * 0.16f, w * 0.42f),
            )
        }
    }
    drawCircle(Brush.radialGradient(listOf(Color.White, p.light, p.a), c, w * 0.08f), w * 0.07f, c)
}

private fun DrawScope.prism(p: ArtPalette, r: Random) {
    val w = size.width
    val h = size.height
    blob(p.a, Offset(w * 0.2f, h * 0.2f), w * 0.8f, 0.45f)
    blob(p.b, Offset(w * 0.85f, h * 0.75f), w * 0.8f, 0.45f)
    repeat(6) { i ->
        val shard = Path().apply {
            val cx = w * r.nextFloat()
            val cy = h * r.nextFloat()
            val s = w * (0.25f + r.nextFloat() * 0.35f)
            moveTo(cx, cy - s * 0.6f)
            lineTo(cx + s * 0.5f, cy + s * 0.1f)
            lineTo(cx - s * 0.1f, cy + s * 0.6f)
            lineTo(cx - s * 0.45f, cy - s * 0.05f)
            close()
        }
        val color = listOf(p.a, p.b, p.light)[i % 3]
        drawPath(shard, Brush.linearGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.05f))))
        drawPath(shard, Color.White.copy(alpha = 0.18f), style = Stroke(1.5f))
    }
}

private fun DrawScope.skyline(p: ArtPalette, r: Random) {
    val w = size.width
    val h = size.height
    blob(p.b, Offset(w * 0.5f, h * 0.7f), w, 0.45f)
    val moon = Offset(w * (0.2f + r.nextFloat() * 0.6f), h * 0.2f)
    blob(p.light, moon, w * 0.25f, 0.35f)
    drawCircle(Brush.radialGradient(listOf(Color.White, p.light), moon, w * 0.08f), w * 0.07f, moon)
    repeat(28) { drawCircle(Color.White.copy(alpha = 0.3f + r.nextFloat() * 0.5f), 1.2f + r.nextFloat() * 1.2f, Offset(w * r.nextFloat(), h * r.nextFloat() * 0.45f)) }
    var x = 0f
    while (x < w) {
        val bw = w * (0.08f + r.nextFloat() * 0.1f)
        val bh = h * (0.18f + r.nextFloat() * 0.35f)
        drawRect(p.deep, Offset(x, h - bh), Size(bw - 2f, bh))
        val win = p.a.copy(alpha = 0.8f)
        var wy = h - bh + 6f
        while (wy < h - 6f) {
            var wx = x + 4f
            while (wx < x + bw - 8f) {
                if (r.nextFloat() < 0.35f) drawRect(if (r.nextFloat() < 0.3f) p.light else win, Offset(wx, wy), Size(3f, 4f))
                wx += 8f
            }
            wy += 10f
        }
        x += bw
    }
}

/** An abstract figure in rim light, standing in for creator photography. */
private fun DrawScope.portrait(p: ArtPalette, r: Random) {
    val w = size.width
    val h = size.height
    blob(p.a, Offset(w * (0.2f + r.nextFloat() * 0.2f), h * 0.25f), w * 0.9f, 0.7f)
    blob(p.b, Offset(w * (0.7f + r.nextFloat() * 0.2f), h * 0.7f), w * 0.8f, 0.6f)
    val headC = Offset(w * 0.5f, h * 0.42f)
    val headR = w * 0.17f
    val body = Path().apply {
        moveTo(w * 0.12f, h * 1.02f)
        cubicTo(w * 0.14f, h * 0.72f, w * 0.3f, h * 0.62f, w * 0.5f, h * 0.62f)
        cubicTo(w * 0.7f, h * 0.62f, w * 0.86f, h * 0.72f, w * 0.88f, h * 1.02f)
        close()
    }
    drawPath(body, Brush.verticalGradient(listOf(p.base, p.deep)))
    drawPath(body, Brush.horizontalGradient(listOf(p.light.copy(alpha = 0.55f), Color.Transparent, p.a.copy(alpha = 0.35f))), style = Stroke(w * 0.012f))
    drawCircle(Brush.radialGradient(listOf(p.base, p.deep), Offset(headC.x - headR * 0.3f, headC.y - headR * 0.3f), headR * 1.4f), headR, headC)
    drawCircle(
        Brush.sweepGradient(listOf(p.light.copy(alpha = 0.7f), Color.Transparent, Color.Transparent, p.a.copy(alpha = 0.6f), p.light.copy(alpha = 0.7f)), headC),
        headR,
        headC,
        style = Stroke(w * 0.014f),
    )
}
