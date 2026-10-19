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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.collections.immutable.toImmutableList
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.go_live
import famelife.shared.generated.resources.inbox_empty_body
import famelife.shared.generated.resources.inbox_empty_title
import famelife.shared.generated.resources.inbox_subtitle
import famelife.shared.generated.resources.inbox_title
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.EmptyState
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.headerScroll
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.motion.scrollLinkedHeader
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.core.ui.timeAgoText

/** Tier 2: stateless list, centered and width-capped on wide windows so lines stay readable. */
@Composable
fun InboxContent(
    state: InboxUiState,
    onGoLive: () -> Unit,
    onOpenSummary: (sessionId: String) -> Unit,
) {
    val dimens = Pulse.dimens
    val list = rememberLazyListState()
    val cascade = rememberCascadeState()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.fillMaxSize().widthIn(max = dimens.contentMaxWidth).windowInsetsPadding(WindowInsets.statusBars),
            state = list,
            contentPadding = PaddingValues(start = dimens.gutter, end = dimens.gutter, top = dimens.lg, bottom = dimens.bottomBarClearance),
        ) {
            item(key = "title") {
                Column(Modifier.scrollLinkedHeader { list.headerScroll() }) {
                    Text(stringResource(Res.string.inbox_title), style = Pulse.type.display, color = Pulse.colors.textPrimary)
                    Text(stringResource(Res.string.inbox_subtitle), style = Pulse.type.body, color = Pulse.colors.textSecondary)
                    Spacer(Modifier.height(dimens.xl))
                }
            }
            if (state.items.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        Glyphs.Bell,
                        Gloss.Blue,
                        title = stringResource(Res.string.inbox_empty_title),
                        body = stringResource(Res.string.inbox_empty_body),
                        action = stringResource(Res.string.go_live),
                        onAction = onGoLive,
                    )
                }
            } else {
                items(state.items, key = { it.id }) { item ->
                    val summaryId = item.summaryId
                    NotificationRow(
                        item,
                        onClick = summaryId?.let { id -> { onOpenSummary(id) } },
                        modifier = Modifier.cascadeIn(cascade, item.id),
                    )
                    Spacer(Modifier.height(dimens.sm))
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: InboxItem, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val fresh = item.fresh
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val (glyph, gloss) = when (item.kind) {
        NotificationKind.WELCOME -> Glyphs.Sparkles to Gloss.Violet
        NotificationKind.LEVEL_UP -> Glyphs.Trophy to Gloss.Gold
        NotificationKind.SESSION -> Glyphs.Wave to Gloss.Cyan
        else -> Glyphs.Bell to Gloss.Slate
    }
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.98f, onClick = onClick) else Modifier)
            .clip(Pulse.shapes.card)
            .background(if (fresh) colors.surfaceElevated else colors.surface)
            .border(dimens.hairline, if (fresh) colors.borderStrong else colors.border, Pulse.shapes.card)
            .padding(dimens.md),
    ) {
        GlossyIcon(glyph, gloss, size = dimens.tileMd)
        Spacer(Modifier.width(dimens.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.title, style = Pulse.type.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
                if (fresh) Box(Modifier.size(dimens.badgeDot).clip(CircleShape).background(colors.cyan))
            }
            Spacer(Modifier.height(dimens.xxs))
            Text(item.body, style = Pulse.type.bodySmall, color = colors.textSecondary)
            Spacer(Modifier.height(dimens.xs))
            Text(timeAgoText(item.ageHours), style = Pulse.type.meta, color = colors.textMuted)
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun InboxPreview() = PreviewContainer {
    AtmosphereBackground { InboxContent(sampleInbox(), {}, {}) }
}

@ScreenPreviews
@Composable
private fun InboxEmptyPreview() = PreviewContainer { AtmosphereBackground { InboxContent(InboxUiState(), {}, {}) } }

@ComponentPreviews
@Composable
private fun NotificationRowPreview() = PreviewContainer {
    NotificationRow(sampleInbox().items.first(), onClick = {}, modifier = Modifier.padding(Pulse.dimens.gutter))
}

private fun sampleInbox() = InboxUiState(
    SampleData.notifications.map {
        InboxItem(it.id, it.kind, it.title, it.body, fresh = it.id == "n-recap", ageHours = ((SampleData.NOW - it.createdAtEpochMs) / 3_600_000L).toInt(), InboxViewModel.summaryIdOf(it))
    }.toImmutableList(),
)
