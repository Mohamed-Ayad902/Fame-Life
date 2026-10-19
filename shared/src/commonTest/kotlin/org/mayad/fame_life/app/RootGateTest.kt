package org.mayad.fame_life.app

import org.mayad.fame_life.data.cloud.SyncState
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.domain.account.AccountUser
import org.mayad.fame_life.domain.account.AuthState
import kotlin.test.Test
import kotlin.test.assertEquals

class RootGateTest {
    private val signedIn = AuthState.SignedIn(AccountUser("u1", "a@b.c"))
    private val profile = CreatorProfile(displayName = "Mo", username = "mo", createdAtEpochMs = 1)

    @Test
    fun splashHoldsUntilTheMinimumTimeAndAuthAreReady() {
        assertEquals(Gate.SPLASH, RootViewModel.gateFor(false, signedIn, SyncState.Ready(), profile))
        assertEquals(Gate.SPLASH, RootViewModel.gateFor(true, AuthState.Checking, SyncState.Idle, null))
    }

    @Test
    fun signedOutGoesToWelcome() {
        assertEquals(Gate.WELCOME, RootViewModel.gateFor(true, AuthState.SignedOut, SyncState.Idle, null))
    }

    @Test
    fun signedInRoutesByProfileAndSync() {
        assertEquals(Gate.MAIN, RootViewModel.gateFor(true, signedIn, SyncState.Loading, profile))
        assertEquals(Gate.SETUP, RootViewModel.gateFor(true, signedIn, SyncState.Ready(), null))
        assertEquals(Gate.SYNC_ERROR, RootViewModel.gateFor(true, signedIn, SyncState.Failed, null))
        assertEquals(Gate.SPLASH, RootViewModel.gateFor(true, signedIn, SyncState.Loading, null))
    }
}
