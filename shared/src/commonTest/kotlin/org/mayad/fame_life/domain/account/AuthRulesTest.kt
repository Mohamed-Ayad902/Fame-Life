package org.mayad.fame_life.domain.account

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRulesTest {
    @Test
    fun emailsAreTrimmedLowercasedAndShapeChecked() {
        assertEquals("mo@example.com", AuthRules.normalizeEmail("  Mo@Example.COM "))
        assertNull(AuthRules.emailError(" mo@example.com "))
        assertNotNull(AuthRules.emailError(""))
        assertNotNull(AuthRules.emailError("mo@example"))
        assertNotNull(AuthRules.emailError("mo example@x.com"))
    }

    @Test
    fun newPasswordsNeedLengthLettersAndDigits() {
        assertNotNull(AuthRules.passwordError("short1"))
        assertNotNull(AuthRules.passwordError("onlyletters"))
        assertNotNull(AuthRules.passwordError("12345678"))
        assertNull(AuthRules.passwordError("letters123"))
    }

    @Test
    fun everyFailureHasUserFacingCopy() {
        AuthErrorKind.entries.forEach { assertTrue(AuthRules.message(it).isNotBlank(), it.name) }
    }

    @Test
    fun unconfiguredBuildsRefuseAccountsHonestly() = runTest {
        val auth = UnavailableAuthService()
        assertEquals(AuthState.SignedOut, auth.state.value)
        val error = assertFailsWith<AuthException> { auth.signUp("mo@example.com", "letters123") }
        assertEquals(AuthErrorKind.NOT_CONFIGURED, error.kind)
    }

    @Test
    fun fakeMatchesFirebaseRulesTheAppReliesOn() = runTest {
        val auth = FakeAuthService()
        auth.signUp("mo@example.com", "letters123")
        assertEquals(AuthErrorKind.EMAIL_IN_USE, assertFailsWith<AuthException> { auth.signUp("MO@example.com", "other123") }.kind)
        auth.signOut()
        assertEquals(AuthErrorKind.WRONG_CREDENTIALS, assertFailsWith<AuthException> { auth.signIn("mo@example.com", "wrong123") }.kind)
        auth.signIn("mo@example.com", "letters123")
        assertTrue(auth.state.value is AuthState.SignedIn)
    }
}
