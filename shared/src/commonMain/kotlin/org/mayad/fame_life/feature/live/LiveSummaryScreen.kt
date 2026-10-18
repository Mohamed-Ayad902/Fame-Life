package org.mayad.fame_life.feature.live

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
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
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GiftArt
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.format.durationLabel
import org.mayad.fame_life.core.platform.shareText
import org.mayad.fame_life.domain.progression.FameProgression
import org.mayad.fame_life.domain.simulation.GiftCatalog
import org.mayad.fame_life.domain.simulation.SessionSummary
import org.mayad.fame_life.feature.profile.LevelCard

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LiveSummaryScreen(sessionId: String, navigator: Navigator) {
    val container = LocalAppContainer.current
    val sessions = container.history.sessions.collectAsState().value
    val summary = remember(sessions, sessionId) { sessions.firstOrNull { it.id == sessionId } }
    val profile = container.profiles.profile.collectAsState().value
    LaunchedEffect(sessionId) { container.analytics.log(AnalyticsEvent.LiveSummaryViewed) }
    BackHandler { navigator.popToMain() }

    AtmosphereBackground(accent = Pulse.colors.gold) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(Pulse.dimens.lg)) {
                GlassIconButton(Glyphs.Close, contentDescription = "Done", onClick = { navigator.popToMain() })
            }
            if (summary == null) {
                EmptyState(
                    Glyphs.Clock,
                    Gloss.Slate,
                    title = "This recap isn't here anymore",
                    body = "Older lives are removed when your history is full.",
                    action = "Back to home",
                    onAction = { navigator.popToMain() },
                )
                return@Column
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Pulse.dimens.gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SummaryHero(summary)
                Spacer(Modifier.height(Pulse.dimens.xl))
                StatGrid(summary)
                summary.topGiftId?.let(GiftCatalog::byId)?.let { gift ->
                    Spacer(Modifier.height(Pulse.dimens.lg))
                    Row(
                        Modifier.fillMaxWidth().clip(Pulse.shapes.panel).background(Pulse.colors.surface).border(1.dp, Pulse.colors.border, Pulse.shapes.panel).padding(Pulse.dimens.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GiftArt(gift.id, size = 64.dp, name = gift.name)
                        Spacer(Modifier.width(Pulse.dimens.lg))
                        Column {
                            Text("Top gift", style = Pulse.type.meta, color = Pulse.colors.textMuted)
                            Text(gift.name, style = Pulse.type.title, color = Pulse.colors.textPrimary)
                        }
                    }
                }
                summary.highlight?.let { quote ->
                    Spacer(Modifier.height(Pulse.dimens.lg))
                    Column(
                        Modifier.fillMaxWidth().clip(Pulse.shapes.panel).background(Pulse.colors.surface).border(1.dp, Pulse.colors.border, Pulse.shapes.panel).padding(Pulse.dimens.lg),
                    ) {
                        Text("Moment of the live", style = Pulse.type.meta, color = Pulse.colors.textMuted)
                        Spacer(Modifier.height(Pulse.dimens.xs))
                        Text("“$quote”", style = Pulse.type.title, color = Pulse.colors.textPrimary)
                    }
                }
                if (profile != null) {
                    Spacer(Modifier.height(Pulse.dimens.lg))
                    LevelCard(FameProgression.progress(profile.famePoints), Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(Pulse.dimens.xxl))
            }
            Column(Modifier.padding(horizontal = Pulse.dimens.gutter, vertical = Pulse.dimens.md)) {
                PrimaryButton(
                    "Go live again",
                    onClick = {
                        container.analytics.log(AnalyticsEvent.LiveRestarted)
                        navigator.popToMain()
                        navigator.push(Route.LiveSetup(summary.title))
                    },
                    leading = Glyphs.Bolt,
                    brush = Pulse.colors.liveGradient,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Pulse.dimens.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
                    SecondaryButton(
                        "Share recap",
                        onClick = {
                            container.analytics.log(AnalyticsEvent.SummaryShared)
                            shareText(shareMessage(summary))
                        },
                        leading = Glyphs.Share,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton("Done", onClick = { navigator.popToMain() }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Shared text always says the audience was simulated, so it can't be passed off as real reach. */
private fun shareMessage(summary: SessionSummary) =
    "I just went live in ${AppConfig.APP_NAME} for ${durationLabel(summary.durationSeconds)}: " +
        "${compactCount(summary.peakViewers)} peak viewers and ${summary.comments} comments in a simulated live. " +
        "(The audience is AI-simulated, not real people.)"

@Composable
private fun SummaryHero(summary: SessionSummary) {
    val colors = Pulse.colors
    val reduce = Pulse.reducedMotion
    val reveal = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, Motion.enter()) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            alpha = reveal.value
            val scale = 0.92f + reveal.value * 0.08f
            scaleX = scale
            scaleY = scale
        },
    ) {
        SimulatedTag(text = "Simulated results")
        Spacer(Modifier.height(Pulse.dimens.md))
        Text("That's a wrap", style = Pulse.type.display, color = colors.textPrimary, textAlign = TextAlign.Center)
        if (summary.title.isNotBlank()) {
            Text(summary.title, style = Pulse.type.body, color = colors.textSecondary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(Pulse.dimens.lg))
        Text("PEAK VIEWERS", style = Pulse.type.overline, color = colors.textMuted)
        AnimatedCount(summary.peakViewers, Pulse.type.metric, colors.textPrimary)
        Spacer(Modifier.height(Pulse.dimens.sm))
        Box(Modifier.clip(Pulse.shapes.pill).background(colors.goldGradient).padding(horizontal = Pulse.dimens.md, vertical = 4.dp)) {
            Text("+${summary.famePointsEarned} fame points", style = Pulse.type.label, color = colors.background)
        }
    }
}

@Composable
private fun StatGrid(summary: SessionSummary) {
    val stats = listOf(
        durationLabel(summary.durationSeconds) to "Duration",
        compactCount(summary.comments) to "Comments",
        compactCount(summary.contextualComments) to "About you",
        compactCount(summary.gifts) to "Gifts",
        compactCount(summary.reactions) to "Reactions",
        summary.milestones.toString() to "Milestones",
    )
    Column(verticalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
        stats.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
                row.forEach { (value, label) -> StatCell(value, label, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier) {
    val colors = Pulse.colors
    Column(
        modifier.clip(Pulse.shapes.control).background(colors.surface).border(1.dp, colors.border, Pulse.shapes.control).padding(vertical = Pulse.dimens.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = Pulse.type.metricSmall, color = colors.textPrimary, maxLines = 1)
        Text(label, style = Pulse.type.meta, color = colors.textMuted, maxLines = 1)
    }
}
