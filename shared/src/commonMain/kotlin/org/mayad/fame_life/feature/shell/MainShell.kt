package org.mayad.fame_life.feature.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.PulseWidth
import org.mayad.fame_life.core.presentation.CollectEvents
import org.mayad.fame_life.feature.discover.DiscoverScreen
import org.mayad.fame_life.feature.home.HomeScreen
import org.mayad.fame_life.feature.notifications.InboxScreen
import org.mayad.fame_life.feature.points.DailyRewardHost
import org.mayad.fame_life.feature.profile.OwnProfileScreen

/**
 * The tab shell. Phones and medium windows get a floating bottom bar with the raised Create orb; expanded windows
 * (tablets, desktop-size) get a side rail, so the content keeps its full height.
 */
@Composable
fun MainShell(navigation: MainNavigation, viewModel: ShellViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            ShellEvent.OpenLiveSetup -> navigation.openLiveSetup("")
        }
    }
    val select = { tab: Tab -> viewModel.onIntent(ShellIntent.Select(tab)) }
    val create = { viewModel.onIntent(ShellIntent.Create) }
    AtmosphereBackground {
        if (Pulse.width == PulseWidth.Expanded) {
            Row(Modifier.fillMaxSize()) {
                PulseNavRail(state.tab, state.unread, onSelect = select, onCreate = create)
                TabContent(state.tab, navigation, onOpenInbox = { select(Tab.INBOX) }, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            TabContent(state.tab, navigation, onOpenInbox = { select(Tab.INBOX) }, Modifier.fillMaxSize())
            PulseTabBar(state.tab, state.unread, onSelect = select, onCreate = create, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
    // The daily check-in calendar opens over the tabs when today's reward is waiting.
    DailyRewardHost()
}

@Composable
private fun TabContent(tab: Tab, navigation: MainNavigation, onOpenInbox: () -> Unit, modifier: Modifier) {
    AnimatedContent(
        targetState = tab,
        transitionSpec = { fadeIn(Motion.standard()) togetherWith fadeOut(Motion.exit()) },
        modifier = modifier,
        label = "tabs",
    ) { current ->
        when (current) {
            Tab.HOME -> HomeScreen(navigation, onOpenInbox)
            Tab.DISCOVER -> DiscoverScreen(navigation)
            Tab.INBOX -> InboxScreen(navigation)
            Tab.PROFILE -> OwnProfileScreen(navigation)
        }
    }
}
