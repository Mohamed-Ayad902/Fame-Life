package org.mayad.fame_life.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.designsystem.components.EmptyState
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.hoursAgoLabel
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.data.local.nowEpochMs
import org.mayad.fame_life.feature.shell.BottomBarClearance

/** Kinds written by the app. Unknown kinds still render, with a neutral icon. */
object NotificationKind {
    const val WELCOME = "welcome"
    const val LEVEL_UP = "level_up"
    const val SESSION = "session"
}

@Composable
fun InboxScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    val notifications by container.notifications.notifications.collectAsState()
    // Remember which items were unread when the tab opened so they stay highlighted while the user reads them.
    val freshIds = remember { notifications.filterNot { it.read }.map { it.id }.toSet() }
    LaunchedEffect(Unit) { container.notifications.markAllRead() }
    val colors = Pulse.colors
    val now = remember { nowEpochMs() }

    LazyColumn(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = Pulse.dimens.gutter, end = Pulse.dimens.gutter, top = Pulse.dimens.lg, bottom = BottomBarClearance.calculateBottomPadding()),
    ) {
        item(key = "title") {
            Column {
                Text("Inbox", style = Pulse.type.display, color = colors.textPrimary)
                Text("Moments from your lives and your progress.", style = Pulse.type.body, color = colors.textSecondary)
                Spacer(Modifier.height(Pulse.dimens.xl))
            }
        }
        if (notifications.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    Glyphs.Bell,
                    Gloss.Blue,
                    title = "All quiet",
                    body = "Level-ups and live recaps will land here after your sessions.",
                    action = "Go live",
                    onAction = { navigator.push(Route.LiveSetup()) },
                )
            }
        } else {
            items(notifications, key = { it.id }) { item ->
                NotificationRow(
                    item,
                    fresh = item.id in freshIds,
                    ageLabel = hoursAgoLabel(((now - item.createdAtEpochMs).coerceAtLeast(0) / 3_600_000L).toInt()),
                    onClick = notificationTarget(item)?.let { route -> { navigator.push(route) } },
                )
                Spacer(Modifier.height(Pulse.dimens.sm))
            }
        }
    }
}

/** Session recaps carry the session id after the colon, e.g. "session:abc". Other kinds open nothing. */
private fun notificationTarget(item: InAppNotification): Route? = when {
    item.kind == NotificationKind.SESSION || item.kind == NotificationKind.LEVEL_UP ->
        item.id.substringAfter(':', "").takeIf { it.isNotBlank() }?.let { Route.Summary(it) }
    else -> null
}

@Composable
private fun NotificationRow(item: InAppNotification, fresh: Boolean, ageLabel: String, onClick: (() -> Unit)?) {
    val colors = Pulse.colors
    val (glyph, gloss) = when (item.kind) {
        NotificationKind.WELCOME -> Glyphs.Sparkles to Gloss.Violet
        NotificationKind.LEVEL_UP -> Glyphs.Trophy to Gloss.Gold
        NotificationKind.SESSION -> Glyphs.Wave to Gloss.Cyan
        else -> Glyphs.Bell to Gloss.Slate
    }
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.98f, onClick = onClick) else Modifier)
            .clip(Pulse.shapes.card)
            .background(if (fresh) colors.surfaceElevated else colors.surface)
            .border(1.dp, if (fresh) colors.borderStrong else colors.border, Pulse.shapes.card)
            .padding(Pulse.dimens.md),
    ) {
        GlossyIcon(glyph, gloss, size = 44.dp)
        Spacer(Modifier.width(Pulse.dimens.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.title, style = Pulse.type.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
                if (fresh) Box(Modifier.size(8.dp).clip(CircleShape).background(colors.cyan))
            }
            Spacer(Modifier.height(2.dp))
            Text(item.body, style = Pulse.type.bodySmall, color = colors.textSecondary)
            Spacer(Modifier.height(Pulse.dimens.xs))
            Text(ageLabel, style = Pulse.type.meta, color = colors.textMuted)
        }
    }
}
