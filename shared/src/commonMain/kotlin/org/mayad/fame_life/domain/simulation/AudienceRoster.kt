package org.mayad.fame_life.domain.simulation

import kotlin.random.Random

/**
 * A fixed cast of simulated viewers for one session. Members are picked from this roster so the same
 * people keep showing up, the way regulars do in a real chat.
 */
class AudienceRoster(size: Int, language: AudienceLanguage, random: Random) {
    val members: List<AudienceMember>

    init {
        val firsts = when (language) {
            AudienceLanguage.ENGLISH -> englishNames
            AudienceLanguage.ARABIC -> arabicNames
            AudienceLanguage.MIXED -> englishNames + arabicNames
        }
        val used = mutableSetOf<String>()
        members = List(size) { index ->
            var handle: String
            do {
                handle = firsts.random(random) + suffixes.random(random)
            } while (!used.add(handle))
            AudienceMember(
                id = "m$index",
                handle = handle,
                hue = random.nextFloat() * 360f,
                isVip = index < VIP_COUNT,
            )
        }
    }

    private val regulars get() = members.drop(VIP_COUNT)

    /** Regulars talk more often than the long tail, which keeps the chat feeling like a community. */
    fun pickCommenter(random: Random): AudienceMember {
        val pool = regulars
        val index = if (random.nextDouble() < 0.55) random.nextInt(minOf(12, pool.size)) else random.nextInt(pool.size)
        return pool[index]
    }

    fun pickVip(random: Random): AudienceMember = members.take(VIP_COUNT).random(random)

    private companion object {
        const val VIP_COUNT = 3
        val englishNames = listOf(
            "maya", "jordan", "leo", "sky", "ava", "kai", "nina", "theo", "zoe", "eli", "ruby", "milo",
            "iris", "jules", "sam", "luna", "max", "cleo", "finn", "remy",
        )
        val arabicNames = listOf(
            "noura", "omar", "layla", "yousef", "salma", "karim", "hala", "zaid", "rania", "adam", "mariam",
            "tarek", "dina", "hamza", "lina", "fares",
        )
        val suffixes = listOf(
            "", ".live", "_vibes", "xo", "99", ".fm", "_daily", "__", ".k", "_on_air", "777", ".wav", "_x",
        )
    }
}
