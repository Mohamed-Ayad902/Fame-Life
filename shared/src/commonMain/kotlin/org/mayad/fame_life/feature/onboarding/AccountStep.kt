package org.mayad.fame_life.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.account_body_new
import famelife.shared.generated.resources.account_body_returning
import famelife.shared.generated.resources.account_continue
import famelife.shared.generated.resources.account_opening
import famelife.shared.generated.resources.account_title_new
import famelife.shared.generated.resources.account_title_returning
import famelife.shared.generated.resources.account_what_we_use
import famelife.shared.generated.resources.account_what_we_use_body
import famelife.shared.generated.resources.common_sign_out
import famelife.shared.generated.resources.common_try_again
import famelife.shared.generated.resources.sync_error_body
import famelife.shared.generated.resources.sync_error_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.SecondaryButton
import org.mayad.fame_life.core.designsystem.components.SplitPanes
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.ui.messageRes
import org.mayad.fame_life.domain.account.AuthErrorKind

/** Stateless: everything the account step shows comes in as parameters, so it previews in any state. */
@Composable
fun AccountStepContent(
    step: Int,
    total: Int,
    returning: Boolean,
    busy: Boolean,
    message: StringResource?,
    enabled: Boolean,
    onContinue: () -> Unit,
) {
    val outer = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(Pulse.dimens.gutter)
    val heading: @Composable ColumnScope.() -> Unit = {
        StepDots(step, total)
        Spacer(Modifier.height(Pulse.dimens.xl))
        GlossyIcon(Glyphs.Shield, Gloss.Blue, size = Pulse.dimens.tileXl)
        Spacer(Modifier.height(Pulse.dimens.lg))
        Text(
            stringResource(if (returning) Res.string.account_title_returning else Res.string.account_title_new),
            style = Pulse.type.display,
            color = Pulse.colors.textPrimary,
        )
        Spacer(Modifier.height(Pulse.dimens.sm))
        Text(
            stringResource(if (returning) Res.string.account_body_returning else Res.string.account_body_new),
            style = Pulse.type.body,
            color = Pulse.colors.textSecondary,
        )
    }
    val actions: @Composable ColumnScope.() -> Unit = { AccountActions(busy, message, enabled, onContinue) }
    if (Pulse.width.split) {
        SplitPanes(
            outer,
            leading = { Column(content = heading) },
            trailing = { Column(Modifier.verticalScroll(rememberScrollState()), content = actions) },
        )
    } else {
        Column(outer.verticalScroll(rememberScrollState())) {
            heading()
            Spacer(Modifier.height(Pulse.dimens.xl))
            actions()
        }
    }
}

@Composable
private fun ColumnScope.AccountActions(busy: Boolean, message: StringResource?, enabled: Boolean, onContinue: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val cascade = rememberCascadeState()
    Column(
        Modifier
            .fillMaxWidth()
            .cascadeIn(cascade, "what-we-use")
            .clip(Pulse.shapes.card)
            .background(colors.surface.copy(alpha = 0.72f))
            .border(dimens.hairline, colors.border, Pulse.shapes.card)
            .padding(dimens.lg),
        verticalArrangement = Arrangement.spacedBy(dimens.sm),
    ) {
        Text(stringResource(Res.string.account_what_we_use), style = Pulse.type.titleSmall, color = colors.textPrimary)
        Text(stringResource(Res.string.account_what_we_use_body), style = Pulse.type.bodySmall, color = colors.textSecondary)
    }
    if (message != null) {
        Spacer(Modifier.height(dimens.lg))
        Text(
            stringResource(message),
            style = Pulse.type.bodySmall,
            color = colors.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    Spacer(Modifier.height(dimens.xxl))
    PrimaryButton(
        stringResource(if (busy) Res.string.account_opening else Res.string.account_continue),
        onClick = onContinue,
        enabled = enabled && !busy,
        leading = Glyphs.User,
        modifier = Modifier.fillMaxWidth().cascadeIn(cascade, "continue"),
    )
    Spacer(Modifier.height(dimens.sm))
}

/** Signed in, but the account's data could not be fetched and nothing is cached yet. */
@Composable
internal fun SyncErrorScreen(onRetry: () -> Unit, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    AtmosphereBackground(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(dimens.gutter)
                .widthIn(max = dimens.contentMaxWidth)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlossyIcon(Glyphs.Globe, Gloss.Slate, size = dimens.tileXxl)
            Spacer(Modifier.height(dimens.lg))
            Text(stringResource(Res.string.sync_error_title), style = Pulse.type.headline, color = colors.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(dimens.sm))
            Text(stringResource(Res.string.sync_error_body), style = Pulse.type.body, color = colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(dimens.xl))
            PrimaryButton(stringResource(Res.string.common_try_again), onRetry, Modifier.fillMaxWidth())
            Spacer(Modifier.height(dimens.sm))
            SecondaryButton(stringResource(Res.string.common_sign_out), onSignOut, Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun AccountStepPreview() = PreviewContainer {
    AtmosphereBackground { AccountStepContent(3, 4, returning = false, busy = false, message = null, enabled = true, onContinue = {}) }
}

@ScreenPreviews
@Composable
private fun AccountStepErrorPreview() = PreviewContainer {
    AtmosphereBackground {
        AccountStepContent(3, 4, returning = true, busy = false, message = AuthErrorKind.NETWORK.messageRes, enabled = true, onContinue = {})
    }
}

@ScreenPreviews
@Composable
private fun SyncErrorPreview() = PreviewContainer { SyncErrorScreen({}, {}) }
