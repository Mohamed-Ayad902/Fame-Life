package org.mayad.fame_life.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.Shapes
import org.mayad.fame_life.core.designsystem.theme.Space
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.data.content.ArtSpec

/**
 * The app's backdrop: midnight base with two static pools of brand light. Static on purpose; animated
 * full-screen gradients cost battery and pull attention from content.
 */
@Composable
fun AtmosphereBackground(modifier: Modifier = Modifier, accent: Color = Pulse.colors.violet, content: @Composable BoxScope.() -> Unit) {
    val colors = Pulse.colors
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(colors.primary.copy(alpha = 0.22f), Color.Transparent), Offset(size.width * 0.1f, size.height * 0.02f), size.width * 0.9f),
                    size.width * 0.9f,
                    Offset(size.width * 0.1f, size.height * 0.02f),
                )
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * 1.05f, size.height * 0.35f), size.width * 0.8f),
                    size.width * 0.8f,
                    Offset(size.width * 1.05f, size.height * 0.35f),
                )
            },
        content = content,
    )
}

/** The main call to action: brand gradient pill with a lit top edge. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: ImageVector? = null,
    brush: Brush? = null,
) {
    val colors = Pulse.colors
    Row(
        modifier
            .height(54.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .pressable(enabled = enabled, onClick = onClick)
            .clip(Shapes.pill)
            .background(brush ?: colors.brandGradient)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), 0f, size.height * 0.55f))
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)), Shapes.pill)
            .padding(horizontal = Space.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            PulseIcon(leading, colors.textOnAccent, size = 20.dp)
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, style = Pulse.type.label, color = colors.textOnAccent, maxLines = 1)
    }
}

/** Quiet glass button for secondary actions. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: ImageVector? = null,
) {
    val colors = Pulse.colors
    Row(
        modifier
            .height(50.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .pressable(enabled = enabled, onClick = onClick)
            .clip(Shapes.pill)
            .background(colors.surfaceElevated.copy(alpha = 0.7f))
            .border(1.dp, colors.border, Shapes.pill)
            .padding(horizontal = Space.lg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            PulseIcon(leading, colors.textPrimary, size = 18.dp)
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, style = Pulse.type.label, color = colors.textPrimary, maxLines = 1)
    }
}

/** Round glass control used over imagery and the camera. */
@Composable
fun GlassIconButton(
    glyph: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = Pulse.colors.textPrimary,
    active: Boolean = false,
) {
    val colors = Pulse.colors
    Box(
        modifier
            .size(size)
            .pressable(onClick = onClick)
            .clip(CircleShape)
            .background(if (active) colors.primary.copy(alpha = 0.85f) else Color(0x66101526))
            .border(1.dp, colors.borderStrong, CircleShape)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        PulseIcon(glyph, tint, size = size * 0.46f)
    }
}

/** Selectable filter or option. Locked chips stay tappable so they can explain the plan limit. */
@Composable
fun PulseChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    locked: Boolean = false,
) {
    val colors = Pulse.colors
    Row(
        modifier
            .height(38.dp)
            .pressable(role = Role.Tab, onClick = onClick)
            .clip(Shapes.pill)
            .background(if (selected) colors.textPrimary else colors.surface.copy(alpha = 0.8f))
            .border(1.dp, if (selected) Color.Transparent else colors.border, Shapes.pill)
            .padding(horizontal = 14.dp)
            .semantics { stateDescription = if (selected) "Selected" else if (locked) "Requires Pro" else "Not selected" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = if (selected) colors.background else colors.textSecondary
        if (leading != null) {
            PulseIcon(leading, content, size = 16.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = Pulse.type.label, color = content, maxLines = 1)
        if (locked) {
            Spacer(Modifier.width(6.dp))
            PulseIcon(Glyphs.Lock, colors.warning, size = 14.dp)
        }
    }
}

enum class AvatarRing { NONE, STORY, LIVE, SELF }

/** Creator avatar. Rings encode state: gradient story ring, live ring, or the signed-in creator's ring. */
@Composable
fun CreatorAvatar(spec: ArtSpec, size: Dp, modifier: Modifier = Modifier, ring: AvatarRing = AvatarRing.NONE) {
    val colors = Pulse.colors
    val ringBrush = when (ring) {
        AvatarRing.NONE -> null
        AvatarRing.STORY -> Brush.sweepGradient(listOf(colors.primary, colors.violet, colors.cyan, colors.primary))
        AvatarRing.LIVE -> colors.liveGradient
        AvatarRing.SELF -> Brush.linearGradient(listOf(colors.cyan, colors.primary))
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (ringBrush != null) {
            Box(Modifier.fillMaxSize().border(2.dp, ringBrush, CircleShape))
        }
        val inset = if (ringBrush != null) 4.dp else 0.dp
        Artwork(spec, Modifier.padding(inset).fillMaxSize().clip(CircleShape))
    }
}

/** LIVE pill with a breathing dot. The dot holds still when reduced motion is on. */
@Composable
fun LiveBadge(modifier: Modifier = Modifier, label: String = "LIVE") {
    val colors = Pulse.colors
    val reduce = LocalReduceMotion.current
    val pulse = if (reduce) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "live")
        val value by transition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "dot")
        value
    }
    Row(
        modifier
            .clip(Shapes.tag)
            .background(colors.liveGradient)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).graphicsLayer { alpha = pulse }.clip(CircleShape).background(Color.White))
        Spacer(Modifier.width(5.dp))
        Text(label, style = Pulse.type.overline, color = Color.White)
    }
}

/** Eye + count over imagery. */
@Composable
fun ViewerPill(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(Shapes.tag)
            .background(Color(0x8C080B14))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .semantics(mergeDescendants = true) { contentDescription = "${compactCount(count)} simulated viewers" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(Glyphs.Eye, Color.White, size = 13.dp)
        Spacer(Modifier.width(4.dp))
        Text(compactCount(count), style = Pulse.type.meta, color = Color.White)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, eyebrow: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    val colors = Pulse.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(eyebrow.uppercase(), style = Pulse.type.overline, color = colors.cyan)
                Spacer(Modifier.height(2.dp))
            }
            Text(title, style = Pulse.type.title, color = colors.textPrimary)
        }
        if (action != null && onAction != null) {
            Text(
                action,
                style = Pulse.type.meta,
                color = colors.textSecondary,
                modifier = Modifier.pressable(onClick = onAction).padding(Space.xs),
            )
        }
    }
}

/** A number that eases to its new value instead of jumping. Formatting stays compact (24.5K). */
@Composable
fun AnimatedCount(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier, compact: Boolean = true) {
    val reduce = LocalReduceMotion.current
    val shown by animateIntAsState(value, animationSpec = if (reduce) tween(0) else tween(600), label = "count")
    Text(if (compact) compactCount(shown) else shown.toString(), style = style, color = color, modifier = modifier, maxLines = 1)
}

@Composable
fun StatColumn(value: String, label: String, modifier: Modifier = Modifier, align: Alignment.Horizontal = Alignment.CenterHorizontally) {
    Column(modifier, horizontalAlignment = align) {
        Text(value, style = Pulse.type.metricSmall, color = Pulse.colors.textPrimary, maxLines = 1)
        Text(label, style = Pulse.type.meta, color = Pulse.colors.textMuted, maxLines = 1)
    }
}

/** The honest-disclosure tag used wherever simulated numbers might be mistaken for real ones. */
@Composable
fun SimulatedTag(modifier: Modifier = Modifier, text: String = "Simulated") {
    val colors = Pulse.colors
    Row(
        modifier
            .clip(Shapes.tag)
            .background(colors.cyan.copy(alpha = 0.12f))
            .border(1.dp, colors.cyan.copy(alpha = 0.3f), Shapes.tag)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(Glyphs.Sparkles, colors.cyan, size = 11.dp)
        Spacer(Modifier.width(4.dp))
        Text(text, style = Pulse.type.meta, color = colors.cyan)
    }
}

@Composable
fun EmptyState(
    glyph: ImageVector,
    gloss: Gloss,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = Pulse.colors
    Column(modifier.fillMaxWidth().padding(Space.xxl), horizontalAlignment = Alignment.CenterHorizontally) {
        GlossyIcon(glyph, gloss, size = 72.dp)
        Spacer(Modifier.height(Space.lg))
        Text(title, style = Pulse.type.title, color = colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.xs))
        Text(body, style = Pulse.type.body, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(Space.lg))
            SecondaryButton(action, onAction)
        }
    }
}

/** A horizontal list row with a lead icon, title, optional detail and trailing content. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = Pulse.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.98f, onClick = onClick) else Modifier)
            .padding(vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Space.md))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) Text(detail, style = Pulse.type.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Spacer(Modifier.width(Space.md))
            trailing()
        }
    }
}

/** Labeled single-line text input with inline validation. */
@Composable
fun PulseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    prefix: String? = null,
    singleLine: Boolean = true,
    maxLength: Int = 60,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    secret: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = Pulse.colors
    Column(modifier.fillMaxWidth()) {
        Text(label, style = Pulse.type.meta, color = colors.textSecondary)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(Shapes.control)
                .background(colors.surface)
                .border(1.dp, if (error != null) colors.error else colors.border, Shapes.control)
                .padding(horizontal = Space.lg, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (prefix != null) Text(prefix, style = Pulse.type.body, color = colors.textMuted)
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, style = Pulse.type.body, color = colors.textMuted)
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = { if (it.length <= maxLength) onValueChange(it) },
                    singleLine = singleLine,
                    textStyle = Pulse.type.body.copy(color = colors.textPrimary),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.cyan),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = keyboardType,
                        autoCorrectEnabled = !secret && keyboardType == androidx.compose.ui.text.input.KeyboardType.Text,
                    ),
                    visualTransformation = if (secret) {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    } else {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(Space.sm))
                trailing()
            }
        }
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error, style = Pulse.type.meta, color = colors.error)
        }
    }
}

/** Header for pushed screens: back control, title and an optional trailing action. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(Glyphs.ChevronLeft, contentDescription = "Back", onClick = onBack)
        Spacer(Modifier.width(Space.md))
        Text(title, style = Pulse.type.headline, color = Pulse.colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailing != null) trailing()
    }
}
