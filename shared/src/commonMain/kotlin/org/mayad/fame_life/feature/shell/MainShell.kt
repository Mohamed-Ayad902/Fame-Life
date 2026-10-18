package org.mayad.fame_life.feature.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.app.Tab
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.feature.discover.DiscoverScreen
import org.mayad.fame_life.feature.home.HomeScreen
import org.mayad.fame_life.feature.notifications.InboxScreen
import org.mayad.fame_life.feature.profile.OwnProfileScreen

/** Height reserved at the bottom of tab content so the floating bar never covers it. */
val BottomBarClearance = PaddingValues(bottom = 112.dp)

@Composable
fun MainShell(navigator: Navigator) {
    var tabName by rememberSaveable { mutableStateOf(Tab.HOME.name) }
    val tab = Tab.valueOf(tabName)
    val container = LocalAppContainer.current
    val unread = container.notifications.notifications.collectAsState().value.count { !it.read }

    AtmosphereBackground {
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(Motion.standard()) togetherWith fadeOut(Motion.exit()) },
            modifier = Modifier.fillMaxSize(),
            label = "tabs",
        ) { current ->
            when (current) {
                Tab.HOME -> HomeScreen(navigator, onOpenInbox = { tabName = Tab.INBOX.name })
                Tab.DISCOVER -> DiscoverScreen(navigator)
                Tab.INBOX -> InboxScreen(navigator)
                Tab.PROFILE -> OwnProfileScreen(navigator)
            }
        }
        PulseTabBar(
            selected = tab,
            unread = unread,
            onSelect = { tabName = it.name },
            onCreate = {
                container.analytics.log(AnalyticsEvent.CreateLiveTapped)
                navigator.push(Route.LiveSetup())
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PulseTabBar(selected: Tab, unread: Int, onSelect: (Tab) -> Unit, onCreate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(24.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(CircleShape)
                .background(Color(0xF00D1220))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.03f))), CircleShape)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(Glyphs.Home, "Home", selected == Tab.HOME) { onSelect(Tab.HOME) }
            TabItem(Glyphs.Discover, "Discover", selected == Tab.DISCOVER) { onSelect(Tab.DISCOVER) }
            Spacer(Modifier.size(64.dp))
            TabItem(Glyphs.Bell, "Inbox", selected == Tab.INBOX, badge = unread) { onSelect(Tab.INBOX) }
            TabItem(Glyphs.User, "Profile", selected == Tab.PROFILE) { onSelect(Tab.PROFILE) }
        }
        CreateOrb(onCreate, Modifier.offset(y = (-22).dp))
    }
}

@Composable
private fun TabItem(glyph: ImageVector, label: String, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
    val colors = Pulse.colors
    val emphasis by animateFloatAsState(if (selected) 1f else 0f, Motion.standard(), label = "tab")
    Column(
        Modifier
            .size(width = 60.dp, height = 56.dp)
            .pressable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = if (badge > 0) "$label, $badge new" else label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            PulseIcon(glyph, if (selected) colors.textPrimary else colors.textMuted, size = 24.dp)
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(colors.live)
                        .border(1.5.dp, Color(0xFF0D1220), CircleShape),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = Pulse.type.meta, color = if (selected) colors.textPrimary else colors.textMuted, maxLines = 1)
        Box(
            Modifier
                .padding(top = 3.dp)
                .size(4.dp)
                .graphicsLayer { alpha = emphasis; scaleX = emphasis; scaleY = emphasis }
                .clip(CircleShape)
                .background(colors.cyan),
        )
    }
}

/** The raised Create button: a lit sphere in brand colors, the one 3D object on the bar. */
@Composable
private fun CreateOrb(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    Box(
        modifier
            .size(70.dp)
            .pressable(pressedScale = 0.9f, onClick = onClick)
            .semantics { contentDescription = "Go live" }
            .drawBehind {
                drawCircle(Brush.radialGradient(listOf(colors.violet.copy(alpha = 0.55f), Color.Transparent), center, size.width * 0.75f), size.width * 0.75f)
            }
            .padding(5.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF7FA2FF), colors.primary, colors.violet), start = Offset.Zero, end = Offset(180f, 180f)))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), Offset(size.width * 0.35f, size.height * 0.25f), size.width * 0.45f),
                    size.width * 0.45f,
                    Offset(size.width * 0.35f, size.height * 0.25f),
                )
            }
            .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PulseIcon(Glyphs.Plus, Color.White, size = 30.dp)
    }
}
