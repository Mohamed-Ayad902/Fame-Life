package org.mayad.fame_life.core.designsystem.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.mayad.fame_life.core.designsystem.theme.Pulse

/**
 * One icon family for the whole app: 24-unit grid, 2-unit rounded strokes, drawn here so no icon library is
 * needed. Glyphs render flat (tinted) in dense UI and inside [GlossyIcon] tiles where they need depth.
 */
object Glyphs {
    private const val GRID = 24f

    val Home = stroke("Home", "M3 10.5L12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z")
    val Discover = stroke("Discover", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M15.5 8.5l-2 5-5 2 2-5z")
    val Plus = stroke("Plus", "M12 5v14", "M5 12h14")
    val Bell = stroke("Bell", "M6 16v-5a6 6 0 0 1 12 0v5l2 2H4z", "M10 20.5a2 2 0 0 0 4 0")
    val User = stroke("User", "M12 12a4 4 0 1 0 0-8a4 4 0 1 0 0 8z", "M4 21a8 8 0 0 1 16 0")
    val Search = stroke("Search", "M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14z", "M20 20l-4-4")
    val Heart = stroke("Heart", HEART)
    val HeartFilled = fill("HeartFilled", HEART)
    val Comment = stroke("Comment", "M4 5h16v11H9l-5 4z")
    val Share = stroke("Share", "M4 12v7a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-7", "M12 3v12", "M7 8l5-5 5 5")
    val Mic = stroke("Mic", MIC_BODY, "M5 11a7 7 0 0 0 14 0", "M12 18v3")
    val MicOff = stroke("MicOff", MIC_BODY, "M5 11a7 7 0 0 0 14 0", "M12 18v3", "M3 3l18 18")
    val FlipCamera = stroke("FlipCamera", "M4 8h3l2-3h6l2 3h3v11H4z", "M9 13.5a3 3 0 0 1 5.2-2", "M15 12.5a3 3 0 0 1-5.2 2")
    val Close = stroke("Close", "M6 6l12 12", "M18 6L6 18")
    val Sliders = stroke("Sliders", "M4 7h9", "M19 7h1", "M4 17h2", "M12 17h8", "M16 5a2 2 0 1 0 0 4a2 2 0 1 0 0-4z", "M9 15a2 2 0 1 0 0 4a2 2 0 1 0 0-4z")
    val Eye = stroke("Eye", "M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z", "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z")
    val Play = fill("Play", "M8 5v14l11-7z")
    val Pause = stroke("Pause", "M8 5v14", "M16 5v14")
    val Bolt = stroke("Bolt", "M13 2L4 14h7l-1 8 9-12h-7z")
    val Flame = stroke("Flame", "M12 22a7 7 0 0 0 7-7c0-4-3-6-4-9-1.5 2-2 3-3 3s-1-2-1-4c-3 2-6 6-6 10a7 7 0 0 0 7 7z")
    val ChevronLeft = stroke("ChevronLeft", "M15 5l-7 7 7 7", mirror = true)
    val ChevronRight = stroke("ChevronRight", "M9 5l7 7-7 7", mirror = true)
    val Check = stroke("Check", "M5 12.5l4.5 4.5L19 7")
    val Lock = stroke("Lock", "M6 11h12v10H6z", "M8.5 11V8a3.5 3.5 0 0 1 7 0v3")
    val Star = stroke("Star", STAR)
    val StarFilled = fill("StarFilled", STAR)
    val Trophy = stroke("Trophy", "M8 4h8v5a4 4 0 0 1-8 0z", "M8 6H5a3 3 0 0 0 3 4", "M16 6h3a3 3 0 0 1-3 4", "M12 13v4", "M8 21h8l-1-4H9z")
    val Clock = stroke("Clock", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M12 7v5l3 2")
    val Gift = stroke("Gift", "M4 10h16v10H4z", "M3 7h18v3H3z", "M12 7v13", "M12 7c-1.5-3-5-3-5-1s3 1 5 1z", "M12 7c1.5-3 5-3 5-1s-3 1-5 1z")
    val Sparkles = stroke("Sparkles", "M12 3l1.8 4.6L18 9.5l-4.2 1.8L12 16l-1.8-4.7L6 9.5l4.2-1.9z", "M19 15l.8 2 2 .8-2 .9-.8 2-.8-2-2-.9 2-.8z")
    val Logout = stroke("Logout", "M15 4h4v16h-4", "M10 8l-4 4 4 4", "M6 12h10")
    val Trash = stroke("Trash", "M4 7h16", "M9 7V4h6v3", "M6 7l1 13h10l1-13")
    val Gamepad = stroke("Gamepad", "M6 8h12a4 4 0 0 1 3.9 4.9l-.9 4a2.5 2.5 0 0 1-4.3 1.1L15 16H9l-1.7 2a2.5 2.5 0 0 1-4.3-1.1l-.9-4A4 4 0 0 1 6 8z", "M8 11v4", "M6 13h4", "M16 12h.01", "M18 14h.01")
    val Music = stroke("Music", "M9 18V6l11-2v12", "M6 15a3 3 0 1 0 0 6a3 3 0 1 0 0-6z", "M17 13a3 3 0 1 0 0 6a3 3 0 1 0 0-6z")
    val Smile = stroke("Smile", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M8 14s1.5 2 4 2 4-2 4-2", "M9 9.5h.01", "M15 9.5h.01")
    val Coffee = stroke("Coffee", "M4 8h13v6a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5z", "M17 10h1.5a2.5 2.5 0 0 1 0 5H17", "M8 2v3", "M12 2v3")
    val Book = stroke("Book", "M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 1-3-3z", "M5 17a3 3 0 0 1 3-3h11")
    val Film = stroke("Film", "M4 4h16v16H4z", "M8 4v16", "M16 4v16", "M4 9h4", "M4 15h4", "M16 9h4", "M16 15h4")
    val Globe = stroke("Globe", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M3 12h18", "M12 3a14 14 0 0 1 0 18a14 14 0 0 1 0-18z")
    val Crown = stroke("Crown", "M3 8l4.5 4L12 5l4.5 7L21 8l-2 11H5z")
    val Users = stroke("Users", "M9 11a3.5 3.5 0 1 0 0-7a3.5 3.5 0 1 0 0 7z", "M2.5 20a6.5 6.5 0 0 1 13 0", "M16 4.5a3.5 3.5 0 0 1 0 6.5", "M18 14a6 6 0 0 1 3.5 6")
    val Edit = stroke("Edit", "M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4")
    val Info = stroke("Info", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M12 11v6", "M12 7.5h.01")
    val Shield = stroke("Shield", "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z", "M9 12l2 2 4-4")
    val Wave = stroke("Wave", "M3 12h2", "M7 8v8", "M11 5v14", "M15 9v6", "M19 11v2")

    private const val HEART = "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z"
    private const val STAR = "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"
    private const val MIC_BODY = "M12 3a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3z"

    private fun stroke(name: String, vararg paths: String, mirror: Boolean = false) = build(name, paths, filled = false, mirror = mirror)
    private fun fill(name: String, vararg paths: String) = build(name, paths, filled = true, mirror = false)

    /** Directional glyphs ([mirror]) flip automatically in right-to-left layouts. */
    private fun build(name: String, paths: Array<out String>, filled: Boolean, mirror: Boolean): ImageVector {
        val builder = ImageVector.Builder(name, GRID.dp, GRID.dp, GRID, GRID, autoMirror = mirror)
        paths.forEach { d ->
            builder.addPath(
                pathData = addPathNodes(d),
                fill = if (filled) SolidColor(Color.White) else null,
                stroke = if (filled) null else SolidColor(Color.White),
                strokeLineWidth = if (filled) 0f else 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}

/** A flat glyph in the given tint. Pass a [contentDescription] whenever the icon is the only label. */
@Composable
fun PulseIcon(
    glyph: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = Pulse.dimens.icon,
    contentDescription: String? = null,
) {
    Image(
        imageVector = glyph,
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
