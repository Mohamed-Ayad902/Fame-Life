package org.mayad.fame_life.feature.live

import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.live_level_body
import famelife.shared.generated.resources.live_level_title
import famelife.shared.generated.resources.live_recap_body
import famelife.shared.generated.resources.live_recap_title
import famelife.shared.generated.resources.live_your_live
import org.jetbrains.compose.resources.getString
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.platform.EpochClock
import org.mayad.fame_life.core.ui.titleRes
import org.mayad.fame_life.data.local.EntitlementRepository
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.data.local.NotificationRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.domain.progression.FameLevel
import org.mayad.fame_life.domain.simulation.LiveSessionState
import org.mayad.fame_life.domain.simulation.SessionSummary
import org.mayad.fame_life.feature.notifications.NotificationKind

private const val ABANDONED_SECONDS = 15

/**
 * Persists a finished live: history, fame points, the inbox recap (in the user's language) and analytics.
 * The repositories mirror each write to the account through AccountSync.
 */
class SessionRecorder(
    private val history: SessionHistoryRepository,
    private val profiles: ProfileRepository,
    private val notifications: NotificationRepository,
    private val entitlements: EntitlementRepository,
    private val analytics: Analytics,
    private val clock: EpochClock,
) {
    suspend fun record(summary: SessionSummary, state: LiveSessionState) {
        history.save(summary, keep = entitlements.entitlements.value.limits.historySize)
        val levels = profiles.recordSession(summary.famePointsEarned)
        val now = clock.now()
        notifications.add(
            InAppNotification(
                id = "session:${summary.id}",
                kind = NotificationKind.SESSION,
                title = getString(Res.string.live_recap_title),
                body = getString(
                    Res.string.live_recap_body,
                    summary.title.ifBlank { getString(Res.string.live_your_live) },
                    compactCount(summary.peakViewers),
                    summary.famePointsEarned,
                ),
                createdAtEpochMs = now,
            ),
        )
        if (levels != null && levels.second > levels.first) {
            val level = FameLevel.entries[levels.second]
            notifications.add(
                InAppNotification(
                    id = "level_up:${summary.id}",
                    kind = NotificationKind.LEVEL_UP,
                    title = getString(Res.string.live_level_title, getString(level.titleRes)),
                    body = getString(Res.string.live_level_body),
                    createdAtEpochMs = now,
                ),
            )
            analytics.log(AnalyticsEvent.AchievementUnlocked(level.name))
        }
        val totals = state.live.totals
        analytics.log(
            if (summary.durationSeconds < ABANDONED_SECONDS) AnalyticsEvent.LiveAbandoned(summary.durationSeconds)
            else AnalyticsEvent.LiveCompleted(summary.durationSeconds, totals.aiRequests, totals.aiFailures, totals.fallbackResponses),
        )
    }
}
