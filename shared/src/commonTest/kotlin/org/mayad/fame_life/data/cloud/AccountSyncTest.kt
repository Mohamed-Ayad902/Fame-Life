package org.mayad.fame_life.data.cloud

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mayad.fame_life.core.platform.InMemoryTextStore
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.data.local.NotificationRepository
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.data.local.SessionHistoryRepository
import org.mayad.fame_life.domain.account.AccountUser
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.account.AuthException
import org.mayad.fame_life.domain.account.AuthState
import org.mayad.fame_life.domain.account.FakeAuthService
import org.mayad.fame_life.domain.simulation.SessionSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountSyncTest {
    private class Rig(
        scope: CoroutineScope,
        val auth: FakeAuthService = FakeAuthService(),
        val cloud: FakeCloudStore = FakeCloudStore(),
        val store: InMemoryTextStore = InMemoryTextStore(),
    ) {
        private val sink = DeferredCloudSink()
        val profiles = ProfileRepository(store, sink) { _, _ -> }
        val history = SessionHistoryRepository(store, sink) { _, _ -> }
        val notifications = NotificationRepository(store, sink) { _, _ -> }
        var errors = 0
        val sync = AccountSync(auth, cloud, store, profiles, history, notifications, scope, historyLimit = { 3 }, onError = { _, _ -> errors++ })
            .also { sink.target = it }
    }

    private fun profile(name: String = "Mo") = CreatorProfile(displayName = name, username = name.lowercase(), createdAtEpochMs = 1)

    private fun summary(id: String, startedAt: Long) = SessionSummary(
        id = id, title = "t", startedAtEpochMs = startedAt, durationSeconds = 60, preset = "RISING", liveStyleId = "aurora",
        peakViewers = 100, comments = 10, contextualComments = 8, reactions = 5, gifts = 1, giftPoints = 5,
        milestones = 0, famePointsEarned = 50,
    )

    private fun TestScope.rig(
        auth: FakeAuthService = FakeAuthService(),
        cloud: FakeCloudStore = FakeCloudStore(),
        store: InMemoryTextStore = InMemoryTextStore(),
    ) = Rig(backgroundScope, auth, cloud, store).also { runCurrent() }

    @Test
    fun newAccountIsReadyWithoutAProfileAndItsFirstProfileIsSavedUnderItsUid() = runTest {
        val rig = rig()
        val user = rig.auth.signUp("Mo@Example.com", "secret123")
        runCurrent()
        assertEquals(SyncState.Ready(), rig.sync.state.value)
        assertNull(rig.profiles.profile.value)

        rig.profiles.save(profile())
        runCurrent()
        assertEquals(profile(), rig.cloud.profiles[user.uid])
    }

    @Test
    fun signInPullsProfileHistoryAndInboxNewestFirst() = runTest {
        val rig = rig()
        val user = rig.auth.signUp("mo@example.com", "secret123")
        rig.auth.signOut()
        rig.cloud.profiles[user.uid] = profile("Cloud")
        rig.cloud.sessions[user.uid] = (1..5).associate { "s$it" to summary("s$it", it.toLong()) }.toMutableMap()
        rig.cloud.notifications[user.uid] = mutableMapOf("n1" to InAppNotification("n1", "k", "t", "b", 5))
        runCurrent()

        rig.auth.signIn("mo@example.com", "secret123")
        runCurrent()
        assertEquals("Cloud", rig.profiles.profile.value?.displayName)
        assertEquals(listOf("s5", "s4", "s3"), rig.history.sessions.value.map { it.id }, "bounded to the plan's history size")
        assertEquals(listOf("n1"), rig.notifications.notifications.value.map { it.id })
        assertEquals(0, rig.cloud.writes, "loading never writes back what it just read")
    }

    @Test
    fun signOutWipesTheDeviceCache() = runTest {
        val rig = rig()
        rig.auth.signUp("mo@example.com", "secret123")
        runCurrent()
        rig.profiles.save(profile())
        rig.history.save(summary("a", 1), keep = 3)
        rig.auth.signOut()
        runCurrent()
        assertNull(rig.profiles.profile.value)
        assertTrue(rig.history.sessions.value.isEmpty())
        assertEquals(SyncState.Idle, rig.sync.state.value)
    }

    @Test
    fun anotherAccountNeverSeesThePreviousAccountsCache() = runTest {
        val store = InMemoryTextStore()
        // Cache left by uid-a, then the app starts already signed in as uid-b while offline.
        store.write("account.owner.v1", "uid-a")
        ProfileRepository(store) { _, _ -> }.save(profile("Alice"))
        val cloud = FakeCloudStore().apply { offline = true }
        val rig = rig(auth = FakeAuthService(AuthState.SignedIn(AccountUser("uid-b", "b@example.com"))), cloud = cloud, store = store)
        assertNull(rig.profiles.profile.value)
        assertEquals(SyncState.Failed, rig.sync.state.value)
    }

    @Test
    fun offlineWithACachedProfileKeepsWorkingAndRetryRecovers() = runTest {
        val store = InMemoryTextStore()
        store.write("account.owner.v1", "uid-a")
        ProfileRepository(store) { _, _ -> }.save(profile("Cached"))
        val cloud = FakeCloudStore().apply { offline = true }
        val rig = rig(auth = FakeAuthService(AuthState.SignedIn(AccountUser("uid-a", "a@example.com"))), cloud = cloud, store = store)
        assertEquals(SyncState.Ready(offline = true), rig.sync.state.value)
        assertEquals("Cached", rig.profiles.profile.value?.displayName)

        cloud.offline = false
        rig.sync.retry()
        runCurrent()
        assertEquals(SyncState.Ready(), rig.sync.state.value)
        assertEquals("Cached", cloud.profiles["uid-a"]?.displayName, "a profile the cloud never got is uploaded, not lost")
    }

    @Test
    fun failedLoadWithNothingCachedCanBeRetried() = runTest {
        val cloud = FakeCloudStore().apply { offline = true }
        val rig = rig(auth = FakeAuthService(AuthState.SignedIn(AccountUser("uid-a", "a@example.com"))), cloud = cloud)
        assertEquals(SyncState.Failed, rig.sync.state.value)
        assertTrue(rig.errors > 0)
        cloud.offline = false
        cloud.profiles["uid-a"] = profile()
        rig.sync.retry()
        runCurrent()
        assertNotNull(rig.profiles.profile.value)
    }

    @Test
    fun sessionsTrimmedFromHistoryAreDeletedInTheCloud() = runTest {
        val rig = rig()
        val user = rig.auth.signUp("mo@example.com", "secret123")
        runCurrent()
        listOf("a", "b", "c").forEachIndexed { i, id -> rig.history.save(summary(id, i.toLong()), keep = 2) }
        runCurrent()
        assertEquals(setOf("b", "c"), rig.cloud.sessions[user.uid]?.keys?.toSet())
    }

    @Test
    fun readingTheInboxMirrorsOnlyChangedNotifications() = runTest {
        val rig = rig()
        val user = rig.auth.signUp("mo@example.com", "secret123")
        runCurrent()
        rig.notifications.add(InAppNotification("n1", "k", "t", "b", 1))
        runCurrent()
        val writes = rig.cloud.writes
        rig.notifications.markAllRead()
        rig.notifications.markAllRead()
        runCurrent()
        assertEquals(writes + 1, rig.cloud.writes)
        assertTrue(rig.cloud.notifications.getValue(user.uid).getValue("n1").read)
    }

    @Test
    fun deletingTheAccountNeedsThePasswordAndRemovesEverything() = runTest {
        val rig = rig()
        val user = rig.auth.signUp("mo@example.com", "secret123")
        runCurrent()
        rig.profiles.save(profile())
        rig.history.save(summary("a", 1), keep = 3)
        runCurrent()

        val wrong = assertFailsWith<AuthException> { rig.sync.deleteAccount("nope") }
        assertEquals(AuthErrorKind.WRONG_CREDENTIALS, wrong.kind)
        assertNotNull(rig.cloud.profiles[user.uid], "nothing is deleted when the password is wrong")

        rig.sync.deleteAccount("secret123")
        runCurrent()
        assertNull(rig.cloud.profiles[user.uid])
        assertNull(rig.cloud.sessions[user.uid])
        assertEquals(listOf(user.uid), rig.auth.deleted)
        assertNull(rig.profiles.profile.value)
    }

    @Test
    fun accountIsKeptWhenCloudDeletionFails() = runTest {
        val rig = rig()
        rig.auth.signUp("mo@example.com", "secret123")
        runCurrent()
        rig.profiles.save(profile())
        runCurrent()
        rig.cloud.offline = true
        assertFailsWith<AuthException> { rig.sync.deleteAccount("secret123") }
        assertTrue(rig.auth.deleted.isEmpty())
        assertNotNull(rig.profiles.profile.value)
    }
}
