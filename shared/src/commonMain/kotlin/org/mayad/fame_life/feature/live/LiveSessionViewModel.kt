package org.mayad.fame_life.feature.live

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.platform.CameraLens
import org.mayad.fame_life.core.platform.EpochClock
import org.mayad.fame_life.core.platform.MotionPreference
import org.mayad.fame_life.core.platform.monotonicMillis
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.EntitlementRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.di.LiveServices
import org.mayad.fame_life.domain.simulation.AudienceEvent
import org.mayad.fame_life.domain.simulation.LiveSessionController
import org.mayad.fame_life.domain.simulation.LiveSessionState
import org.mayad.fame_life.domain.simulation.SessionStatus
import org.mayad.fame_life.domain.simulation.SimulationConfig

private const val COUNTDOWN_FROM = 3

@Immutable
data class LiveSessionUiState(
    val session: LiveSessionState,
    val profile: CreatorProfile?,
    val style: LiveStyle,
    /** 3, 2, 1 before the room opens; 0 once it is live. */
    val countdown: Int = COUNTDOWN_FROM,
    val lens: CameraLens = CameraLens.FRONT,
    val confirmEnd: Boolean = false,
    /** The engine's comment and reaction lists, as immutable lists for the overlays. */
    val comments: ImmutableList<AudienceEvent.Comment> = persistentListOf(),
    val reactions: ImmutableList<AudienceEvent.Reaction> = persistentListOf(),
) {
    val paused: Boolean get() = session.live.status == SessionStatus.PAUSED
}

sealed interface LiveSessionIntent {
    data object AskEnd : LiveSessionIntent
    data object KeepGoing : LiveSessionIntent
    data object ConfirmEnd : LiveSessionIntent
    data object ToggleMic : LiveSessionIntent
    data object FlipLens : LiveSessionIntent
    data object TogglePause : LiveSessionIntent
    data object Resume : LiveSessionIntent

    /** The app went to the background. The creator resumes explicitly, so nothing happens while they are away. */
    data object AppStopped : LiveSessionIntent
}

sealed interface LiveSessionEvent {
    /** The session ended and was recorded. */
    data class Finished(val sessionId: String) : LiveSessionEvent

    /** Closed before the room opened: nothing to record. */
    data object Cancelled : LiveSessionEvent
}

/**
 * Owns one live session. The [LiveSessionController] runs in [viewModelScope], so rotation keeps the room going and
 * leaving the screen (the ViewModel is cleared) ends it and releases the microphone. A finished session is recorded
 * exactly once.
 */
class LiveSessionViewModel(
    sessionId: String,
    config: SimulationConfig,
    profiles: ProfileRepository,
    entitlements: EntitlementRepository,
    services: LiveServices,
    private val recorder: SessionRecorder,
    clock: EpochClock,
    motion: MotionPreference,
) : MviViewModel<LiveSessionUiState, LiveSessionIntent, LiveSessionEvent>(
    LiveSessionUiState(LiveSessionState(), profiles.profile.value, LiveStyles.byId(config.liveStyleId)),
) {
    private val controller = LiveSessionController(
        sessionId = sessionId,
        config = config,
        limits = entitlements.entitlements.value.limits,
        speechInput = services.speech,
        remote = services.audience,
        scope = viewModelScope,
        clock = ::monotonicMillis,
        epochClock = clock::now,
    )

    init {
        viewModelScope.launch {
            controller.state.collect { session ->
                setState {
                    copy(
                        session = session,
                        // Rebuild only when the engine produced a new list, so unchanged overlays skip recomposition.
                        comments = if (session.live.comments === this.session.live.comments) comments else session.live.comments.toImmutableList(),
                        reactions = if (session.live.reactions === this.session.live.reactions) reactions else session.live.reactions.toImmutableList(),
                    )
                }
            }
        }
        viewModelScope.launch { profiles.profile.collect { profile -> setState { copy(profile = profile) } } }
        viewModelScope.launch {
            val step = if (motion.reduced()) Motion.COUNTDOWN_STEP_REDUCED else Motion.COUNTDOWN_STEP
            for (n in COUNTDOWN_FROM downTo 1) {
                setState { copy(countdown = n) }
                delay(step.toLong())
            }
            setState { copy(countdown = 0) }
            controller.start()
        }
        viewModelScope.launch {
            val session = controller.state.first { it.summary != null }
            recorder.record(checkNotNull(session.summary), session)
            emit(LiveSessionEvent.Finished(checkNotNull(session.summary).id))
        }
    }

    override fun onIntent(intent: LiveSessionIntent) {
        when (intent) {
            LiveSessionIntent.AskEnd -> setState { copy(confirmEnd = true) }
            LiveSessionIntent.KeepGoing -> setState { copy(confirmEnd = false) }
            LiveSessionIntent.ConfirmEnd -> {
                setState { copy(confirmEnd = false) }
                if (currentState.session.live.status == SessionStatus.READY) emit(LiveSessionEvent.Cancelled) else controller.end()
            }
            LiveSessionIntent.ToggleMic -> controller.setMicMuted(!currentState.session.micMuted)
            LiveSessionIntent.FlipLens -> setState { copy(lens = if (lens == CameraLens.FRONT) CameraLens.BACK else CameraLens.FRONT) }
            LiveSessionIntent.TogglePause -> if (currentState.paused) controller.resume() else controller.pause()
            LiveSessionIntent.Resume -> controller.resume()
            LiveSessionIntent.AppStopped -> controller.pause()
        }
    }

    override fun onCleared() {
        controller.end()
    }
}
