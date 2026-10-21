package org.mayad.fame_life.domain.economy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.mayad.fame_life.core.analytics.InMemoryAnalytics
import org.mayad.fame_life.core.platform.InMemoryTextStore
import org.mayad.fame_life.data.economy.CheckInResponse
import org.mayad.fame_life.data.economy.CloudPointsService
import org.mayad.fame_life.data.economy.EconomyStore
import org.mayad.fame_life.data.economy.PointsApi
import org.mayad.fame_life.data.economy.SpendResponse
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.EntitlementRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.data.remote.BackendException
import org.mayad.fame_life.domain.account.AccountUser
import org.mayad.fame_life.domain.account.AuthState
import org.mayad.fame_life.domain.account.FakeAuthService
import org.mayad.fame_life.feature.ViewModelTest
import org.mayad.fame_life.feature.live.AdStatus
import org.mayad.fame_life.feature.live.LiveSetupEvent
import org.mayad.fame_life.feature.live.LiveSetupIntent
import org.mayad.fame_life.feature.live.LiveSetupViewModel
import org.mayad.fame_life.feature.points.DailyRewardIntent
import org.mayad.fame_life.feature.points.DailyRewardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val DAY = 86_400_000L
private const val HOUR = 3_600_000L

class StreakRulesTest {
    private val config = EconomyConfig.Default

    @Test
    fun aClaimNeedsANewLocalDayAndTheMinimumGap() {
        val wallet = Wallet(streak = 1, lastClaimDay = 10, lastClaimAtMs = 10 * DAY + 23 * HOUR)
        assertFalse(StreakRules.canClaim(wallet, 11 * DAY + HOUR, 0)) // a new day, but only two hours later
        assertTrue(StreakRules.canClaim(wallet, 11 * DAY + 6 * HOUR, 0))
        // Shifting the time zone back makes it the same local day again.
        assertFalse(StreakRules.canClaim(wallet, 11 * DAY + 6 * HOUR, -600))
    }

    @Test
    fun theCalendarMarksClaimedTodayAndUpcomingDays() {
        val wallet = Wallet(streak = 2, lastClaimDay = 9, lastClaimAtMs = 9 * DAY)
        val days = StreakRules.calendar(wallet, config, 10 * DAY + 12 * HOUR, 0)
        assertEquals(config.streakRewards.size, days.size)
        assertEquals(listOf(StreakDayState.CLAIMED, StreakDayState.CLAIMED, StreakDayState.TODAY), days.take(3).map { it.state })
        assertTrue(days.drop(3).all { it.state == StreakDayState.UPCOMING })
        // Missing a day starts again from day 1.
        assertEquals(StreakDayState.TODAY, StreakRules.calendar(wallet, config, 12 * DAY, 0).first().state)
        // The cycle wraps after the last day.
        val full = Wallet(streak = config.streakRewards.size, lastClaimDay = 9, lastClaimAtMs = 9 * DAY)
        assertEquals(StreakDayState.TODAY, StreakRules.calendar(full, config, 10 * DAY + 12 * HOUR, 0).first().state)
    }

    @Test
    fun badAdminValuesFallBackFieldByField() {
        val bad = EconomyConfig(streakRewards = emptyList(), adReward = -5, liveCosts = mapOf("noir" to -1, "aurora" to 10)).sanitized()
        assertEquals(EconomyConfig.Default.streakRewards, bad.streakRewards)
        assertEquals(EconomyConfig.Default.adReward, bad.adReward)
        assertEquals(10, bad.liveCost("aurora"))
        assertEquals(bad.defaultLiveCost, bad.liveCost("noir"))
    }
}

/** A wallet the "server" controls, following backend/src/economy.mjs for the parts the app relies on. */
private class FakeServer(var now: Long) : EconomyStore, PointsApi {
    val wallet = MutableStateFlow<Wallet?>(null)
    val config = MutableStateFlow<EconomyConfig?>(EconomyConfig(liveCosts = mapOf("aurora" to 30, "noir" to 50), startingBalance = 40))
    val charged = mutableListOf<String>()
    var failNext: Exception? = null

    override fun wallet(uid: String) = wallet
    override fun config() = config

    private fun current() = wallet.value ?: Wallet(balance = config.value!!.startingBalance)

    override suspend fun checkIn(offsetMinutes: Int): CheckInResponse {
        val w = current()
        if (!StreakRules.canClaim(w, now, offsetMinutes)) return CheckInResponse(claimed = false, wallet = w)
        val streak = StreakRules.streakToday(w, now, offsetMinutes)
        val rewards = config.value!!.streakRewards
        val reward = rewards[(streak - 1) % rewards.size]
        val next = w.copy(balance = w.balance + reward, streak = streak, lastClaimDay = StreakRules.localDay(now, offsetMinutes), lastClaimAtMs = now)
        wallet.value = next
        return CheckInResponse(true, reward, next)
    }

    override suspend fun spend(sessionId: String, styleId: String): SpendResponse {
        val cost = config.value!!.liveCost(styleId)
        val w = current()
        if (sessionId !in charged) {
            if (w.balance < cost) throw BackendException(402)
            charged += sessionId
            wallet.value = w.copy(balance = w.balance - cost)
        }
        // A lost response: the server charged, the app never heard back.
        failNext?.let { failNext = null; throw it }
        return SpendResponse(cost, wallet.value)
    }

    fun creditAd(points: Int) {
        wallet.value = current().let { it.copy(balance = it.balance + points) }
    }
}

private class FakeAds(private val outcome: AdOutcome, private val onShow: () -> Unit = {}) : RewardedAds {
    override val isAvailable = true
    override suspend fun show(userId: String): AdOutcome = outcome.also { onShow() }
}

class PointsFlowTest : ViewModelTest() {
    private val start = 100 * DAY + 12 * HOUR
    private val server = FakeServer(start)
    private val auth = FakeAuthService(AuthState.SignedIn(AccountUser("uid-1", "mo@example.com")))
    private val profiles = ProfileRepository(InMemoryTextStore()) { _, _ -> }.apply {
        save(CreatorProfile(displayName = "Mo", username = "mo", createdAtEpochMs = 1))
    }

    private fun service(scope: CoroutineScope) =
        CloudPointsService(auth, server, server, { server.now }, { 0 }, scope, { _, _ -> }, tickMs = 1_000_000_000)

    private fun studio(points: PointsService, ads: RewardedAds = NoRewardedAds) = LiveSetupViewModel(
        "", profiles, SessionHistoryRepository(InMemoryTextStore()) { _, _ -> }, EntitlementRepository(), points, ads, InMemoryAnalytics(),
    ) { server.now }

    /** A wallet that already collected today's reward, so the tests start from a known balance. */
    private fun checkedIn(balance: Int) = Wallet(balance = balance, streak = 1, lastClaimDay = StreakRules.localDay(start, 0), lastClaimAtMs = start)

    @Test
    fun aWalletTheServerHasNotCreatedYetShowsTheStartingBalanceAndAWaitingReward() = runTest(dispatcher) {
        val points = service(backgroundScope)
        val ready = assertIs<PointsState.Ready>(points.state.first { it is PointsState.Ready })
        assertEquals(40, ready.balance)
        assertTrue(ready.canClaimToday)
        assertEquals(EconomyConfig.Default.streakRewards.first(), ready.todayReward)
    }

    @Test
    fun theStreakDialogOpensByItselfAndShowsWhatTheServerGranted() = runTest(dispatcher) {
        val points = service(backgroundScope)
        val dialog = DailyRewardViewModel(points, InMemoryAnalytics())
        assertTrue(dialog.state.value.visible)
        dialog.onIntent(DailyRewardIntent.Claim)
        assertEquals(EconomyConfig.Default.streakRewards.first(), dialog.state.value.claimed)
        assertNull(dialog.state.value.todayReward)
        dialog.onIntent(DailyRewardIntent.Dismiss)
        assertFalse(dialog.state.value.visible)
        assertEquals(40 + EconomyConfig.Default.streakRewards.first(), (points.state.value as PointsState.Ready).balance)
    }

    @Test
    fun goingLiveChargesTheStyleCost() = runTest(dispatcher) {
        server.wallet.value = checkedIn(40)
        val points = service(backgroundScope)
        val vm = studio(points)
        assertEquals(30, vm.state.value.cost)
        vm.onIntent(LiveSetupIntent.GoLive(camera = false, mic = true))
        assertIs<LiveSetupEvent.Start>(vm.events.first())
        assertEquals(10, (points.state.value as PointsState.Ready).balance)
    }

    @Test
    fun aLostResponseRetriesWithTheSameSessionSoItIsNeverChargedTwice() = runTest(dispatcher) {
        server.wallet.value = checkedIn(100)
        val points = service(backgroundScope)
        val vm = studio(points)
        server.failNext = IllegalStateException("socket closed")
        vm.onIntent(LiveSetupIntent.GoLive(camera = false, mic = true))
        assertEquals(PointsError.NETWORK, vm.state.value.pointsError)
        vm.onIntent(LiveSetupIntent.GoLive(camera = false, mic = true))
        val started = assertIs<LiveSetupEvent.Start>(vm.events.first())
        assertEquals(listOf(started.sessionId), server.charged)
        assertEquals(70, (points.state.value as PointsState.Ready).balance)
    }

    @Test
    fun shortOfPointsOffersAnAdAndClearsOnceTheServerCreditsIt() = runTest(dispatcher) {
        server.wallet.value = checkedIn(20)
        val points = service(backgroundScope)
        val vm = studio(points, FakeAds(AdOutcome.EARNED) { server.creditAd(25) })
        vm.onIntent(LiveSetupIntent.GoLive(camera = false, mic = true))
        assertEquals(30, vm.state.value.shortfall?.cost)
        assertTrue(server.charged.isEmpty())

        vm.onIntent(LiveSetupIntent.WatchAd)
        assertEquals(AdStatus.CREDITED, vm.state.value.adStatus)
        assertEquals(25, vm.state.value.adCredited)
        assertNull(vm.state.value.shortfall) // 45 ≥ 30
    }

    @Test
    fun anAdTheServerHasNotConfirmedYetSaysThePointsAreOnTheirWay() = runTest(dispatcher) {
        server.wallet.value = checkedIn(0)
        val points = service(backgroundScope)
        val vm = studio(points, FakeAds(AdOutcome.EARNED))
        vm.onIntent(LiveSetupIntent.GoLive(camera = false, mic = true))
        vm.onIntent(LiveSetupIntent.WatchAd)
        testScheduler.advanceTimeBy(31_000)
        assertEquals(AdStatus.PENDING, vm.state.value.adStatus)
    }

    @Test
    fun withoutAnEconomyLivesAreFree() = runTest(dispatcher) {
        val vm = studio(UnavailablePointsService())
        assertNull(vm.state.value.cost)
        vm.onIntent(LiveSetupIntent.GoLive(camera = false, mic = true))
        assertIs<LiveSetupEvent.Start>(vm.events.first())
    }

    @Test
    fun anAdminCanSwitchTheEconomyOff() = runTest(dispatcher) {
        server.config.value = EconomyConfig(enabled = false)
        val points = service(backgroundScope)
        assertEquals(PointsState.Unavailable, points.state.first { it !is PointsState.Loading })
    }
}
