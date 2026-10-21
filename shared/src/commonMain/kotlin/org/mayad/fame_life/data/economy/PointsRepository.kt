package org.mayad.fame_life.data.economy

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mayad.fame_life.core.platform.EpochClock
import org.mayad.fame_life.data.remote.BackendException
import org.mayad.fame_life.data.remote.PulseBackend
import org.mayad.fame_life.domain.account.AuthService
import org.mayad.fame_life.domain.account.AuthState
import org.mayad.fame_life.domain.economy.ClaimResult
import org.mayad.fame_life.domain.economy.EconomyConfig
import org.mayad.fame_life.domain.economy.PointsError
import org.mayad.fame_life.domain.economy.PointsService
import org.mayad.fame_life.domain.economy.PointsState
import org.mayad.fame_life.domain.economy.SpendResult
import org.mayad.fame_life.domain.economy.StreakRules
import org.mayad.fame_life.domain.economy.Wallet

/** Live reads of the two documents the economy needs. Both are server-write-only (firestore.rules). */
interface EconomyStore {
    /** `wallets/{uid}`; null until the server creates it on the first check-in or live. */
    fun wallet(uid: String): Flow<Wallet?>

    /** `config/economy`; null when an admin hasn't created it (the defaults apply). */
    fun config(): Flow<EconomyConfig?>
}

class FirestoreEconomyStore : EconomyStore {
    private val db = Firebase.firestore

    override fun wallet(uid: String): Flow<Wallet?> =
        db.collection("wallets").document(uid).snapshots.map { if (it.exists) it.data(WalletDoc.serializer()).toWallet() else null }

    override fun config(): Flow<EconomyConfig?> =
        db.collection("config").document("economy").snapshots.map { if (it.exists) it.data(ConfigDoc.serializer()).toConfig() else null }
}

/** Every field the server writes, so decoding never trips over one the app doesn't use. */
@Serializable
private data class WalletDoc(
    val balance: Int = 0,
    val streak: Int = 0,
    val lastClaimDay: Int = -1,
    val lastClaimAtMs: Long = 0,
    val adsDay: Int = -1,
    val adsToday: Int = 0,
    val createdAtMs: Long = 0,
    val updatedAtMs: Long = 0,
) {
    fun toWallet() = Wallet(balance, streak, lastClaimDay, lastClaimAtMs, adsDay, adsToday)
}

@Serializable
private data class ConfigDoc(
    val enabled: Boolean = EconomyConfig.Default.enabled,
    val startingBalance: Int = EconomyConfig.Default.startingBalance,
    val streakRewards: List<Int> = EconomyConfig.Default.streakRewards,
    val defaultLiveCost: Int = EconomyConfig.Default.defaultLiveCost,
    val liveCosts: Map<String, Int> = EconomyConfig.Default.liveCosts,
    val adReward: Int = EconomyConfig.Default.adReward,
    val adsPerDay: Int = EconomyConfig.Default.adsPerDay,
    /** Read by the server only. */
    val adUnits: List<String> = emptyList(),
) {
    fun toConfig() = EconomyConfig(enabled, startingBalance, streakRewards, defaultLiveCost, liveCosts, adReward, adsPerDay)
}

/** The backend's points routes (backend/README.md). */
interface PointsApi {
    suspend fun checkIn(offsetMinutes: Int): CheckInResponse
    suspend fun spend(sessionId: String, styleId: String): SpendResponse
}

@Serializable
data class CheckInResponse(val claimed: Boolean = false, val reward: Int = 0, val wallet: Wallet? = null)

@Serializable
data class SpendResponse(val cost: Int = 0, val wallet: Wallet? = null)

private val json = Json { ignoreUnknownKeys = true }

class BackendPointsApi(private val backend: PulseBackend) : PointsApi {
    override suspend fun checkIn(offsetMinutes: Int): CheckInResponse =
        json.decodeFromString(CheckInResponse.serializer(), post("/v1/points/checkin", """{"tz_offset_minutes":$offsetMinutes}"""))

    override suspend fun spend(sessionId: String, styleId: String): SpendResponse =
        json.decodeFromString(SpendResponse.serializer(), post("/v1/points/spend", json.encodeToString(SpendRequest.serializer(), SpendRequest(sessionId, styleId))))

    private suspend fun post(path: String, body: String) = backend.post(path, body.encodeToByteArray(), "application/json")
}

@Serializable
private data class SpendRequest(@SerialName("session_id") val sessionId: String, @SerialName("style_id") val styleId: String)

/**
 * [PointsService] over the wallet document (read live from Firestore) and the backend (every change). The state
 * re-evaluates each minute as well, so a streak reward becomes claimable at local midnight while the app is open.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CloudPointsService(
    private val auth: AuthService,
    store: EconomyStore,
    private val api: PointsApi,
    private val clock: EpochClock,
    private val utcOffsetMinutes: (nowMs: Long) -> Int,
    scope: CoroutineScope,
    private val onError: (String, Throwable) -> Unit,
    tickMs: Long = 60_000,
) : PointsService {

    private val ticks = flow {
        while (true) {
            emit(Unit)
            delay(tickMs)
        }
    }

    override val state: StateFlow<PointsState> = auth.state.flatMapLatest { auth ->
        when (auth) {
            is AuthState.SignedIn -> combine(
                store.wallet(auth.user.uid).catch { onError("wallet read", it); emit(null) },
                store.config().catch { onError("economy config read", it); emit(null) },
                ticks,
            ) { wallet, config, _ -> readyState(wallet, (config ?: EconomyConfig.Default).sanitized()) }
            AuthState.Checking -> flowOf(PointsState.Loading)
            AuthState.SignedOut -> flowOf(PointsState.Unavailable)
        }
    }.stateIn(scope, SharingStarted.Eagerly, PointsState.Loading)

    private fun readyState(stored: Wallet?, config: EconomyConfig): PointsState {
        if (!config.enabled) return PointsState.Unavailable
        // Before the server's first write the wallet would start with the starting balance.
        val wallet = stored ?: Wallet(balance = config.startingBalance)
        val now = clock.now()
        val offset = utcOffsetMinutes(now)
        val claimable = StreakRules.canClaim(wallet, now, offset)
        // A streak survives until the end of the day after its last check-in.
        val alive = wallet.lastClaimDay >= StreakRules.localDay(now, offset) - 1
        return PointsState.Ready(
            balance = wallet.balance,
            streakDays = if (alive) wallet.streak else 0,
            canClaimToday = claimable,
            calendar = StreakRules.calendar(wallet, config, now, offset),
            config = config,
            adsLeftToday = StreakRules.adsLeftToday(wallet, config, now).coerceAtLeast(0),
        )
    }

    override suspend fun claimDailyReward(): ClaimResult = guard({ ClaimResult.Failed(it) }) {
        val response = api.checkIn(utcOffsetMinutes(clock.now()))
        if (response.claimed) ClaimResult.Claimed(response.reward, response.wallet?.streak ?: 1) else ClaimResult.AlreadyClaimed
    }

    override suspend fun payForLive(sessionId: String, styleId: String): SpendResult {
        val ready = state.value as? PointsState.Ready ?: return SpendResult.Free
        return guard({ SpendResult.Failed(it) }) {
            try {
                val response = api.spend(sessionId, styleId)
                if (response.wallet == null) SpendResult.Free else SpendResult.Paid(response.cost)
            } catch (e: BackendException) {
                if (e.status != PAYMENT_REQUIRED) throw e
                SpendResult.NotEnough(ready.config.liveCost(styleId), ready.balance)
            }
        }
    }

    override fun rewardUserId(): String? = (auth.state.value as? AuthState.SignedIn)?.user?.uid

    private suspend fun <T> guard(failed: (PointsError) -> T, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: BackendException) {
        onError("points request", e)
        failed(if (e.status == UNAVAILABLE) PointsError.UNAVAILABLE else PointsError.SERVER)
    } catch (e: Exception) {
        // HttpURLConnection's IOExceptions: no connection, timeout.
        onError("points request", e)
        failed(PointsError.NETWORK)
    }

    private companion object {
        const val PAYMENT_REQUIRED = 402
        const val UNAVAILABLE = 503
    }
}
