package org.mayad.fame_life.domain.simulation

import kotlinx.serialization.Serializable

/** Totals collected while a session runs. Every number on the summary screen comes from here. */
data class SessionTotals(
    val comments: Int = 0,
    val contextualComments: Int = 0,
    val reactions: Int = 0,
    val gifts: Int = 0,
    val giftPoints: Int = 0,
    val milestones: Int = 0,
    val aiRequests: Int = 0,
    val aiFailures: Int = 0,
    val fallbackResponses: Int = 0,
)

/** The persisted record of a finished session. Versioned so older records stay readable. */
@Serializable
data class SessionSummary(
    val id: String,
    val summaryVersion: Int = 1,
    val title: String,
    val startedAtEpochMs: Long,
    val durationSeconds: Int,
    val preset: String,
    val liveStyleId: String,
    val peakViewers: Int,
    val comments: Int,
    val contextualComments: Int,
    val reactions: Int,
    val gifts: Int,
    val giftPoints: Int,
    val milestones: Int,
    val topGiftId: String? = null,
    val highlight: String? = null,
    val famePointsEarned: Int,
)
