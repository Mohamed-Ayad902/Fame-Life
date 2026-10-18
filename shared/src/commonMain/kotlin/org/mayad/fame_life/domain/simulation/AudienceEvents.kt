package org.mayad.fame_life.domain.simulation

enum class GiftTier { SMALL, MEDIUM, MAJOR }

/** A simulated gift. Gifts are entertainment events with fame points, never money. */
data class Gift(
    val id: String,
    val name: String,
    val tier: GiftTier,
    val famePoints: Int,
    /** Relative chance among gifts of the same tier. */
    val weight: Int,
)

object GiftCatalog {
    val all = listOf(
        Gift("spark", "Spark", GiftTier.SMALL, famePoints = 5, weight = 6),
        Gift("heart", "Heart", GiftTier.SMALL, famePoints = 8, weight = 5),
        Gift("mic", "Golden Mic", GiftTier.MEDIUM, famePoints = 40, weight = 4),
        Gift("comet", "Comet", GiftTier.MEDIUM, famePoints = 55, weight = 3),
        Gift("crown", "Crown", GiftTier.MAJOR, famePoints = 220, weight = 2),
        Gift("galaxy", "Galaxy", GiftTier.MAJOR, famePoints = 400, weight = 1),
    )

    fun byId(id: String): Gift? = all.firstOrNull { it.id == id }
    fun ofTier(tier: GiftTier): List<Gift> = all.filter { it.tier == tier }
}

/** A simulated audience member. Identity is derived from a stable index so the same name keeps the same avatar. */
data class AudienceMember(
    val id: String,
    val handle: String,
    val hue: Float,
    val isVip: Boolean = false,
)

enum class ReactionKind { HEART, FIRE, LAUGH, WOW, CLAP }

enum class CommentTone { SUPPORTIVE, CURIOUS, FUNNY, HYPE, CRITICAL, NEUTRAL }

sealed interface AudienceEvent {
    val id: Long
    val atMs: Long

    data class Comment(
        override val id: Long,
        override val atMs: Long,
        val author: AudienceMember,
        val text: String,
        val tone: CommentTone,
        /** True when the comment came from the AI or the contextual fallback rather than the idle filler. */
        val contextual: Boolean,
    ) : AudienceEvent

    data class GiftSent(
        override val id: Long,
        override val atMs: Long,
        val from: AudienceMember,
        val gift: Gift,
        val reason: String?,
    ) : AudienceEvent

    data class Reaction(
        override val id: Long,
        override val atMs: Long,
        val kind: ReactionKind,
        val count: Int,
    ) : AudienceEvent

    data class Milestone(
        override val id: Long,
        override val atMs: Long,
        val viewers: Int,
    ) : AudienceEvent

    data class VipJoined(
        override val id: Long,
        override val atMs: Long,
        val member: AudienceMember,
    ) : AudienceEvent

    data class TrendingMoment(
        override val id: Long,
        override val atMs: Long,
        val reason: String,
    ) : AudienceEvent
}

/** What the AI (or fallback) is allowed to suggest. The scheduler decides whether anything happens. */
enum class EventSuggestion { NONE, SMALL_GIFT, MEDIUM_GIFT, MAJOR_GIFT, VIP_JOIN, TRENDING, REACTION_BURST }
