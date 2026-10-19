package org.mayad.fame_life.feature.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.go_live
import famelife.shared.generated.resources.tab_discover
import famelife.shared.generated.resources.tab_home
import famelife.shared.generated.resources.tab_inbox
import famelife.shared.generated.resources.tab_profile
import famelife.shared.generated.resources.tab_with_new
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse

/** The floating tab bar, the side rail for wide windows, and the raised Create orb. */

private data class TabSpec(val tab: Tab, val glyph: ImageVector, val label: StringResource)

private val Tabs = listOf(
    TabSpec(Tab.HOME, Glyphs.Home, Res.string.tab_home),
    TabSpec(Tab.DISCOVER, Glyphs.Discover, Res.string.tab_discover),
    TabSpec(Tab.INBOX, Glyphs.Bell, Res.string.tab_inbox),
    TabSpec(Tab.PROFILE, Glyphs.User, Res.string.tab_profile),
)

@Composable
private fun Modifier.navSurface(shape: Shape): Modifier {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    return this
        .shadow(dimens.navElevation, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
        .clip(shape)
        .background(colors.navSurface)
        .border(dimens.hairline, Brush.verticalGradient(listOf(colors.highlight.copy(alpha = 0.14f), colors.highlight.copy(alpha = 0.03f))), shape)
}

@Composable
internal fun PulseTabBar(selected: Tab, unread: Int, onSelect: (Tab) -> Unit, onCreate: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = Pulse.dimens
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = dimens.lg, vertical = dimens.sm),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(dimens.navBarHeight)
                .navSurface(CircleShape)
                .padding(horizontal = dimens.sm),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tabs.take(2).forEach { TabItem(it, selected == it.tab, if (it.tab == Tab.INBOX) unread else 0) { onSelect(it.tab) } }
            Spacer(Modifier.size(dimens.createOrb - dimens.xs))
            Tabs.drop(2).forEach { TabItem(it, selected == it.tab, if (it.tab == Tab.INBOX) unread else 0) { onSelect(it.tab) } }
        }
        CreateOrb(onCreate, Modifier.offset(y = -dimens.createOrbLift))
    }
}

@Composable
internal fun PulseNavRail(selected: Tab, unread: Int, onSelect: (Tab) -> Unit, onCreate: () -> Unit) {
    val dimens = Pulse.dimens
    Column(
        Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(dimens.md)
            .width(dimens.navRailWidth)
            .navSurface(Pulse.shapes.panel)
            .padding(vertical = dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.lg),
    ) {
        CreateOrb(onCreate)
        Spacer(Modifier.height(dimens.sm))
        Tabs.forEach { TabItem(it, selected == it.tab, if (it.tab == Tab.INBOX) unread else 0) { onSelect(it.tab) } }
    }
}

@Composable
private fun TabItem(spec: TabSpec, selected: Boolean, badge: Int, onClick: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val label = stringResource(spec.label)
    val description = if (badge > 0) stringResource(Res.string.tab_with_new, label, badge) else label
    val emphasis = animateFloatAsState(if (selected) 1f else 0f, Motion.spec(Motion.standard()), label = "tab")
    Column(
        Modifier
            .size(width = dimens.navItemWidth, height = dimens.navItemHeight)
            .pressable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            PulseIcon(spec.glyph, if (selected) colors.textPrimary else colors.textMuted, size = dimens.iconMedium)
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = dimens.xs, y = -dimens.xxs)
                        .size(dimens.badgeDot)
                        .clip(CircleShape)
                        .background(colors.live)
                        .border(dimens.stroke, colors.navSurface, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(dimens.xs))
        Text(label, style = Pulse.type.meta, color = if (selected) colors.textPrimary else colors.textMuted, maxLines = 1)
        Box(
            Modifier
                .padding(top = dimens.xxs)
                .size(dimens.dotSmall)
                // The selection dot animates in its layer; switching tabs never recomposes the bar for it.
                .graphicsLayer { val e = emphasis.value; alpha = e; scaleX = e; scaleY = e }
                .clip(CircleShape)
                .background(colors.cyan),
        )
    }
}

/** The raised Create button: a lit sphere in brand colors, the one 3D object on the bar. */
@Composable
private fun CreateOrb(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val description = stringResource(Res.string.go_live)
    Box(
        modifier
            .size(dimens.createOrb)
            .pressable(pressedScale = 0.9f, onClick = onClick)
            .semantics { contentDescription = description }
            .drawBehind {
                drawCircle(Brush.radialGradient(listOf(colors.violet.copy(alpha = 0.55f), Color.Transparent), center, size.width * 0.75f), size.width * 0.75f)
            }
            .padding(dimens.xs)
            .clip(CircleShape)
            .background(colors.orbGradient)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(colors.highlight.copy(alpha = 0.55f), Color.Transparent), Offset(size.width * 0.35f, size.height * 0.25f), size.width * 0.45f),
                    size.width * 0.45f,
                    Offset(size.width * 0.35f, size.height * 0.25f),
                )
            }
            .border(dimens.stroke, Brush.verticalGradient(listOf(colors.highlight.copy(alpha = 0.7f), Color.Transparent)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PulseIcon(Glyphs.Plus, colors.onMedia, size = dimens.iconLarge)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ComponentPreviews
@Composable
private fun TabBarPreview() = PreviewContainer {
    Box(Modifier.fillMaxWidth().height(Pulse.dimens.bottomBarClearance + Pulse.dimens.xl)) {
        PulseTabBar(Tab.HOME, unread = 2, onSelect = {}, onCreate = {}, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@ComponentPreviews
@Composable
private fun NavRailPreview() = PreviewContainer { PulseNavRail(Tab.DISCOVER, unread = 0, onSelect = {}, onCreate = {}) }
