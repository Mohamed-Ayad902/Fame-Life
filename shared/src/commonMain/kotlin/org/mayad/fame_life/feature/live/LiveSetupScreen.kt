package org.mayad.fame_life.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.analytics.AnalyticsEvent
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
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.platform.CameraLens
import org.mayad.fame_life.core.platform.CameraPreview
import org.mayad.fame_life.core.platform.LivePermissions
import org.mayad.fame_life.core.platform.isCameraPreviewSupported
import org.mayad.fame_life.core.platform.rememberLivePermissions
import org.mayad.fame_life.data.local.nowEpochMs
import org.mayad.fame_life.domain.entitlement.PlanLimits
import org.mayad.fame_life.domain.simulation.AudienceLanguage
import org.mayad.fame_life.domain.simulation.AudiencePreset
import org.mayad.fame_life.domain.simulation.AudienceVibe
import org.mayad.fame_life.domain.simulation.EventFrequency
import org.mayad.fame_life.domain.simulation.SimulationConfig
import org.mayad.fame_life.feature.profile.selfAvatar
import kotlin.random.Random

private const val DAY_MS = 24 * 60 * 60 * 1000L

/** The creator studio: everything the user picks before going live. Common choices first, the rest under "More". */
@Composable
fun LiveSetupScreen(initialTitle: String, navigator: Navigator) {
    val container = LocalAppContainer.current
    val profile = container.profiles.profile.collectAsState().value ?: return
    val entitlements by container.entitlements.entitlements.collectAsState()
    val sessions by container.history.sessions.collectAsState()
    val limits = entitlements.limits
    val permissions = rememberLivePermissions()
    val colors = Pulse.colors

    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var presetName by rememberSaveable { mutableStateOf(AudiencePreset.RISING.name) }
    var vibeNames by rememberSaveable { mutableStateOf(listOf(AudienceVibe.SUPPORTIVE.name, AudienceVibe.CURIOUS.name, AudienceVibe.FUNNY.name)) }
    var gifts by rememberSaveable { mutableStateOf(true) }
    var frequencyName by rememberSaveable { mutableStateOf(EventFrequency.LIVELY.name) }
    var languageName by rememberSaveable { mutableStateOf(AudienceLanguage.ENGLISH.name) }
    var styleId by rememberSaveable { mutableStateOf(LiveStyles.Aurora.id) }
    var minutes by rememberSaveable { mutableStateOf(limits.maxSessionSeconds / 60) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    var proHint by rememberSaveable { mutableStateOf<String?>(null) }

    val now = remember { nowEpochMs() }
    val livesToday = sessions.count { now - it.startedAtEpochMs < DAY_MS }
    val livesLeft = (limits.sessionsPerDay - livesToday).coerceAtLeast(0)
    val style = LiveStyles.byId(styleId)

    LaunchedEffect(Unit) { container.analytics.log(AnalyticsEvent.LiveSetupOpened) }
    LaunchedEffect(permissions.asked, permissions.camera, permissions.microphone) {
        if (permissions.asked) container.analytics.log(AnalyticsEvent.PermissionResult(permissions.camera, permissions.microphone))
    }

    fun goLive() {
        val vibes = vibeNames.map(AudienceVibe::valueOf).toSet()
        val config = SimulationConfig(
            title = title.trim(),
            preset = AudiencePreset.valueOf(presetName),
            vibes = vibes,
            hatersMode = AudienceVibe.CRITICAL in vibes,
            giftsEnabled = gifts,
            frequency = EventFrequency.valueOf(frequencyName),
            language = AudienceLanguage.valueOf(languageName),
            liveStyleId = styleId,
            maxDurationSeconds = minutes * 60,
            seed = Random.nextLong(),
        ).constrainedTo(limits)
        val sessionId = "s" + Random.nextLong().toULong().toString(36)
        container.analytics.log(
            AnalyticsEvent.LiveStarted(
                preset = config.preset.name,
                style = config.liveStyleId,
                plan = entitlements.plan.name,
                camera = permissions.camera,
                mic = permissions.microphone,
            ),
        )
        navigator.push(Route.Live(sessionId, config))
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        // The studio backdrop is the camera itself when allowed, so the user sees exactly how they'll appear.
        StudioBackdrop(permissions, style, selfAvatar(profile.avatarPalette))
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color(0x99080B14), 0.35f to Color(0x33080B14), 0.62f to Color(0xCC080B14), 1f to colors.background),
            ),
        )
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(Pulse.dimens.lg), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Glyphs.Close, contentDescription = "Close", onClick = { navigator.pop() })
                Spacer(Modifier.weight(1f))
                LiveBadge(label = "PREVIEW")
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Pulse.dimens.gutter)) {
                Spacer(Modifier.height(140.dp))
                Text("Creator studio", style = Pulse.type.overline, color = style.glow)
                Text("Set the room", style = Pulse.type.display, color = colors.textPrimary)
                Spacer(Modifier.height(Pulse.dimens.lg))
                if (!permissions.camera || !permissions.microphone) {
                    PermissionPanel(permissions)
                    Spacer(Modifier.height(Pulse.dimens.lg))
                }
                PulseTextField(title, { title = it }, "What's your live about?", placeholder = "Q&A, cooking, a game, a story…", maxLength = 60)
                Spacer(Modifier.height(Pulse.dimens.xl))

                SetupLabel("Audience size")
                ChipRow {
                    AudiencePreset.entries.forEach { preset ->
                        val locked = preset !in limits.presets
                        PulseChip(
                            preset.label,
                            selected = preset.name == presetName,
                            locked = locked,
                            leading = if (locked) Glyphs.Lock else Glyphs.Users,
                            onClick = { if (locked) proHint = "${preset.label} crowds are part of Pro." else presetName = preset.name },
                        )
                    }
                }
                Text(AudiencePreset.valueOf(presetName).hint + " simulated viewers", style = Pulse.type.meta, color = colors.textMuted)
                Spacer(Modifier.height(Pulse.dimens.lg))

                SetupLabel("Audience mood")
                ChipRow {
                    AudienceVibe.entries.forEach { vibe ->
                        val locked = vibe !in limits.vibes
                        val selected = vibe.name in vibeNames
                        PulseChip(
                            vibe.label,
                            selected = selected,
                            locked = locked,
                            leading = if (locked) Glyphs.Lock else null,
                            onClick = {
                                when {
                                    locked -> proHint = "The ${vibe.label} mood is part of Pro."
                                    // Keep at least one mood so the room always has a voice.
                                    selected && vibeNames.size > 1 -> vibeNames = vibeNames - vibe.name
                                    !selected -> vibeNames = vibeNames + vibe.name
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(Pulse.dimens.lg))

                SetupLabel("Live style")
                Row(horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
                    LiveStyles.all.forEach { option ->
                        val locked = option.id !in limits.liveStyles
                        StyleCard(option, selected = option.id == styleId, locked = locked) {
                            if (locked) proHint = "The ${option.name} style is part of Pro." else styleId = option.id
                        }
                    }
                }

                AnimatedVisibility(proHint != null) {
                    ProHint(proHint.orEmpty(), onSeePro = { navigator.push(Route.Paywall) }, onDismiss = { proHint = null })
                }

                Spacer(Modifier.height(Pulse.dimens.lg))
                SecondaryButton(
                    if (showMore) "Fewer options" else "More options",
                    onClick = { showMore = !showMore },
                    leading = Glyphs.Sliders,
                )
                AnimatedVisibility(showMore) {
                    Column(Modifier.padding(top = Pulse.dimens.lg)) {
                        SetupLabel("Gifts")
                        ChipRow {
                            PulseChip("Gifts on", selected = gifts, onClick = { gifts = true }, leading = Glyphs.Gift)
                            PulseChip("Gifts off", selected = !gifts, onClick = { gifts = false })
                        }
                        Spacer(Modifier.height(Pulse.dimens.lg))
                        SetupLabel("Pace")
                        ChipRow {
                            EventFrequency.entries.forEach { frequency ->
                                PulseChip(frequency.label, selected = frequency.name == frequencyName, onClick = { frequencyName = frequency.name })
                            }
                        }
                        Spacer(Modifier.height(Pulse.dimens.lg))
                        SetupLabel("Audience language")
                        ChipRow {
                            AudienceLanguage.entries.forEach { language ->
                                PulseChip(language.label, selected = language.name == languageName, onClick = { languageName = language.name })
                            }
                        }
                        Spacer(Modifier.height(Pulse.dimens.lg))
                        SetupLabel("Length")
                        ChipRow {
                            listOf(5, 10, 20, 45).forEach { option ->
                                val locked = option * 60 > limits.maxSessionSeconds
                                PulseChip(
                                    "$option min",
                                    selected = option == minutes,
                                    locked = locked,
                                    leading = if (locked) Glyphs.Lock else Glyphs.Clock,
                                    onClick = { if (locked) proHint = "Lives longer than ${limits.maxSessionSeconds / 60} minutes are part of Pro." else minutes = option },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Pulse.dimens.xl))
            }
            Column(Modifier.padding(horizontal = Pulse.dimens.gutter, vertical = Pulse.dimens.md)) {
                PrimaryButton(
                    if (livesLeft > 0) "Go live" else "No lives left today",
                    onClick = ::goLive,
                    enabled = livesLeft > 0,
                    leading = Glyphs.Bolt,
                    brush = colors.liveGradient,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Pulse.dimens.sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    SimulatedTag(text = "Simulated audience")
                    Spacer(Modifier.width(Pulse.dimens.sm))
                    Text(livesLeftLabel(livesLeft, limits), style = Pulse.type.meta, color = colors.textMuted)
                }
            }
        }
    }
}

private fun livesLeftLabel(left: Int, limits: PlanLimits) =
    if (left == 0) "Free includes ${limits.sessionsPerDay} lives a day" else "$left of ${limits.sessionsPerDay} lives left today"

@Composable
private fun StudioBackdrop(permissions: LivePermissions, style: LiveStyle, avatar: org.mayad.fame_life.data.content.ArtSpec) {
    if (permissions.camera && isCameraPreviewSupported) {
        CameraPreview(CameraLens.FRONT, Modifier.fillMaxSize())
    } else {
        Artwork(style.backdrop, Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().padding(top = 120.dp), contentAlignment = Alignment.TopCenter) {
                CreatorAvatar(avatar, 132.dp, ring = AvatarRing.LIVE)
            }
        }
    }
}

@Composable
private fun PermissionPanel(permissions: LivePermissions) {
    val colors = Pulse.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Pulse.shapes.panel)
            .background(colors.surface.copy(alpha = 0.88f))
            .border(1.dp, colors.borderStrong, Pulse.shapes.panel)
            .padding(Pulse.dimens.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlossyIcon(Glyphs.Mic, Gloss.Violet, size = 44.dp)
            Spacer(Modifier.width(Pulse.dimens.md))
            Column(Modifier.weight(1f)) {
                Text(
                    if (!permissions.supported) "Camera and mic aren't ready on this device yet" else "Let your audience see and hear you",
                    style = Pulse.type.titleSmall,
                    color = colors.textPrimary,
                )
                Text(
                    when {
                        !permissions.supported -> "You can still go live; the simulated audience will react to the room instead of your words."
                        permissions.asked -> "Without them the audience can't react to what you say. You can still go live, or allow access in system settings."
                        else -> "Your camera stays on your phone. Speech becomes text so the simulated audience can respond to it."
                    },
                    style = Pulse.type.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        if (permissions.supported && !permissions.asked) {
            Spacer(Modifier.height(Pulse.dimens.md))
            SecondaryButton("Allow camera and mic", onClick = permissions::request, modifier = Modifier.fillMaxWidth())
        }
    }
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
private fun StyleCard(style: LiveStyle, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val colors = Pulse.colors
    Column(
        Modifier
            .width(132.dp)
            .pressable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = "${style.name} style" + if (locked) ", requires Pro" else "" },
    ) {
        Artwork(
            style.backdrop,
            Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(Pulse.shapes.control)
                .border(2.dp, if (selected) style.glow else colors.border, Pulse.shapes.control),
        ) {
            if (locked) {
                Box(Modifier.fillMaxSize().background(colors.scrim), contentAlignment = Alignment.Center) {
                    PulseIcon(Glyphs.Lock, colors.textPrimary, size = 18.dp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(style.name, style = Pulse.type.label, color = if (selected) colors.textPrimary else colors.textSecondary)
    }
}

@Composable
private fun ProHint(text: String, onSeePro: () -> Unit, onDismiss: () -> Unit) {
    val colors = Pulse.colors
    Row(
        Modifier
            .padding(top = Pulse.dimens.lg)
            .fillMaxWidth()
            .clip(Pulse.shapes.control)
            .background(colors.gold.copy(alpha = 0.12f))
            .border(1.dp, colors.gold.copy(alpha = 0.35f), Pulse.shapes.control)
            .padding(start = Pulse.dimens.md, top = Pulse.dimens.sm, bottom = Pulse.dimens.sm, end = Pulse.dimens.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(Glyphs.Crown, colors.gold, size = 18.dp)
        Spacer(Modifier.width(Pulse.dimens.sm))
        Text(text, style = Pulse.type.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(
            "See Pro",
            style = Pulse.type.label,
            color = colors.gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.pressable(onClick = onSeePro).padding(horizontal = Pulse.dimens.sm, vertical = Pulse.dimens.xs),
        )
        Box(Modifier.size(36.dp).pressable(onClick = onDismiss).semantics { contentDescription = "Dismiss" }, contentAlignment = Alignment.Center) {
            PulseIcon(Glyphs.Close, colors.textMuted, size = 14.dp)
        }
    }
}
