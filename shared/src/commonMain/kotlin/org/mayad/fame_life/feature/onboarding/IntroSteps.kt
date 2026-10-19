package org.mayad.fame_life.feature.onboarding

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.honesty_audience_body
import famelife.shared.generated.resources.honesty_audience_title
import famelife.shared.generated.resources.honesty_button
import famelife.shared.generated.resources.honesty_camera_body
import famelife.shared.generated.resources.honesty_camera_title
import famelife.shared.generated.resources.honesty_eyebrow
import famelife.shared.generated.resources.honesty_numbers_body
import famelife.shared.generated.resources.honesty_numbers_title
import famelife.shared.generated.resources.honesty_speech_body
import famelife.shared.generated.resources.honesty_speech_title
import famelife.shared.generated.resources.honesty_title
import famelife.shared.generated.resources.how_eyebrow
import famelife.shared.generated.resources.how_listen_body
import famelife.shared.generated.resources.how_listen_title
import famelife.shared.generated.resources.how_react_body
import famelife.shared.generated.resources.how_react_title
import famelife.shared.generated.resources.how_talk_body
import famelife.shared.generated.resources.how_talk_title
import famelife.shared.generated.resources.how_title
import famelife.shared.generated.resources.intro_next
import famelife.shared.generated.resources.intro_step_of
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.SplitPanes
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse

@Immutable
private data class Beat(val glyph: ImageVector, val gloss: Gloss, val title: String, val body: String)

/** Step 2: what actually happens in a live, as three linked beats. */
@Composable
internal fun HowItWorksStep(step: Int, total: Int, onContinue: () -> Unit) {
    val beats = persistentListOf(
        Beat(Glyphs.Mic, Gloss.Violet, stringResource(Res.string.how_talk_title), stringResource(Res.string.how_talk_body)),
        Beat(Glyphs.Sparkles, Gloss.Cyan, stringResource(Res.string.how_listen_title), stringResource(Res.string.how_listen_body)),
        Beat(Glyphs.Gift, Gloss.Gold, stringResource(Res.string.how_react_title), stringResource(Res.string.how_react_body)),
    )
    IntroScaffold(
        step,
        total,
        eyebrow = stringResource(Res.string.how_eyebrow),
        title = stringResource(Res.string.how_title),
        button = stringResource(Res.string.intro_next),
        beats = beats,
        connected = true,
        onContinue = onContinue,
    )
}

/** Step 3: the honest part, said plainly before an account exists. */
@Composable
internal fun HonestyStep(step: Int, total: Int, onContinue: () -> Unit) {
    val points = persistentListOf(
        Beat(Glyphs.Users, Gloss.Violet, stringResource(Res.string.honesty_audience_title), stringResource(Res.string.honesty_audience_body, AppConfig.APP_NAME)),
        Beat(Glyphs.Eye, Gloss.Blue, stringResource(Res.string.honesty_camera_title), stringResource(Res.string.honesty_camera_body)),
        Beat(Glyphs.Comment, Gloss.Cyan, stringResource(Res.string.honesty_speech_title), stringResource(Res.string.honesty_speech_body)),
        Beat(Glyphs.Star, Gloss.Gold, stringResource(Res.string.honesty_numbers_title), stringResource(Res.string.honesty_numbers_body)),
    )
    IntroScaffold(
        step,
        total,
        eyebrow = stringResource(Res.string.honesty_eyebrow),
        title = stringResource(Res.string.honesty_title),
        button = stringResource(Res.string.honesty_button),
        beats = points,
        connected = false,
        onContinue = onContinue,
    )
}

/** Portrait: heading, beats, button stacked. Wide windows: heading and button on the start pane, beats beside. */
@Composable
private fun IntroScaffold(
    step: Int,
    total: Int,
    eyebrow: String,
    title: String,
    button: String,
    beats: ImmutableList<Beat>,
    connected: Boolean,
    onContinue: () -> Unit,
) {
    val dimens = Pulse.dimens
    val cascade = rememberCascadeState()
    val outer = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(dimens.gutter)
    val beatList: @Composable ColumnScope.() -> Unit = {
        beats.forEachIndexed { index, beat ->
            BeatRow(beat, connector = connected && index < beats.lastIndex, modifier = Modifier.cascadeIn(cascade, beat.title))
        }
    }
    if (Pulse.width.split) {
        SplitPanes(
            outer,
            leading = {
                Column {
                    IntroHeading(step, total, eyebrow, title)
                    Spacer(Modifier.height(dimens.xl))
                    PrimaryButton(button, onContinue, Modifier.fillMaxWidth())
                }
            },
            trailing = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(dimens.md), content = beatList)
            },
        )
    } else {
        Column(outer) {
            IntroHeading(step, total, eyebrow, title)
            Spacer(Modifier.height(dimens.xl))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(dimens.md), content = beatList)
            Spacer(Modifier.height(dimens.lg))
            PrimaryButton(button, onContinue, Modifier.fillMaxWidth())
            Spacer(Modifier.height(dimens.sm))
        }
    }
}

@Composable
private fun IntroHeading(step: Int, total: Int, eyebrow: String, title: String) {
    Column {
        StepDots(step, total)
        Spacer(Modifier.height(Pulse.dimens.xl))
        Text(eyebrow, style = Pulse.type.overline, color = Pulse.colors.cyan)
        Spacer(Modifier.height(Pulse.dimens.xs))
        Text(title, style = Pulse.type.display, color = Pulse.colors.textPrimary)
    }
}

@Composable
private fun BeatRow(beat: Beat, connector: Boolean, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(
        modifier
            .fillMaxWidth()
            .clip(Pulse.shapes.card)
            .background(colors.surface.copy(alpha = 0.72f))
            .border(dimens.hairline, colors.border, Pulse.shapes.card)
            .padding(dimens.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GlossyIcon(beat.glyph, beat.gloss, size = dimens.tileLg)
            if (connector) {
                // A short lit thread hinting that each beat feeds the next.
                Spacer(Modifier.height(dimens.xs))
                Box(
                    Modifier
                        .size(width = dimens.ring, height = dimens.lg)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(beat.gloss.glow.copy(alpha = 0.8f), beat.gloss.glow.copy(alpha = 0f)))),
                )
            }
        }
        Spacer(Modifier.width(dimens.lg))
        Column(Modifier.weight(1f)) {
            Text(beat.title, style = Pulse.type.title, color = colors.textPrimary)
            Spacer(Modifier.height(dimens.xs))
            Text(beat.body, style = Pulse.type.bodySmall, color = colors.textSecondary)
        }
    }
}

/** Progress through the intro: the current step is a longer lit capsule. */
@Composable
internal fun StepDots(step: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val description = stringResource(Res.string.intro_step_of, step + 1, total)
    Row(modifier.semantics { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy(dimens.xs)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(width = if (i == step) dimens.xl else dimens.dot, height = dimens.dot)
                    .clip(CircleShape)
                    .background(if (i <= step) colors.cyan else colors.borderStrong),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun HowItWorksPreview() = PreviewContainer { AtmosphereBackground { HowItWorksStep(1, 4) {} } }

@ScreenPreviews
@Composable
private fun HonestyPreview() = PreviewContainer { AtmosphereBackground { HonestyStep(2, 4) {} } }

@ComponentPreviews
@Composable
private fun StepDotsPreview() = PreviewContainer { StepDots(1, 4, Modifier.padding(Pulse.dimens.gutter)) }
