package org.mayad.fame_life.core.preview

import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.domain.ai.SpeechStatus
import org.mayad.fame_life.domain.simulation.AiActivity
import org.mayad.fame_life.domain.simulation.AudienceEvent
import org.mayad.fame_life.domain.simulation.AudienceMember
import org.mayad.fame_life.domain.simulation.CommentTone
import org.mayad.fame_life.domain.simulation.GiftCatalog
import org.mayad.fame_life.domain.simulation.LiveSessionState
import org.mayad.fame_life.domain.simulation.LiveSnapshot
import org.mayad.fame_life.domain.simulation.ReactionKind
import org.mayad.fame_life.domain.simulation.SessionStatus
import org.mayad.fame_life.domain.simulation.SessionSummary

/**
 * The one place previews get data from. Built on the fictional seed content so previews look like the app, and
 * never touches repositories, Firebase or the network.
 */
object SampleData {
    const val NOW = 1_760_000_000_000L
    private const val HOUR = 3_600_000L

    val profile = CreatorProfile(
        displayName = "Maya Noor",
        username = "mayanoor",
        bio = "Late-night stories and loud laughs.",
        avatarPalette = 3,
        interests = listOf("STORYTELLING", "COMEDY"),
        createdAtEpochMs = NOW - 72 * HOUR,
        famePoints = 1_840,
        sessionsCompleted = 7,
    )

    val newProfile = profile.copy(famePoints = 0, sessionsCompleted = 0, bio = "")

    val sessions = List(4) { i ->
        SessionSummary(
            id = "sample-$i",
            title = listOf("Story time", "Late night chat", "Hot takes", "Music talk")[i],
            startedAtEpochMs = NOW - (i + 1) * 20 * HOUR,
            durationSeconds = 420 + i * 95,
            preset = "RISING",
            liveStyleId = "aurora",
            peakViewers = 2_480 - i * 310,
            comments = 186 - i * 20,
            contextualComments = 140 - i * 15,
            reactions = 920 - i * 80,
            gifts = 12 - i,
            giftPoints = 340 - i * 30,
            milestones = 2,
            topGiftId = "crown",
            highlight = "wait, tell us more!!",
            famePointsEarned = 260 - i * 20,
        )
    }

    val summary = sessions.first()

    val notifications = listOf(
        InAppNotification("n-recap", "session", "Live recap ready", "Story time peaked at 2.4K simulated viewers.", NOW - HOUR),
        InAppNotification("n-level", "level_up", "You're now a Rising Creator", "Your lives earned enough fame points.", NOW - 2 * HOUR, read = true),
        InAppNotification("n-welcome", "welcome", "Welcome to Pulse", "Tap the glowing button to start your first live.", NOW - 72 * HOUR, read = true),
    )

    val creator = SeedContent.creators.first()
    val post = SeedContent.posts.first()
    val room = SeedContent.liveRooms.first()

    private val members = listOf(
        AudienceMember("m1", "lina.vibes", 280f),
        AudienceMember("m2", "omar_k", 30f, isVip = true),
        AudienceMember("m3", "nightowl", 190f),
        AudienceMember("m4", "sara.m", 330f),
    )

    val comments = listOf(
        AudienceEvent.Comment(1, 1_000, members[0], "wait, tell us more!!", CommentTone.CURIOUS, contextual = true),
        AudienceEvent.Comment(2, 2_000, members[1], "this story is wild 😂", CommentTone.FUNNY, contextual = true),
        AudienceEvent.Comment(3, 3_000, members[2], "hello from Cairo 👋", CommentTone.SUPPORTIVE, contextual = false),
        AudienceEvent.Comment(4, 4_000, members[3], "not sure I agree tbh", CommentTone.CRITICAL, contextual = true),
    )

    val gift = AudienceEvent.GiftSent(10, 5_000, members[1], GiftCatalog.byId("crown")!!, reason = null)

    val liveState = LiveSessionState(
        live = LiveSnapshot(
            status = SessionStatus.RUNNING,
            elapsedSeconds = 312,
            remainingSeconds = 288,
            viewers = 2_480,
            peakViewers = 2_610,
            comments = comments,
            spotlight = gift,
            reactions = listOf(AudienceEvent.Reaction(20, 5_500, ReactionKind.HEART, 5)),
        ),
        ai = AiActivity.ON_DEVICE,
        speech = SpeechStatus.LISTENING,
        livePartial = "so then I told them the whole story",
    )
}
