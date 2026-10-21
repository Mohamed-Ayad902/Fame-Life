package org.mayad.fame_life.domain.economy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * The points economy as the app sees it. Points are a simulation currency: nothing here is money and nothing is
 * bought with them outside the app. The app never changes a balance itself: the backend grants and spends
 * (backend/src/economy.mjs) and writes `wallets/{uid}`, which the app only reads.
 */

/**
 * Admin-tuned values from the Firestore document `config/economy`, edited in the Firebase console. Missing fields take
 * the defaults below, which match the backend's DEFAULT_CONFIG so the app shows the same numbers the server charges.
 */
@Serializable
data class EconomyConfig(
    val enabled: Boolean = true,
    val startingBalance: Int = 60,
    /** Points for day 1, day 2, … of a streak; after the last day the cycle starts again. */
    val streakRewards: List<Int> = listOf(20, 25, 30, 40, 50, 60, 100),
    val defaultLiveCost: Int = 30,
    /** Cost to go live per live style id ([org.mayad.fame_life.feature.live.LiveStyles]). */
    val liveCosts: Map<String, Int> = mapOf("aurora" to 30, "noir" to 50),
    val adReward: Int = 25,
    val adsPerDay: Int = 5,
) {
    fun liveCost(styleId: String): Int = liveCosts[styleId] ?: defaultLiveCost

    /** Same field-by-field fallback as the server's parseConfig, so a bad admin value never breaks the screen. */
    fun sanitized(): EconomyConfig {
        val d = Default
        fun pts(v: Int, fallback: Int) = if (v in 0..MAX_POINTS) v else fallback
        return EconomyConfig(
            enabled = enabled,
            startingBalance = pts(startingBalance, d.startingBalance),
            streakRewards = streakRewards.takeIf { it.size in 1..MAX_STREAK_DAYS && it.all { v -> v in 0..MAX_POINTS } } ?: d.streakRewards,
            defaultLiveCost = pts(defaultLiveCost, d.defaultLiveCost),
            liveCosts = liveCosts.filterValues { it in 0..MAX_POINTS },
            adReward = pts(adReward, d.adReward),
            adsPerDay = if (adsPerDay in 0..100) adsPerDay else d.adsPerDay,
        )
    }

    companion object {
        val Default = EconomyConfig()
        const val MAX_POINTS = 100_000
        const val MAX_STREAK_DAYS = 31
    }
}

/** `wallets/{uid}`, written by the backend only. [lastClaimDay] is the user's local day number of the last check-in. */
@Serializable
data class Wallet(
    val balance: Int = 0,
    val streak: Int = 0,
    val lastClaimDay: Int = -1,
    val lastClaimAtMs: Long = 0,
    val adsDay: Int = -1,
    val adsToday: Int = 0,
)

enum class StreakDayState { CLAIMED, TODAY, UPCOMING }

data class StreakDay(val number: Int, val reward: Int, val state: StreakDayState)

/** The streak rules, mirrored from the server so the app can tell whether today's reward is waiting. */
object StreakRules {
    private const val DAY_MS = 86_400_000L
    /** Must match MIN_CLAIM_GAP_MS on the server. */
    const val MIN_CLAIM_GAP_MS = 6 * 3_600_000L

    fun localDay(nowMs: Long, offsetMinutes: Int): Int {
        val offset = offsetMinutes.coerceIn(-720, 840)
        return ((nowMs + offset * 60_000L).floorDiv(DAY_MS)).toInt()
    }

    fun canClaim(wallet: Wallet, nowMs: Long, offsetMinutes: Int): Boolean =
        localDay(nowMs, offsetMinutes) > wallet.lastClaimDay && nowMs - wallet.lastClaimAtMs >= MIN_CLAIM_GAP_MS

    /** The streak length counting today: what a claim now would make it, or the current one when already claimed. */
    fun streakToday(wallet: Wallet, nowMs: Long, offsetMinutes: Int): Int {
        if (!canClaim(wallet, nowMs, offsetMinutes)) return wallet.streak.coerceAtLeast(1)
        return if (wallet.lastClaimDay == localDay(nowMs, offsetMinutes) - 1) wallet.streak + 1 else 1
    }

    /** The current cycle of rewards (day 1 … day N) with what is claimed, waiting today, and still ahead. */
    fun calendar(wallet: Wallet, config: EconomyConfig, nowMs: Long, offsetMinutes: Int): List<StreakDay> {
        val rewards = config.streakRewards
        val claimable = canClaim(wallet, nowMs, offsetMinutes)
        val todayIndex = (streakToday(wallet, nowMs, offsetMinutes) - 1) % rewards.size
        return rewards.mapIndexed { i, reward ->
            val state = when {
                i < todayIndex -> StreakDayState.CLAIMED
                i == todayIndex -> if (claimable) StreakDayState.TODAY else StreakDayState.CLAIMED
                else -> StreakDayState.UPCOMING
            }
            StreakDay(number = i + 1, reward = reward, state = state)
        }
    }

    fun adsLeftToday(wallet: Wallet, config: EconomyConfig, nowMs: Long): Int =
        config.adsPerDay - if (wallet.adsDay == localDay(nowMs, 0)) wallet.adsToday else 0
}

/** What the Home HUD, the streak dialog and the live studio read. */
sealed interface PointsState {
    /** No backend in this build, signed out, or an admin switched the economy off: lives are free. */
    data object Unavailable : PointsState
    data object Loading : PointsState
    data class Ready(
        val balance: Int,
        /** Consecutive check-in days, counting today once it's claimed. */
        val streakDays: Int,
        val canClaimToday: Boolean,
        val calendar: List<StreakDay>,
        val config: EconomyConfig,
        val adsLeftToday: Int,
    ) : PointsState {
        /** Today's reward if it is still waiting. */
        val todayReward: Int? get() = calendar.firstOrNull { it.state == StreakDayState.TODAY }?.reward
    }
}

/** Why a points request didn't go through. The screen maps it to copy. */
enum class PointsError { NETWORK, UNAVAILABLE, SERVER }

sealed interface ClaimResult {
    data class Claimed(val reward: Int, val streakDays: Int) : ClaimResult
    data object AlreadyClaimed : ClaimResult
    data class Failed(val error: PointsError) : ClaimResult
}

sealed interface SpendResult {
    data class Paid(val cost: Int) : SpendResult
    /** The economy is off: the live is free. */
    data object Free : SpendResult
    data class NotEnough(val cost: Int, val balance: Int) : SpendResult
    data class Failed(val error: PointsError) : SpendResult
}

/**
 * The points wallet. Every change is made by the server; [state] follows the wallet document live, so a reward ad
 * credited by AdMob's server callback shows up without the app asking.
 */
interface PointsService {
    val state: StateFlow<PointsState>

    suspend fun claimDailyReward(): ClaimResult

    /** Charges the live style's cost for [sessionId]. Calling it again for the same session never charges twice. */
    suspend fun payForLive(sessionId: String, styleId: String): SpendResult

    /** The uid the reward ad is verified for, or null when signed out. */
    fun rewardUserId(): String?
}

/** For builds without a backend: no points, lives are free. */
class UnavailablePointsService : PointsService {
    override val state: StateFlow<PointsState> = MutableStateFlow<PointsState>(PointsState.Unavailable).asStateFlow()
    override suspend fun claimDailyReward(): ClaimResult = ClaimResult.Failed(PointsError.UNAVAILABLE)
    override suspend fun payForLive(sessionId: String, styleId: String): SpendResult = SpendResult.Free
    override fun rewardUserId(): String? = null
}

/** How a rewarded ad ended. Only the server decides whether it earned points (AdMob's signed callback). */
enum class AdOutcome { EARNED, DISMISSED, NOT_READY, FAILED }

/** Rewarded video ads (AdMob on Android). [isAvailable] is false until real AdMob ids are configured. */
interface RewardedAds {
    val isAvailable: Boolean

    /** Loads (if needed) and shows one ad, with [userId] attached for server-side verification. */
    suspend fun show(userId: String): AdOutcome
}

object NoRewardedAds : RewardedAds {
    override val isAvailable = false
    override suspend fun show(userId: String) = AdOutcome.NOT_READY
}
