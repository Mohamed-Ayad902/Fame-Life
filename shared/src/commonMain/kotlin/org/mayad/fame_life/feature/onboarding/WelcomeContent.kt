package org.mayad.fame_life.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.app_tagline
import famelife.shared.generated.resources.welcome_body
import famelife.shared.generated.resources.welcome_disclosure
import famelife.shared.generated.resources.welcome_get_started
import famelife.shared.generated.resources.welcome_have_account
import famelife.shared.generated.resources.welcome_sample_comment_1
import famelife.shared.generated.resources.welcome_sample_comment_2
import famelife.shared.generated.resources.welcome_sample_comment_3
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.LiveBadge
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.SecondaryButton
import org.mayad.fame_life.core.designsystem.components.SplitPanes
import org.mayad.fame_life.core.designsystem.components.ViewerPill
import org.mayad.fame_life.core.designsystem.icons.GiftArt
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.PulseAspect
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec

/** The welcome card's tilt, in degrees. */
private const val STAGE_TILT = -4f
private const val STAGE_WIDTH_FRACTION = 0.68f
private const val WELCOME_ART = 42


@Composable
internal fun WelcomeContent(onContinue: () -> Unit, onSignIn: () -> Unit) {
    val padding = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(Pulse.dimens.gutter)
    if (Pulse.width.split) {
        SplitPanes(
            padding,
            leading = { WelcomeStage(Modifier.fillMaxHeight()) },
            trailing = { Column { WelcomeCopy(onContinue, onSignIn) } },
        )
    } else {
        Column(padding) {
            WelcomeStage(Modifier.fillMaxWidth().weight(1f))
            WelcomeCopy(onContinue, onSignIn)
        }
    }
}

/** A small stage of what's to come: a tilted live card with floating comments and gifts around it. */
@Composable
private fun WelcomeStage(modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth(STAGE_WIDTH_FRACTION)
                .aspectRatio(PulseAspect.Phone)
                .rotate(STAGE_TILT)
                .clip(Pulse.shapes.hero)
                .border(dimens.hairline, colors.borderStrong, Pulse.shapes.hero),
        ) {
            Artwork(ArtSpec(3, ArtMotif.PORTRAIT, WELCOME_ART), Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to colors.mediaScrim)))
            Row(Modifier.padding(dimens.md), horizontalArrangement = Arrangement.spacedBy(dimens.sm)) {
                LiveBadge()
                ViewerPill(12_480)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(dimens.md), verticalArrangement = Arrangement.spacedBy(dimens.xs)) {
                FloatingComment(stringResource(Res.string.welcome_sample_comment_1))
                FloatingComment(stringResource(Res.string.welcome_sample_comment_2))
                FloatingComment(stringResource(Res.string.welcome_sample_comment_3))
            }
        }
        GiftArt("crown", Modifier.align(Alignment.TopEnd).offset(x = -dimens.md, y = dimens.xl), size = dimens.tileXxl)
        GiftArt("heart", Modifier.align(Alignment.CenterStart).offset(x = dimens.xs, y = dimens.xxxl), size = dimens.tileXl)
        GiftArt("spark", Modifier.align(Alignment.BottomEnd).offset(x = -dimens.xl, y = -dimens.xxl), size = dimens.tileLg)
    }
}

@Composable
private fun ColumnScope.WelcomeCopy(onContinue: () -> Unit, onSignIn: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val cascade = rememberCascadeState()
    Text(stringResource(Res.string.app_tagline), style = Pulse.type.display, color = colors.textPrimary, modifier = Modifier.cascadeIn(cascade, "tagline"))
    Spacer(Modifier.height(dimens.md))
    Text(stringResource(Res.string.welcome_body), style = Pulse.type.body, color = colors.textSecondary, modifier = Modifier.cascadeIn(cascade, "body"))
    Spacer(Modifier.height(dimens.md))
    Text(
        stringResource(Res.string.welcome_disclosure, AppConfig.APP_NAME),
        style = Pulse.type.bodySmall,
        color = colors.textMuted,
        modifier = Modifier.cascadeIn(cascade, "disclosure"),
    )
    Spacer(Modifier.height(dimens.xl))
    PrimaryButton(stringResource(Res.string.welcome_get_started), onContinue, Modifier.fillMaxWidth().cascadeIn(cascade, "start"))
    Spacer(Modifier.height(dimens.sm))
    SecondaryButton(stringResource(Res.string.welcome_have_account), onSignIn, Modifier.fillMaxWidth().cascadeIn(cascade, "signin"))
    Spacer(Modifier.height(dimens.sm))
}

@Composable
private fun FloatingComment(text: String) {
    Text(
        text,
        style = Pulse.type.bodySmall,
        color = Pulse.colors.onMedia,
        modifier = Modifier
            .clip(Pulse.shapes.bubble)
            .background(Pulse.colors.glass)
            .padding(horizontal = Pulse.dimens.sm, vertical = Pulse.dimens.xs),
    )
}

@ScreenPreviews
@Composable
private fun WelcomePreview() = PreviewContainer { AtmosphereBackground { WelcomeContent({}, {}) } }
