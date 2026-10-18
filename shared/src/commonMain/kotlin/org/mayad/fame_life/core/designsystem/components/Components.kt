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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.common_back
import famelife.shared.generated.resources.common_live
import famelife.shared.generated.resources.common_not_selected
import famelife.shared.generated.resources.common_requires_pro
import famelife.shared.generated.resources.common_selected
import famelife.shared.generated.resources.common_simulated
import famelife.shared.generated.resources.common_simulated_viewers
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec

/** Opacity of a disabled control: still legible, clearly inactive. */
private const val DISABLED_ALPHA = 0.45f
private const val DEFAULT_MAX_LENGTH = 60

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
    val dimens = Pulse.dimens
    Row(
        modifier
            .height(dimens.buttonHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressable(enabled = enabled, onClick = onClick)
            .clip(Pulse.shapes.pill)
            .background(brush ?: colors.brandGradient)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(colors.highlight.copy(alpha = 0.22f), Color.Transparent), 0f, size.height * 0.55f))
            }
            .border(dimens.hairline, Brush.verticalGradient(listOf(colors.highlight.copy(alpha = 0.45f), Color.Transparent)), Pulse.shapes.pill)
            .padding(horizontal = dimens.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            PulseIcon(leading, colors.textOnAccent, size = dimens.icon)
            Spacer(Modifier.width(dimens.sm))
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
    val dimens = Pulse.dimens
    Row(
        modifier
            .height(dimens.buttonHeightCompact)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressable(enabled = enabled, onClick = onClick)
            .clip(Pulse.shapes.pill)
            .background(colors.surfaceElevated.copy(alpha = 0.7f))
            .border(dimens.hairline, colors.border, Pulse.shapes.pill)
            .padding(horizontal = dimens.lg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            PulseIcon(leading, colors.textPrimary, size = dimens.iconSmall)
            Spacer(Modifier.width(dimens.sm))
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
    size: Dp = Pulse.dimens.glassButton,
    tint: Color = Pulse.colors.textPrimary,
    active: Boolean = false,
) {
    val colors = Pulse.colors
    Box(
        modifier
            .size(size)
            .pressable(onClick = onClick)
            .clip(CircleShape)
            .background(if (active) colors.primary.copy(alpha = 0.85f) else colors.glass)
            .border(Pulse.dimens.hairline, colors.borderStrong, CircleShape)
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
    val dimens = Pulse.dimens
    val state = when {
        selected -> stringResource(Res.string.common_selected)
        locked -> stringResource(Res.string.common_requires_pro)
        else -> stringResource(Res.string.common_not_selected)
    }
    Row(
        modifier
            .height(dimens.chipHeight)
            .pressable(role = Role.Tab, onClick = onClick)
            .clip(Pulse.shapes.pill)
            .background(if (selected) colors.textPrimary else colors.surface.copy(alpha = 0.8f))
            .border(dimens.hairline, if (selected) Color.Transparent else colors.border, Pulse.shapes.pill)
            .padding(horizontal = dimens.md)
            .semantics { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = if (selected) colors.background else colors.textSecondary
        if (leading != null) {
            PulseIcon(leading, content, size = dimens.iconSmall)
            Spacer(Modifier.width(dimens.xs))
        }
        Text(text, style = Pulse.type.label, color = content, maxLines = 1)
        if (locked) {
            Spacer(Modifier.width(dimens.xs))
            PulseIcon(Glyphs.Lock, colors.warning, size = dimens.iconTiny)
        }
    }
}

enum class AvatarRing { NONE, STORY, LIVE, SELF }

/** Creator avatar. Rings encode state: gradient story ring, live ring, or the signed-in creator's ring. */
@Composable
fun CreatorAvatar(spec: ArtSpec, size: Dp, modifier: Modifier = Modifier, ring: AvatarRing = AvatarRing.NONE) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val ringBrush = when (ring) {
        AvatarRing.NONE -> null
        AvatarRing.STORY -> Brush.sweepGradient(listOf(colors.primary, colors.violet, colors.cyan, colors.primary))
        AvatarRing.LIVE -> colors.liveGradient
        AvatarRing.SELF -> Brush.linearGradient(listOf(colors.cyan, colors.primary))
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val art = Modifier.fillMaxSize().clip(CircleShape)
        if (ringBrush != null) {
            Box(Modifier.fillMaxSize().border(dimens.ring, ringBrush, CircleShape))
            Artwork(spec, Modifier.padding(dimens.xs).then(art))
        } else {
            Artwork(spec, art)
        }
    }
}

/**
 * LIVE pill with a breathing dot. The dot holds still when reduced motion is on; the infinite transition only runs
 * while the badge is composed, so it stops when the badge scrolls away or the screen leaves.
 */
@Composable
fun LiveBadge(modifier: Modifier = Modifier, label: String = stringResource(Res.string.common_live)) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val pulse = if (Pulse.reducedMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "live").animateFloat(
            0.35f,
            1f,
            infiniteRepeatable(tween(Motion.BREATH, easing = LinearEasing), RepeatMode.Reverse),
            label = "dot",
        )
    }
    Row(
        modifier
            .clip(Pulse.shapes.tag)
            .background(colors.liveGradient)
            .padding(horizontal = dimens.sm, vertical = dimens.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Read inside the layer block, so breathing never recomposes the badge.
        Box(Modifier.size(dimens.dot).graphicsLayer { alpha = pulse?.value ?: 1f }.clip(CircleShape).background(colors.onMedia))
        Spacer(Modifier.width(dimens.xs))
        Text(label, style = Pulse.type.overline, color = colors.onMedia)
    }
}

/** Eye + count over imagery. */
@Composable
fun ViewerPill(count: Int, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val description = stringResource(Res.string.common_simulated_viewers, compactCount(count))
    Row(
        modifier
            .clip(Pulse.shapes.tag)
            .background(colors.glassStrong)
            .padding(horizontal = dimens.sm, vertical = dimens.xxs)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(Glyphs.Eye, colors.onMedia, size = dimens.iconTiny)
        Spacer(Modifier.width(dimens.xs))
        Text(compactCount(count), style = Pulse.type.meta, color = colors.onMedia)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, eyebrow: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    val colors = Pulse.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(eyebrow.uppercase(), style = Pulse.type.overline, color = colors.cyan)
                Spacer(Modifier.height(Pulse.dimens.xxs))
            }
            Text(title, style = Pulse.type.title, color = colors.textPrimary)
        }
        if (action != null && onAction != null) {
            Text(
                action,
                style = Pulse.type.meta,
                color = colors.textSecondary,
                modifier = Modifier.pressable(onClick = onAction).padding(Pulse.dimens.xs),
            )
        }
    }
}

/** A number that eases to its new value instead of jumping. Formatting stays compact (24.5K). */
@Composable
fun AnimatedCount(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier, compact: Boolean = true) {
    val shown by animateIntAsState(value, animationSpec = Motion.spec(tween(Motion.COUNT)), label = "count")
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
fun SimulatedTag(modifier: Modifier = Modifier, text: String = stringResource(Res.string.common_simulated)) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(
        modifier
            .clip(Pulse.shapes.tag)
            .background(colors.cyan.copy(alpha = 0.12f))
            .border(dimens.hairline, colors.cyan.copy(alpha = 0.3f), Pulse.shapes.tag)
            .padding(horizontal = dimens.sm, vertical = dimens.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(Glyphs.Sparkles, colors.cyan, size = dimens.iconTiny)
        Spacer(Modifier.width(dimens.xs))
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
    val dimens = Pulse.dimens
    Column(modifier.fillMaxWidth().padding(dimens.xxl), horizontalAlignment = Alignment.CenterHorizontally) {
        GlossyIcon(glyph, gloss, size = dimens.tileXl)
        Spacer(Modifier.height(dimens.lg))
        Text(title, style = Pulse.type.title, color = colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(dimens.xs))
        Text(body, style = Pulse.type.body, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(dimens.lg))
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
    val dimens = Pulse.dimens
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.98f, onClick = onClick) else Modifier)
            .padding(vertical = dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(dimens.md))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) Text(detail, style = Pulse.type.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Spacer(Modifier.width(dimens.md))
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
    maxLength: Int = DEFAULT_MAX_LENGTH,
    keyboardType: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Column(modifier.fillMaxWidth()) {
        Text(label, style = Pulse.type.meta, color = colors.textSecondary)
        Spacer(Modifier.height(dimens.xs))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(Pulse.shapes.control)
                .background(colors.surface)
                .border(dimens.hairline, if (error != null) colors.error else colors.border, Pulse.shapes.control)
                .padding(horizontal = dimens.lg, vertical = dimens.fieldPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (prefix != null) Text(prefix, style = Pulse.type.body, color = colors.textMuted)
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, style = Pulse.type.body, color = colors.textMuted)
                BasicTextField(
                    value = value,
                    onValueChange = { if (it.length <= maxLength) onValueChange(it) },
                    singleLine = singleLine,
                    textStyle = Pulse.type.body.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.cyan),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        autoCorrectEnabled = !secret && keyboardType == KeyboardType.Text,
                    ),
                    visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(dimens.sm))
                trailing()
            }
        }
        if (error != null) {
            Spacer(Modifier.height(dimens.xs))
            Text(error, style = Pulse.type.meta, color = colors.error)
        }
    }
}

/** Header for pushed screens: back control (mirrored in RTL), title and an optional trailing action. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    val dimens = Pulse.dimens
    Row(
        modifier.fillMaxWidth().padding(horizontal = dimens.lg, vertical = dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(Glyphs.ChevronLeft, contentDescription = stringResource(Res.string.common_back), onClick = onBack)
        Spacer(Modifier.width(dimens.md))
        Text(title, style = Pulse.type.headline, color = Pulse.colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailing != null) trailing()
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

private val PreviewAvatar = ArtSpec(3, ArtMotif.PORTRAIT, 42)

@ComponentPreviews
@Composable
private fun ButtonsPreview() = PreviewContainer {
    Column(Modifier.padding(Pulse.dimens.gutter), verticalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
        PrimaryButton(stringResource(Res.string.common_live), {}, Modifier.fillMaxWidth(), leading = Glyphs.Bolt)
        PrimaryButton(stringResource(Res.string.common_live), {}, Modifier.fillMaxWidth(), enabled = false)
        SecondaryButton(stringResource(Res.string.common_back), {}, leading = Glyphs.ChevronLeft)
        Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
            GlassIconButton(Glyphs.Bell, stringResource(Res.string.common_back), {})
            GlassIconButton(Glyphs.Mic, stringResource(Res.string.common_back), {}, active = true)
        }
    }
}

@ComponentPreviews
@Composable
private fun ChipsAndTagsPreview() = PreviewContainer {
    Column(Modifier.padding(Pulse.dimens.gutter), verticalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
            PulseChip(stringResource(Res.string.common_selected), selected = true, onClick = {}, leading = Glyphs.Star)
            PulseChip(stringResource(Res.string.common_requires_pro), selected = false, onClick = {}, locked = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm), verticalAlignment = Alignment.CenterVertically) {
            LiveBadge()
            ViewerPill(12_480)
            SimulatedTag()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
            AvatarRing.entries.forEach { CreatorAvatar(PreviewAvatar, Pulse.dimens.avatarLg, ring = it) }
        }
    }
}

@ComponentPreviews
@Composable
private fun RowsAndFieldsPreview() = PreviewContainer {
    Column(Modifier.padding(Pulse.dimens.gutter)) {
        ScreenHeader(stringResource(Res.string.common_simulated), onBack = {})
        SectionHeader(stringResource(Res.string.common_simulated), eyebrow = stringResource(Res.string.common_live))
        ListRow(
            stringResource(Res.string.common_simulated),
            detail = stringResource(Res.string.common_simulated_viewers, "2.4K"),
            leading = { GlossyIcon(Glyphs.Sparkles, Gloss.Cyan, size = Pulse.dimens.tileSm) },
            onClick = {},
        )
        PulseTextField("", {}, stringResource(Res.string.common_simulated), placeholder = stringResource(Res.string.common_live))
        Spacer(Modifier.height(Pulse.dimens.md))
        PulseTextField("x", {}, stringResource(Res.string.common_simulated), error = stringResource(Res.string.common_requires_pro))
        Row(Modifier.fillMaxWidth().padding(top = Pulse.dimens.lg), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatColumn("2.4K", stringResource(Res.string.common_simulated))
            AnimatedCount(12_480, Pulse.type.metricSmall, Pulse.colors.textPrimary)
        }
    }
}

@ComponentPreviews
@Composable
private fun EmptyStatePreview() = PreviewContainer {
    EmptyState(Glyphs.Sparkles, Gloss.Violet, stringResource(Res.string.common_simulated), stringResource(Res.string.common_simulated_viewers, "0"))
}
