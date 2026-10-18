package org.mayad.fame_life.core.preview

import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.InAppNotification
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
}
