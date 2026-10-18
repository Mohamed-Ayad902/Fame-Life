package org.mayad.fame_life.feature.onboarding

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse

private data class Beat(val glyph: ImageVector, val gloss: Gloss, val title: String, val body: String)

/** Step 2: what actually happens in a live, as three linked beats. */
@Composable
internal fun HowItWorksStep(step: Int, total: Int, onContinue: () -> Unit) {
    val beats = listOf(
        Beat(Glyphs.Mic, Gloss.Violet, "You talk", "Go live with your camera and just speak. Stories, jokes, a question for the room."),
        Beat(Glyphs.Sparkles, Gloss.Cyan, "The audience listens", "Your phone turns speech into text. The AI audience reads what you mean, not your voice."),
        Beat(Glyphs.Gift, Gloss.Gold, "The room reacts", "Comments, questions, gifts and viewer spikes follow the moment, then a recap when you end."),
    )
    IntroScaffold(step, total, eyebrow = "HOW A LIVE WORKS", title = "Talk. They listen.\nThe room reacts.", button = "Next", onContinue = onContinue) {
        beats.forEachIndexed { index, beat ->
            StaggeredBeat(index) {
                BeatRow(beat, connector = index < beats.lastIndex)
            }
        }
    }
}

/** Step 3: the honest part, said plainly before an account exists. */
@Composable
internal fun HonestyStep(step: Int, total: Int, onContinue: () -> Unit) {
    val points = listOf(
        Beat(Glyphs.Users, Gloss.Violet, "The audience is simulated", "No real people watch. Viewers, comments, gifts and followers are made by ${AppConfig.APP_NAME}."),
        Beat(Glyphs.Eye, Gloss.Blue, "Your camera stays on your phone", "Video is shown only to you. It's never recorded, uploaded or broadcast."),
        Beat(Glyphs.Comment, Gloss.Cyan, "Only speech text may leave", "Raw audio is never stored. When the cloud audience is on, short bits of text go to our server, nowhere else."),
        Beat(Glyphs.Star, Gloss.Gold, "Numbers are for fun", "Fame levels and counts live inside the app. They aren't real reach, and we never post for you."),
    )
    IntroScaffold(step, total, eyebrow = "BEFORE YOU START", title = "Real feelings,\nsimulated crowd.", button = "Sounds good", onContinue = onContinue) {
        points.forEachIndexed { index, point ->
            StaggeredBeat(index) {
                BeatRow(point, connector = false)
            }
        }
    }
}

@Composable
private fun IntroScaffold(
    step: Int,
    total: Int,
    eyebrow: String,
    title: String,
    button: String,
    onContinue: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = Pulse.colors
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(Pulse.dimens.gutter)) {
        StepDots(step, total)
        Spacer(Modifier.height(Pulse.dimens.xl))
        Text(eyebrow, style = Pulse.type.overline, color = colors.cyan)
        Spacer(Modifier.height(Pulse.dimens.xs))
        Text(title, style = Pulse.type.display, color = colors.textPrimary)
        Spacer(Modifier.height(Pulse.dimens.xl))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Pulse.dimens.md),
        ) { content() }
        Spacer(Modifier.height(Pulse.dimens.lg))
        PrimaryButton(button, onContinue, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Pulse.dimens.sm))
    }
}

@Composable
private fun BeatRow(beat: Beat, connector: Boolean) {
    val colors = Pulse.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Pulse.shapes.card)
            .background(colors.surface.copy(alpha = 0.72f))
            .border(1.dp, colors.border, Pulse.shapes.card)
            .padding(Pulse.dimens.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GlossyIcon(beat.glyph, beat.gloss, size = 52.dp)
            if (connector) {
                // A short lit thread hinting that each beat feeds the next.
                Spacer(Modifier.height(Pulse.dimens.xs))
                Box(
                    Modifier
                        .size(width = 2.dp, height = 18.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(beat.gloss.glow.copy(alpha = 0.8f), beat.gloss.glow.copy(alpha = 0f)))),
                )
            }
        }
        Spacer(Modifier.width(Pulse.dimens.lg))
        Column(Modifier.weight(1f)) {
            Text(beat.title, style = Pulse.type.title, color = colors.textPrimary)
            Spacer(Modifier.height(Pulse.dimens.xs))
            Text(beat.body, style = Pulse.type.bodySmall, color = colors.textSecondary)
        }
    }
}

/** Rises each item in after the previous one; shown immediately when motion is reduced. */
@Composable
private fun StaggeredBeat(index: Int, content: @Composable () -> Unit) {
    val reduceMotion = Pulse.reducedMotion
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(Motion.FAST.toLong() * index)
        progress.animateTo(1f, Motion.enter())
    }
    Box(
        Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 24.dp.toPx()
        },
    ) { content() }
}

/** Progress through the intro: the current step is a longer lit capsule. */
@Composable
internal fun StepDots(step: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    Row(
        modifier.semantics { contentDescription = "Step ${step + 1} of $total" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(width = if (i == step) 22.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(if (i <= step) colors.cyan else colors.borderStrong),
            )
        }
    }
}
