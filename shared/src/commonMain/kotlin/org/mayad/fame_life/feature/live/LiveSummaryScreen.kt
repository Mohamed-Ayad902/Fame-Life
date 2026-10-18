package org.mayad.fame_life.feature.live

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.summary_about_you
import famelife.shared.generated.resources.summary_again
import famelife.shared.generated.resources.summary_back_home
import famelife.shared.generated.resources.summary_comments
import famelife.shared.generated.resources.summary_done
import famelife.shared.generated.resources.summary_duration
import famelife.shared.generated.resources.summary_gifts
import famelife.shared.generated.resources.summary_milestones
import famelife.shared.generated.resources.summary_missing_body
import famelife.shared.generated.resources.summary_missing_title
import famelife.shared.generated.resources.summary_moment
import famelife.shared.generated.resources.summary_peak
import famelife.shared.generated.resources.summary_points
import famelife.shared.generated.resources.summary_quote
import famelife.shared.generated.resources.summary_reactions
import famelife.shared.generated.resources.summary_share
import famelife.shared.generated.resources.summary_share_text
import famelife.shared.generated.resources.summary_simulated_results
import famelife.shared.generated.resources.summary_top_gift
import famelife.shared.generated.resources.summary_wrap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.components.AnimatedCount
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.EmptyState
import org.mayad.fame_life.core.designsystem.components.GlassIconButton
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.SecondaryButton
import org.mayad.fame_life.core.designsystem.components.SimulatedTag
import org.mayad.fame_life.core.designsystem.components.SplitPanes
import org.mayad.fame_life.core.designsystem.icons.GiftArt
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.platform.shareText
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.core.ui.durationText
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.domain.progression.FameProgression
import org.mayad.fame_life.domain.simulation.GiftCatalog
import org.mayad.fame_life.domain.simulation.SessionSummary
import org.mayad.fame_life.feature.profile.LevelCard

private const val STATS_PER_ROW = 3
private const val HERO_START_SCALE = 0.92f

/** Tier 1: finds the recap, logs the view, shares and navigates. Back always returns home, never into the live. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LiveSummaryScreen(sessionId: String, navigator: Navigator) {
    val container = LocalAppContainer.current
    val sessions = container.history.sessions.collectAsState().value
    val summary = remember(sessions, sessionId) { sessions.firstOrNull { it.id == sessionId } }
    val profile = container.profiles.profile.collectAsState().value
    LaunchedEffect(sessionId) { container.analytics.log(AnalyticsEvent.LiveSummaryViewed) }
    BackHandler { navigator.popToMain() }
    // Shared text always says the audience was simulated, so it can't be passed off as real reach.
    val shareMessage = summary?.let {
        stringResource(Res.string.summary_share_text, AppConfig.APP_NAME, durationText(it.durationSeconds), compactCount(it.peakViewers), it.comments)
    }
    LiveSummaryContent(
        summary = summary,
        profile = profile,
        onDone = { navigator.popToMain() },
        onAgain = {
            container.analytics.log(AnalyticsEvent.LiveRestarted)
            navigator.popToMain()
            navigator.push(Route.LiveSetup(summary?.title.orEmpty()))
        },
        onShare = {
            container.analytics.log(AnalyticsEvent.SummaryShared)
            shareMessage?.let(::shareText)
        },
    )
}

/** Tier 2: stateless recap. Wide windows put the hero and level beside the stats. */
@Composable
fun LiveSummaryContent(summary: SessionSummary?, profile: CreatorProfile?, onDone: () -> Unit, onAgain: () -> Unit, onShare: () -> Unit) {
    val dimens = Pulse.dimens
    AtmosphereBackground(accent = Pulse.colors.gold) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(dimens.lg)) {
                GlassIconButton(Glyphs.Close, contentDescription = stringResource(Res.string.summary_done), onClick = onDone)
            }
            if (summary == null) {
                EmptyState(
                    Glyphs.Clock,
                    Gloss.Slate,
                    title = stringResource(Res.string.summary_missing_title),
                    body = stringResource(Res.string.summary_missing_body),
                    action = stringResource(Res.string.summary_back_home),
                    onAction = onDone,
                )
                return@Column
            }
            val cascade = rememberCascadeState()
            val hero: @Composable ColumnScope.() -> Unit = {
                SummaryHero(summary)
                if (profile != null) {
                    Spacer(Modifier.height(dimens.lg))
                    LevelCard(FameProgression.progress(profile.famePoints), Modifier.fillMaxWidth().cascadeIn(cascade, "level"))
                }
            }
            val details: @Composable ColumnScope.() -> Unit = {
                StatGrid(summary, cascade)
                summary.topGiftId?.let(GiftCatalog::byId)?.let { gift ->
                    Spacer(Modifier.height(dimens.lg))
                    Row(Modifier.cascadeIn(cascade, "gift").summaryPanel(), verticalAlignment = Alignment.CenterVertically) {
                        val name = gift.label()
                        GiftArt(gift.id, size = dimens.tileXl, name = name)
                        Spacer(Modifier.width(dimens.lg))
                        Column {
                            Text(stringResource(Res.string.summary_top_gift), style = Pulse.type.meta, color = Pulse.colors.textMuted)
                            Text(name, style = Pulse.type.title, color = Pulse.colors.textPrimary)
                        }
                    }
                }
                summary.highlight?.let { quote ->
                    Spacer(Modifier.height(dimens.lg))
                    Column(Modifier.cascadeIn(cascade, "moment").summaryPanel()) {
                        Text(stringResource(Res.string.summary_moment), style = Pulse.type.meta, color = Pulse.colors.textMuted)
                        Spacer(Modifier.height(dimens.xs))
                        Text(stringResource(Res.string.summary_quote, quote), style = Pulse.type.title, color = Pulse.colors.textPrimary)
                    }
                }
                Spacer(Modifier.height(dimens.xxl))
            }
            if (Pulse.width.split) {
                SplitPanes(
                    Modifier.weight(1f).padding(horizontal = dimens.gutter),
                    leading = { Column(horizontalAlignment = Alignment.CenterHorizontally, content = hero) },
                    trailing = { Column(Modifier.verticalScroll(rememberScrollState()), content = details) },
                )
            } else {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.gutter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    hero()
                    Spacer(Modifier.height(dimens.xl))
                    details()
                }
            }
            SummaryActions(onAgain, onShare, onDone)
        }
    }
}

@Composable
private fun Modifier.summaryPanel(): Modifier {
    val colors = Pulse.colors
    return fillMaxWidth()
        .clip(Pulse.shapes.panel)
        .background(colors.surface)
        .border(Pulse.dimens.hairline, colors.border, Pulse.shapes.panel)
        .padding(Pulse.dimens.lg)
}

@Composable
private fun SummaryActions(onAgain: () -> Unit, onShare: () -> Unit, onDone: () -> Unit) {
    val dimens = Pulse.dimens
    Column(Modifier.padding(horizontal = dimens.gutter, vertical = dimens.md)) {
        PrimaryButton(
            stringResource(Res.string.summary_again),
            onClick = onAgain,
            leading = Glyphs.Bolt,
            brush = Pulse.colors.liveGradient,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(dimens.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.sm)) {
            SecondaryButton(stringResource(Res.string.summary_share), onClick = onShare, leading = Glyphs.Share, modifier = Modifier.weight(1f))
            SecondaryButton(stringResource(Res.string.summary_done), onClick = onDone, modifier = Modifier.weight(1f))
        }
    }
}

/** The reveal: the headline rises in slowly enough to land, then the count eases up to the peak. */
@Composable
private fun SummaryHero(summary: SessionSummary) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val reduce = Pulse.reducedMotion
    val reveal = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, Motion.reveal()) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            alpha = reveal.value
            val scale = HERO_START_SCALE + reveal.value * (1f - HERO_START_SCALE)
            scaleX = scale
            scaleY = scale
        },
    ) {
        SimulatedTag(text = stringResource(Res.string.summary_simulated_results))
        Spacer(Modifier.height(dimens.md))
        Text(stringResource(Res.string.summary_wrap), style = Pulse.type.display, color = colors.textPrimary, textAlign = TextAlign.Center)
        if (summary.title.isNotBlank()) {
            Text(summary.title, style = Pulse.type.body, color = colors.textSecondary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(dimens.lg))
        Text(stringResource(Res.string.summary_peak), style = Pulse.type.overline, color = colors.textMuted)
        AnimatedCount(summary.peakViewers, Pulse.type.metric, colors.textPrimary)
        Spacer(Modifier.height(dimens.sm))
        Box(Modifier.clip(Pulse.shapes.pill).background(colors.goldGradient).padding(horizontal = dimens.md, vertical = dimens.xs)) {
            Text(stringResource(Res.string.summary_points, summary.famePointsEarned), style = Pulse.type.label, color = colors.background)
        }
    }
}

@Composable
private fun StatGrid(summary: SessionSummary, cascade: org.mayad.fame_life.core.designsystem.motion.CascadeState) {
    val stats: List<Pair<String, StringResource>> = listOf(
        durationText(summary.durationSeconds) to Res.string.summary_duration,
        compactCount(summary.comments) to Res.string.summary_comments,
        compactCount(summary.contextualComments) to Res.string.summary_about_you,
        compactCount(summary.gifts) to Res.string.summary_gifts,
        compactCount(summary.reactions) to Res.string.summary_reactions,
        summary.milestones.toString() to Res.string.summary_milestones,
    )
    Column(verticalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
        stats.chunked(STATS_PER_ROW).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
                row.forEachIndexed { i, (value, label) ->
                    StatCell(value, stringResource(label), Modifier.weight(1f).cascadeIn(cascade, "stat-$rowIndex-$i"))
                }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier) {
    val colors = Pulse.colors
    Column(
        modifier
            .clip(Pulse.shapes.control)
            .background(colors.surface)
            .border(Pulse.dimens.hairline, colors.border, Pulse.shapes.control)
            .padding(vertical = Pulse.dimens.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = Pulse.type.metricSmall, color = colors.textPrimary, maxLines = 1)
        Text(label, style = Pulse.type.meta, color = colors.textMuted, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun LiveSummaryPreview() = PreviewContainer { LiveSummaryContent(SampleData.summary, SampleData.profile, {}, {}, {}) }

@ScreenPreviews
@Composable
private fun LiveSummaryMissingPreview() = PreviewContainer { LiveSummaryContent(null, null, {}, {}, {}) }
