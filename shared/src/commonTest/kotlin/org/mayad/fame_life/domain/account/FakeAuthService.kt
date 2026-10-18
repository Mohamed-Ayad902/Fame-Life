package org.mayad.fame_life.domain.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory Google accounts for tests. [picked] is the account the "picker" returns next; null means the user
 * closes the picker. Mirrors the Firebase behavior the app relies on: one uid per Google account, created on first
 * sign-in, and re-authentication must use the same account.
 */
class FakeAuthService(initial: AuthState = AuthState.SignedOut) : AuthService {
    private val uids = mutableMapOf<String, String>() // email -> uid
    private val mutableState = MutableStateFlow(initial)
    override val state: StateFlow<AuthState> = mutableState
    var picked: String? = "mo@example.com"
    var failNext: AuthErrorKind? = null
    val deleted = mutableListOf<String>()
    private var nextUid = 1

    private fun pick(): String {
        failNext?.let { failNext = null; throw AuthException(it) }
        return picked ?: throw AuthException(AuthErrorKind.CANCELLED)
    }

    /** Test helper: pick [email] and sign in with it. */
    suspend fun signInAs(email: String): AccountUser {
        picked = email
        return signInWithGoogle()
    }

    override suspend fun signInWithGoogle(): AccountUser {
        val email = pick()
        val uid = uids.getOrPut(email) { "uid-${nextUid++}" }
        return AccountUser(uid, email).also { mutableState.value = AuthState.SignedIn(it) }
    }

    override suspend fun signOut() {
        mutableState.value = AuthState.SignedOut
    }

    override suspend fun reauthenticateWithGoogle() {
        val user = (state.value as? AuthState.SignedIn)?.user ?: throw AuthException(AuthErrorKind.WRONG_ACCOUNT)
        if (pick() != user.email) throw AuthException(AuthErrorKind.WRONG_ACCOUNT)
    }

    override suspend fun deleteCurrentUser() {
        val user = (state.value as? AuthState.SignedIn)?.user ?: throw AuthException(AuthErrorKind.WRONG_ACCOUNT)
        uids.remove(user.email)
        deleted += user.uid
        mutableState.value = AuthState.SignedOut
    }
}
