package org.mayad.fame_life.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.go_live
import famelife.shared.generated.resources.live_audience_size
import famelife.shared.generated.resources.live_close
import famelife.shared.generated.resources.live_dismiss
import famelife.shared.generated.resources.live_fewer_options
import famelife.shared.generated.resources.live_gifts
import famelife.shared.generated.resources.live_gifts_off
import famelife.shared.generated.resources.live_gifts_on
import famelife.shared.generated.resources.live_language
import famelife.shared.generated.resources.live_length
import famelife.shared.generated.resources.live_length_pro
import famelife.shared.generated.resources.live_lives_free
import famelife.shared.generated.resources.live_lives_left
import famelife.shared.generated.resources.live_minutes
import famelife.shared.generated.resources.live_mood
import famelife.shared.generated.resources.live_mood_pro
import famelife.shared.generated.resources.live_more_options
import famelife.shared.generated.resources.live_no_lives
import famelife.shared.generated.resources.live_pace
import famelife.shared.generated.resources.live_preset_pro
import famelife.shared.generated.resources.live_preview
import famelife.shared.generated.resources.live_see_pro
import famelife.shared.generated.resources.live_set_room
import famelife.shared.generated.resources.live_simulated_audience
import famelife.shared.generated.resources.live_size_hint
import famelife.shared.generated.resources.live_studio
import famelife.shared.generated.resources.live_style
import famelife.shared.generated.resources.live_style_description
import famelife.shared.generated.resources.live_style_description_locked
import famelife.shared.generated.resources.live_style_pro
import famelife.shared.generated.resources.live_title_hint
import famelife.shared.generated.resources.live_title_label
import famelife.shared.generated.resources.perm_allow
import famelife.shared.generated.resources.perm_body
import famelife.shared.generated.resources.perm_denied_body
import famelife.shared.generated.resources.perm_title
import famelife.shared.generated.resources.perm_unsupported_body
import famelife.shared.generated.resources.perm_unsupported_title
import famelife.shared.generated.resources.points_go_live_cost
import famelife.shared.generated.resources.points_paying
import famelife.shared.generated.resources.points_style_cost
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.GlassIconButton
import org.mayad.fame_life.core.designsystem.components.LiveBadge
import org.mayad.fame_life.core.designsystem.components.PrimaryButton
import org.mayad.fame_life.core.designsystem.components.PulseChip
import org.mayad.fame_life.core.designsystem.components.PulseTextField
import org.mayad.fame_life.core.designsystem.components.SecondaryButton
import org.mayad.fame_life.core.designsystem.components.SimulatedTag
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.GlossyIcon
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.platform.CameraLens
import org.mayad.fame_life.core.platform.CameraPreview
import org.mayad.fame_life.core.ui.label
import org.mayad.fame_life.core.ui.rangeLabel
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.domain.simulation.AudienceLanguage
import org.mayad.fame_life.domain.simulation.AudiencePreset
import org.mayad.fame_life.domain.simulation.AudienceVibe
import org.mayad.fame_life.domain.simulation.EventFrequency
import org.mayad.fame_life.core.ui.messageRes
import org.mayad.fame_life.domain.economy.EconomyConfig
import org.mayad.fame_life.domain.economy.PointsState
import org.mayad.fame_life.domain.economy.StreakRules
import org.mayad.fame_life.domain.economy.Wallet
import org.mayad.fame_life.feature.points.PointsBalancePill
import org.mayad.fame_life.feature.points.PointsShortPanel
import org.mayad.fame_life.feature.profile.selfAvatar


private const val TITLE_MAX = 60
private const val PANEL_ALPHA = 0.88f

/** What the permission panel should say. */
enum class PermissionNeed { NONE, UNSUPPORTED, NOT_ASKED, DENIED }

/**
 * Tier 2: the creator studio. The backdrop is the camera when allowed, so the user sees exactly how they'll appear.
 * Phones stack the options under the preview; wide windows keep the preview on the start side and the options beside it.
 */
@Composable
fun LiveSetupContent(
    state: LiveSetupUiState,
    permission: PermissionNeed,
    cameraPreview: Boolean,
    onIntent: (LiveSetupIntent) -> Unit,
    onGoLive: () -> Unit,
    onAllow: () -> Unit,
    onClose: () -> Unit,
    onSeePro: () -> Unit,
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val style = LiveStyles.byId(state.draft.styleId)
    Box(Modifier.fillMaxSize().background(colors.background)) {
        StudioBackdrop(cameraPreview, style, selfAvatar(state.avatarPalette ?: 0))
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to colors.glassStrong, 0.35f to colors.glassFaint, 0.62f to colors.mediaScrim, 1f to colors.background),
            ),
        )
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(dimens.lg), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Glyphs.Close, contentDescription = stringResource(Res.string.live_close), onClick = onClose)
                Spacer(Modifier.weight(1f))
                (state.points as? PointsState.Ready)?.let {
                    PointsBalancePill(it.balance)
                    Spacer(Modifier.width(dimens.sm))
                }
                LiveBadge(label = stringResource(Res.string.live_preview))
            }
            val options: @Composable ColumnScope.() -> Unit = {
                SetupOptions(state, permission, style, onIntent, onAllow, onSeePro)
            }
            if (Pulse.width.split) {
                Row(Modifier.weight(1f)) {
                    Spacer(Modifier.weight(1f))
                    Column(
                        Modifier
                            .weight(1f)
                            .widthIn(max = dimens.contentMaxWidth)
                            .fillMaxHeight()
                            .clip(Pulse.shapes.panel)
                            .background(colors.glassStrong)
                            .verticalScroll(rememberScrollState())
                            .padding(dimens.lg),
                        content = options,
                    )
                    Spacer(Modifier.width(dimens.gutter))
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.gutter)) {
                    Spacer(Modifier.height(dimens.setupStageHeight))
                    options()
                }
            }
            GoLiveBar(state, onGoLive, onIntent)
        }
    }
}

@Composable
private fun ColumnScope.SetupOptions(
    state: LiveSetupUiState,
    permission: PermissionNeed,
    style: LiveStyle,
    onIntent: (LiveSetupIntent) -> Unit,
    onAllow: () -> Unit,
    onSeePro: () -> Unit,
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val draft = state.draft
    val limits = state.limits
    val cascade = rememberCascadeState()
    val showMore = state.showMore

    Text(stringResource(Res.string.live_studio), style = Pulse.type.overline, color = style.glow)
    Text(stringResource(Res.string.live_set_room), style = Pulse.type.display, color = colors.textPrimary)
    Spacer(Modifier.height(dimens.lg))
    if (permission != PermissionNeed.NONE) {
        PermissionPanel(permission, onAllow, Modifier.cascadeIn(cascade, "permission"))
        Spacer(Modifier.height(dimens.lg))
    }
    PulseTextField(
        draft.title,
        { onIntent(LiveSetupIntent.Title(it)) },
        stringResource(Res.string.live_title_label),
        placeholder = stringResource(Res.string.live_title_hint),
        maxLength = TITLE_MAX,
        modifier = Modifier.cascadeIn(cascade, "title"),
    )
    Spacer(Modifier.height(dimens.xl))

    Column(Modifier.cascadeIn(cascade, "size")) {
        SetupLabel(stringResource(Res.string.live_audience_size))
        ChipRow {
            AudiencePreset.entries.forEach { preset ->
                val locked = preset !in limits.presets
                PulseChip(
                    preset.label(),
                    selected = preset == draft.preset,
                    locked = locked,
                    leading = if (locked) Glyphs.Lock else Glyphs.Users,
                    onClick = { onIntent(LiveSetupIntent.Preset(preset)) },
                )
            }
        }
        Text(stringResource(Res.string.live_size_hint, draft.preset.rangeLabel()), style = Pulse.type.meta, color = colors.textMuted)
    }
    Spacer(Modifier.height(dimens.lg))

    Column(Modifier.cascadeIn(cascade, "mood")) {
        SetupLabel(stringResource(Res.string.live_mood))
        ChipRow {
            AudienceVibe.entries.forEach { vibe ->
                val locked = vibe !in limits.vibes
                val selected = vibe in draft.vibes
                PulseChip(
                    vibe.label(),
                    selected = selected,
                    locked = locked,
                    leading = if (locked) Glyphs.Lock else null,
                    onClick = { onIntent(LiveSetupIntent.ToggleVibe(vibe)) },
                )
            }
        }
    }
    Spacer(Modifier.height(dimens.lg))

    Column(Modifier.cascadeIn(cascade, "style")) {
        SetupLabel(stringResource(Res.string.live_style))
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.md)) {
            LiveStyles.all.forEach { option ->
                val locked = option.id !in limits.liveStyles
                StyleCard(option, selected = option.id == draft.styleId, locked = locked, cost = state.costOf(option.id)) {
                    onIntent(LiveSetupIntent.Style(option.id))
                }
            }
        }
    }

    // Keeps the last hint while it animates out.
    var lastHint by remember { mutableStateOf<ProHint?>(null) }
    state.proHint?.let { lastHint = it }
    AnimatedVisibility(state.proHint != null) {
        lastHint?.let { ProHintBanner(proHintText(it), onSeePro = onSeePro, onDismiss = { onIntent(LiveSetupIntent.DismissHint) }) }
    }

    Spacer(Modifier.height(dimens.lg))
    SecondaryButton(
        stringResource(if (showMore) Res.string.live_fewer_options else Res.string.live_more_options),
        onClick = { onIntent(LiveSetupIntent.ToggleMore) },
        leading = Glyphs.Sliders,
    )
    AnimatedVisibility(showMore) {
        Column(Modifier.padding(top = dimens.lg)) {
            SetupLabel(stringResource(Res.string.live_gifts))
            ChipRow {
                PulseChip(stringResource(Res.string.live_gifts_on), selected = draft.gifts, onClick = { onIntent(LiveSetupIntent.Gifts(true)) }, leading = Glyphs.Gift)
                PulseChip(stringResource(Res.string.live_gifts_off), selected = !draft.gifts, onClick = { onIntent(LiveSetupIntent.Gifts(false)) })
            }
            Spacer(Modifier.height(dimens.lg))
            SetupLabel(stringResource(Res.string.live_pace))
            ChipRow {
                EventFrequency.entries.forEach { frequency ->
                    PulseChip(frequency.label(), selected = frequency == draft.frequency, onClick = { onIntent(LiveSetupIntent.Frequency(frequency)) })
                }
            }
            Spacer(Modifier.height(dimens.lg))
            SetupLabel(stringResource(Res.string.live_language))
            ChipRow {
                AudienceLanguage.entries.forEach { language ->
                    PulseChip(language.label(), selected = language == draft.language, onClick = { onIntent(LiveSetupIntent.Language(language)) })
                }
            }
            Spacer(Modifier.height(dimens.lg))
            SetupLabel(stringResource(Res.string.live_length))
            ChipRow {
                LENGTH_OPTIONS.forEach { option ->
                    val locked = option * SECONDS_PER_MINUTE > limits.maxSessionSeconds
                    PulseChip(
                        stringResource(Res.string.live_minutes, option),
                        selected = option == draft.minutes,
                        locked = locked,
                        leading = if (locked) Glyphs.Lock else Glyphs.Clock,
                        onClick = { onIntent(LiveSetupIntent.Minutes(option)) },
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(dimens.xl))
}

@Composable
private fun GoLiveBar(state: LiveSetupUiState, onGoLive: () -> Unit, onIntent: (LiveSetupIntent) -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val wallet = state.points as? PointsState.Ready
    Column(Modifier.padding(horizontal = dimens.gutter, vertical = dimens.md)) {
        // Keeps the last shortfall while it animates out.
        var lastShortfall by remember { mutableStateOf(state.shortfall) }
        state.shortfall?.let { lastShortfall = it }
        AnimatedVisibility(state.shortfall != null && wallet != null) {
            val shortfall = lastShortfall
            if (shortfall != null && wallet != null) {
                PointsShortPanel(
                    cost = shortfall.cost,
                    balance = wallet.balance,
                    adReward = wallet.config.adReward,
                    adsLeft = wallet.adsLeftToday,
                    adsAvailable = state.adsAvailable,
                    todayReward = wallet.todayReward,
                    adStatus = state.adStatus,
                    adCredited = state.adCredited,
                    onWatchAd = { onIntent(LiveSetupIntent.WatchAd) },
                    onClaimDaily = { onIntent(LiveSetupIntent.ClaimDaily) },
                    onDismiss = { onIntent(LiveSetupIntent.DismissShortfall) },
                    modifier = Modifier.padding(bottom = dimens.md),
                )
            }
        }
        state.pointsError?.let {
            Text(stringResource(it.messageRes), style = Pulse.type.bodySmall, color = colors.error, modifier = Modifier.padding(bottom = dimens.sm))
        }
        val cost = state.cost
        PrimaryButton(
            when {
                state.livesLeft == 0 -> stringResource(Res.string.live_no_lives)
                state.paying -> stringResource(Res.string.points_paying)
                cost != null -> stringResource(Res.string.points_go_live_cost, cost)
                else -> stringResource(Res.string.go_live)
            },
            onClick = onGoLive,
            enabled = state.canGoLive,
            leading = Glyphs.Bolt,
            brush = colors.liveGradient,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(dimens.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            SimulatedTag(text = stringResource(Res.string.live_simulated_audience))
            Spacer(Modifier.width(dimens.sm))
            Text(
                if (state.livesLeft == 0) stringResource(Res.string.live_lives_free, state.limits.sessionsPerDay)
                else stringResource(Res.string.live_lives_left, state.livesLeft, state.limits.sessionsPerDay),
                style = Pulse.type.meta,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun StudioBackdrop(cameraPreview: Boolean, style: LiveStyle, avatar: ArtSpec) {
    if (cameraPreview) {
        CameraPreview(CameraLens.FRONT, Modifier.fillMaxSize())
    } else {
        Artwork(style.backdrop, Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().padding(top = Pulse.dimens.setupStageTop), contentAlignment = Alignment.TopCenter) {
                CreatorAvatar(avatar, Pulse.dimens.avatarHero, ring = AvatarRing.LIVE)
            }
        }
    }
}

@Composable
private fun PermissionPanel(need: PermissionNeed, onAllow: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Column(
        modifier
            .fillMaxWidth()
            .clip(Pulse.shapes.panel)
            .background(colors.surface.copy(alpha = PANEL_ALPHA))
            .border(dimens.hairline, colors.borderStrong, Pulse.shapes.panel)
            .padding(dimens.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlossyIcon(Glyphs.Mic, Gloss.Violet, size = dimens.tileMd)
            Spacer(Modifier.width(dimens.md))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (need == PermissionNeed.UNSUPPORTED) Res.string.perm_unsupported_title else Res.string.perm_title),
                    style = Pulse.type.titleSmall,
                    color = colors.textPrimary,
                )
                Text(
                    stringResource(
                        when (need) {
                            PermissionNeed.UNSUPPORTED -> Res.string.perm_unsupported_body
                            PermissionNeed.DENIED -> Res.string.perm_denied_body
                            else -> Res.string.perm_body
                        },
                    ),
                    style = Pulse.type.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        if (need == PermissionNeed.NOT_ASKED) {
            Spacer(Modifier.height(dimens.md))
            SecondaryButton(stringResource(Res.string.perm_allow), onClick = onAllow, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun proHintText(hint: ProHint): String = when (hint) {
    is ProHint.Preset -> stringResource(Res.string.live_preset_pro, hint.preset.label())
    is ProHint.Mood -> stringResource(Res.string.live_mood_pro, hint.vibe.label())
    is ProHint.Style -> stringResource(Res.string.live_style_pro, stringResource(LiveStyles.byId(hint.styleId).name))
    is ProHint.Length -> stringResource(Res.string.live_length_pro, hint.maxMinutes)
}

@Composable
private fun SetupLabel(text: String) {
    Text(text, style = Pulse.type.label, color = Pulse.colors.textPrimary)
    Spacer(Modifier.height(Pulse.dimens.sm))
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = Pulse.dimens.xs),
        horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm),
    ) { content() }
}

@Composable
private fun StyleCard(style: LiveStyle, selected: Boolean, locked: Boolean, cost: Int?, onClick: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val name = stringResource(style.name)
    val description = stringResource(if (locked) Res.string.live_style_description_locked else Res.string.live_style_description, name)
    Column(
        Modifier
            .width(dimens.railCardNarrow)
            .pressable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = description },
    ) {
        Artwork(
            style.backdrop,
            Modifier
                .fillMaxWidth()
                .height(dimens.tileXxl)
                .clip(Pulse.shapes.control)
                .border(dimens.ring, if (selected) style.glow else colors.border, Pulse.shapes.control),
        ) {
            if (locked) {
                Box(Modifier.fillMaxSize().background(colors.scrim), contentAlignment = Alignment.Center) {
                    PulseIcon(Glyphs.Lock, colors.textPrimary, size = dimens.iconSmall)
                }
            }
        }
        Spacer(Modifier.height(dimens.xs))
        Text(name, style = Pulse.type.label, color = if (selected) colors.textPrimary else colors.textSecondary)
        if (cost != null) Text(stringResource(Res.string.points_style_cost, cost), style = Pulse.type.meta, color = colors.gold)
    }
}

@Composable
private fun ProHintBanner(text: String, onSeePro: () -> Unit, onDismiss: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val dismiss = stringResource(Res.string.live_dismiss)
    Row(
        Modifier
            .padding(top = dimens.lg)
            .fillMaxWidth()
            .clip(Pulse.shapes.control)
            .background(colors.gold.copy(alpha = 0.12f))
            .border(dimens.hairline, colors.gold.copy(alpha = 0.35f), Pulse.shapes.control)
            .padding(start = dimens.md, top = dimens.sm, bottom = dimens.sm, end = dimens.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(Glyphs.Crown, colors.gold, size = dimens.iconSmall)
        Spacer(Modifier.width(dimens.sm))
        Text(text, style = Pulse.type.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(
            stringResource(Res.string.live_see_pro),
            style = Pulse.type.label,
            color = colors.gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.pressable(onClick = onSeePro).padding(horizontal = dimens.sm, vertical = dimens.xs),
        )
        Box(Modifier.size(dimens.touch).pressable(onClick = onDismiss).semantics { contentDescription = dismiss }, contentAlignment = Alignment.Center) {
            PulseIcon(Glyphs.Close, colors.textMuted, size = dimens.iconTiny)
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun LiveSetupPreview() = PreviewContainer {
    LiveSetupContent(
        state = LiveSetupUiState(SetupDraft(title = "Story time"), avatarPalette = 3, livesLeft = 2, proHint = ProHint.Mood(AudienceVibe.CRITICAL), points = previewWallet(120)),
        permission = PermissionNeed.NOT_ASKED,
        cameraPreview = false,
        onIntent = {},
        onGoLive = {},
        onAllow = {},
        onClose = {},
        onSeePro = {},
    )
}

@ScreenPreviews
@Composable
private fun LiveSetupShortOfPointsPreview() = PreviewContainer {
    LiveSetupContent(
        state = LiveSetupUiState(
            SetupDraft(title = "Late night Q&A", styleId = LiveStyles.Noir.id),
            avatarPalette = 5,
            livesLeft = 3,
            points = previewWallet(20),
            adsAvailable = true,
            shortfall = Shortfall(cost = 50),
        ),
        permission = PermissionNeed.NONE,
        cameraPreview = false,
        onIntent = {},
        onGoLive = {},
        onAllow = {},
        onClose = {},
        onSeePro = {},
    )
}

private fun previewWallet(balance: Int): PointsState.Ready {
    val wallet = Wallet(balance = balance, streak = 2, lastClaimDay = 0, lastClaimAtMs = 0)
    val config = EconomyConfig.Default
    return PointsState.Ready(
        balance = balance,
        streakDays = 2,
        canClaimToday = true,
        calendar = StreakRules.calendar(wallet, config, nowMs = 86_400_000L + StreakRules.MIN_CLAIM_GAP_MS, offsetMinutes = 0),
        config = config,
        adsLeftToday = config.adsPerDay,
    )
}
