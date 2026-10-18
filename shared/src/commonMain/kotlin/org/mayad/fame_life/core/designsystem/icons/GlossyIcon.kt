package org.mayad.fame_life.core.designsystem.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import org.mayad.fame_life.core.designsystem.theme.Pulse

/** Three tones of one hue plus a glow. Light comes from the top, the base sinks into shadow. */
@Immutable
data class Gloss(val top: Color, val mid: Color, val bottom: Color, val glow: Color) {
    companion object {
        val Blue = Gloss(Color(0xFF9DB8FF), Color(0xFF4C7DFF), Color(0xFF2338A8), Color(0xFF4C7DFF))
        val Violet = Gloss(Color(0xFFD2B5FF), Color(0xFF945CFF), Color(0xFF4B23A6), Color(0xFF945CFF))
        val Cyan = Gloss(Color(0xFFB9FBFF), Color(0xFF35C9DA), Color(0xFF136C86), Color(0xFF55E5F2))
        val Coral = Gloss(Color(0xFFFFC7AE), Color(0xFFFF7A59), Color(0xFFB2352A), Color(0xFFFF8A5B))
        val Rose = Gloss(Color(0xFFFFB3C4), Color(0xFFFF4D6D), Color(0xFFA0173A), Color(0xFFFF4D6D))
        val Gold = Gloss(Color(0xFFFFF0B8), Color(0xFFFFC14D), Color(0xFFB86B12), Color(0xFFFFC66D))
        val Mint = Gloss(Color(0xFFC4FFE6), Color(0xFF3FD69C), Color(0xFF14805A), Color(0xFF52D6A0))
        val Slate = Gloss(Color(0xFF5A6788), Color(0xFF2A3554), Color(0xFF141B30), Color(0xFF000000))
    }
}

/**
 * A glyph on a lit, rounded tile with a specular highlight, rim light and a colored contact shadow. Used for
 * category marks, achievements and quick actions where a flat outline would feel weightless.
 */
@Composable
fun GlossyIcon(
    glyph: ImageVector,
    gloss: Gloss,
    modifier: Modifier = Modifier,
    size: Dp = Pulse.dimens.tileMd,
    glyphSize: Dp = size * 0.46f,
    contentDescription: String? = null,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) { drawGlossTile(gloss) }
        PulseIcon(glyph, Pulse.colors.shadow.copy(alpha = 0.22f), Modifier.offset(y = Pulse.dimens.hairline), glyphSize)
        PulseIcon(glyph, Pulse.colors.highlight, size = glyphSize, contentDescription = contentDescription)
    }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlossTile(gloss: Gloss, cornerFraction: Float = 0.32f) {
    val w = size.width
    val h = size.height
    val radius = CornerRadius(w * cornerFraction)
    // Contact shadow: a soft colored pool under the tile.
    drawOval(
        brush = Brush.radialGradient(
            listOf(gloss.glow.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(w / 2, h * 0.92f),
            radius = w * 0.55f,
        ),
        topLeft = Offset(w * 0.05f, h * 0.7f),
        size = Size(w * 0.9f, h * 0.42f),
    )
    val inset = w * 0.04f
    val tileSize = Size(w - inset * 2, h - inset * 2.6f)
    val tileOrigin = Offset(inset, inset)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(gloss.top, gloss.mid, gloss.bottom), startY = 0f, endY = tileSize.height + inset),
        topLeft = tileOrigin,
        size = tileSize,
        cornerRadius = radius,
    )
    // Specular highlight across the upper half.
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.0f)),
            startY = tileOrigin.y,
            endY = tileOrigin.y + tileSize.height * 0.55f,
        ),
        topLeft = Offset(tileOrigin.x + tileSize.width * 0.08f, tileOrigin.y + tileSize.height * 0.04f),
        size = Size(tileSize.width * 0.84f, tileSize.height * 0.46f),
        cornerRadius = CornerRadius(w * cornerFraction * 0.8f),
    )
    // Rim light: bright at the top edge, gone by the bottom.
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.0f)),
            startY = tileOrigin.y,
            endY = tileOrigin.y + tileSize.height * 0.7f,
        ),
        topLeft = tileOrigin,
        size = tileSize,
        cornerRadius = radius,
        style = Stroke(width = w * 0.025f),
    )
}
