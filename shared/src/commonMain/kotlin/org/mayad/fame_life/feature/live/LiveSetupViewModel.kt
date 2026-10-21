package org.mayad.fame_life.feature.live

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.platform.EpochClock
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.data.local.EntitlementRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.domain.entitlement.Plan
import org.mayad.fame_life.domain.entitlement.PlanLimits
import org.mayad.fame_life.domain.simulation.AudienceLanguage
import org.mayad.fame_life.domain.simulation.AudiencePreset
import org.mayad.fame_life.domain.simulation.AudienceVibe
import org.mayad.fame_life.domain.simulation.EventFrequency
import org.mayad.fame_life.domain.simulation.SessionSummary
import org.mayad.fame_life.domain.simulation.SimulationConfig
import kotlin.random.Random

private const val DAY_MS = 24 * 60 * 60 * 1000L
internal const val SECONDS_PER_MINUTE = 60
internal val LENGTH_OPTIONS = listOf(5, 10, 20, 45)

/** Everything the creator picks before going live. */
@Immutable
data class SetupDraft(
    val title: String = "",
    val preset: AudiencePreset = AudiencePreset.RISING,
    val vibes: Set<AudienceVibe> = setOf(AudienceVibe.SUPPORTIVE, AudienceVibe.CURIOUS, AudienceVibe.FUNNY),
    val gifts: Boolean = true,
    val frequency: EventFrequency = EventFrequency.LIVELY,
    val language: AudienceLanguage = AudienceLanguage.MIXED,
    val styleId: String = LiveStyles.Aurora.id,
    val minutes: Int = LENGTH_OPTIONS[1],
)

/** Why a tapped option is locked. The screen turns it into copy; the ViewModel only decides that it is locked. */
@Immutable
sealed interface ProHint {
    data class Preset(val preset: AudiencePreset) : ProHint
    data class Mood(val vibe: AudienceVibe) : ProHint
    data class Style(val styleId: String) : ProHint
    data class Length(val maxMinutes: Int) : ProHint
}

@Immutable
data class LiveSetupUiState(
    val draft: SetupDraft,
    /** The creator's avatar look; null only before the account cache loads. */
    val avatarPalette: Int? = null,
    val limits: PlanLimits = PlanLimits.Free,
    val plan: Plan = Plan.FREE,
    val livesLeft: Int = 0,
    val proHint: ProHint? = null,
    val showMore: Boolean = false,
) {
    val canGoLive: Boolean get() = livesLeft > 0
}

sealed interface LiveSetupIntent {
    data class Title(val value: String) : LiveSetupIntent
    data class Preset(val preset: AudiencePreset) : LiveSetupIntent
    data class ToggleVibe(val vibe: AudienceVibe) : LiveSetupIntent
    data class Style(val styleId: String) : LiveSetupIntent
    data class Gifts(val on: Boolean) : LiveSetupIntent
    data class Frequency(val frequency: EventFrequency) : LiveSetupIntent
    data class Language(val language: AudienceLanguage) : LiveSetupIntent
    data class Minutes(val minutes: Int) : LiveSetupIntent
    data object DismissHint : LiveSetupIntent
    data object ToggleMore : LiveSetupIntent
    data class PermissionResult(val camera: Boolean, val mic: Boolean) : LiveSetupIntent
    data class GoLive(val camera: Boolean, val mic: Boolean) : LiveSetupIntent
}

sealed interface LiveSetupEvent {
    data class Start(val sessionId: String, val config: SimulationConfig) : LiveSetupEvent
}

/**
 * The pre-live studio. Owns the draft and decides which options the plan unlocks. The plan here only shapes the UI:
 * the engine re-applies [SimulationConfig.constrainedTo], and paid AI limits are enforced by the backend.
 */
class LiveSetupViewModel(
    initialTitle: String,
    profiles: ProfileRepository,
    history: SessionHistoryRepository,
    entitlements: EntitlementRepository,
    private val analytics: Analytics,
    private val clock: EpochClock,
) : MviViewModel<LiveSetupUiState, LiveSetupIntent, LiveSetupEvent>(
    LiveSetupUiState(
        draft = SetupDraft(title = initialTitle, minutes = entitlements.entitlements.value.limits.maxSessionSeconds / SECONDS_PER_MINUTE),
    ),
) {

    init {
        analytics.log(AnalyticsEvent.LiveSetupOpened)
        viewModelScope.launch {
            combine(profiles.profile, history.sessions, entitlements.entitlements) { profile, sessions, ent ->
                Triple(profile?.avatarPalette, ent, livesLeft(ent.limits, sessions, clock.now()))
            }.collect { (palette, ent, left) ->
                setState { copy(avatarPalette = palette, limits = ent.limits, plan = ent.plan, livesLeft = left) }
            }
        }
    }

    override fun onIntent(intent: LiveSetupIntent) {
        val limits = currentState.limits
        when (intent) {
            is LiveSetupIntent.Title -> updateDraft { copy(title = intent.value) }
            is LiveSetupIntent.Preset ->
                if (intent.preset in limits.presets) updateDraft { copy(preset = intent.preset) } else hint(ProHint.Preset(intent.preset))
            is LiveSetupIntent.ToggleVibe -> toggleVibe(intent.vibe, limits)
            is LiveSetupIntent.Style ->
                if (intent.styleId in limits.liveStyles) updateDraft { copy(styleId = intent.styleId) } else hint(ProHint.Style(intent.styleId))
            is LiveSetupIntent.Gifts -> updateDraft { copy(gifts = intent.on) }
            is LiveSetupIntent.Frequency -> updateDraft { copy(frequency = intent.frequency) }
            is LiveSetupIntent.Language -> updateDraft { copy(language = intent.language) }
            is LiveSetupIntent.Minutes ->
                if (intent.minutes * SECONDS_PER_MINUTE <= limits.maxSessionSeconds) updateDraft { copy(minutes = intent.minutes) }
                else hint(ProHint.Length(limits.maxSessionSeconds / SECONDS_PER_MINUTE))
            LiveSetupIntent.DismissHint -> hint(null)
            LiveSetupIntent.ToggleMore -> setState { copy(showMore = !showMore) }
            is LiveSetupIntent.PermissionResult -> analytics.log(AnalyticsEvent.PermissionResult(intent.camera, intent.mic))
            is LiveSetupIntent.GoLive -> goLive(intent.camera, intent.mic)
        }
    }

    private fun toggleVibe(vibe: AudienceVibe, limits: PlanLimits) {
        val vibes = currentState.draft.vibes
        when {
            vibe !in limits.vibes -> hint(ProHint.Mood(vibe))
            // Keep at least one mood so the room always has a voice.
            vibe in vibes && vibes.size > 1 -> updateDraft { copy(vibes = vibes - vibe) }
            vibe !in vibes -> updateDraft { copy(vibes = vibes + vibe) }
        }
    }

    private fun goLive(camera: Boolean, mic: Boolean) {
        val state = currentState
        if (!state.canGoLive) return
        val draft = state.draft
        val config = SimulationConfig(
            title = draft.title.trim(),
            preset = draft.preset,
            vibes = draft.vibes,
            hatersMode = AudienceVibe.CRITICAL in draft.vibes,
            giftsEnabled = draft.gifts,
            frequency = draft.frequency,
            language = draft.language,
            liveStyleId = draft.styleId,
            maxDurationSeconds = draft.minutes * SECONDS_PER_MINUTE,
            seed = Random.nextLong(),
        ).constrainedTo(state.limits)
        analytics.log(AnalyticsEvent.LiveStarted(config.preset.name, config.liveStyleId, state.plan.name, camera, mic))
        emit(LiveSetupEvent.Start(newSessionId(), config))
    }

    private fun updateDraft(change: SetupDraft.() -> SetupDraft) = setState { copy(draft = draft.change(), proHint = null) }

    private fun hint(hint: ProHint?) = setState { copy(proHint = hint) }

    companion object {
        /** Lives left today: a rolling 24 hours over the account's saved sessions. */
        fun livesLeft(limits: PlanLimits, sessions: List<SessionSummary>, now: Long): Int =
            (limits.sessionsPerDay - sessions.count { now - it.startedAtEpochMs < DAY_MS }).coerceAtLeast(0)

        private fun newSessionId() = "s" + Random.nextLong().toULong().toString(36)
    }
}
