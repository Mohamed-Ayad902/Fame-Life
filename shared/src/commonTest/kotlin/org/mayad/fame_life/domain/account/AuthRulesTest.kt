package org.mayad.fame_life.domain.account

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRulesTest {
    @Test
    fun everyRealFailureHasUserFacingCopyAndClosingThePickerHasNone() {
        assertNull(AuthRules.message(AuthErrorKind.CANCELLED))
        AuthErrorKind.entries.filter { it != AuthErrorKind.CANCELLED }
            .forEach { assertTrue(AuthRules.message(it)?.isNotBlank() == true, it.name) }
    }

    @Test
    fun unconfiguredBuildsRefuseAccountsHonestly() = runTest {
        val auth = UnavailableAuthService()
        assertEquals(AuthState.SignedOut, auth.state.value)
        assertEquals(AuthErrorKind.NOT_CONFIGURED, assertFailsWith<AuthException> { auth.signInWithGoogle() }.kind)
    }

    @Test
    fun fakeMatchesTheGoogleBehaviorTheAppReliesOn() = runTest {
        val auth = FakeAuthService()
        val first = auth.signInAs("mo@example.com")
        auth.signOut()
        assertEquals(first.uid, auth.signInAs("mo@example.com").uid, "the same Google account keeps its uid")
        auth.picked = null
        assertEquals(AuthErrorKind.CANCELLED, assertFailsWith<AuthException> { auth.reauthenticateWithGoogle() }.kind)
        auth.picked = "someone@example.com"
        assertEquals(AuthErrorKind.WRONG_ACCOUNT, assertFailsWith<AuthException> { auth.reauthenticateWithGoogle() }.kind)
    }
}
