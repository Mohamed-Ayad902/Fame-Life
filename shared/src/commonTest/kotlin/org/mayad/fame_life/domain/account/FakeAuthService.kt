package org.mayad.fame_life.domain.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory accounts for tests. Mirrors the Firebase rules the app relies on (unique email, password check). */
class FakeAuthService(initial: AuthState = AuthState.SignedOut) : AuthService {
    private val accounts = mutableMapOf<String, Pair<String, String>>() // email -> (uid, password)
    private val mutableState = MutableStateFlow(initial)
    override val state: StateFlow<AuthState> = mutableState
    var failNext: AuthErrorKind? = null
    var deleted = mutableListOf<String>()
    var resetRequests = mutableListOf<String>()
    private var nextUid = 1

    private fun maybeFail() {
        failNext?.let { failNext = null; throw AuthException(it) }
    }

    override suspend fun signUp(email: String, password: String): AccountUser {
        maybeFail()
        val key = AuthRules.normalizeEmail(email)
        if (key in accounts) throw AuthException(AuthErrorKind.EMAIL_IN_USE)
        val uid = "uid-${nextUid++}"
        accounts[key] = uid to password
        return AccountUser(uid, key).also { mutableState.value = AuthState.SignedIn(it) }
    }

    override suspend fun signIn(email: String, password: String): AccountUser {
        maybeFail()
        val key = AuthRules.normalizeEmail(email)
        val (uid, stored) = accounts[key] ?: throw AuthException(AuthErrorKind.WRONG_CREDENTIALS)
        if (stored != password) throw AuthException(AuthErrorKind.WRONG_CREDENTIALS)
        return AccountUser(uid, key).also { mutableState.value = AuthState.SignedIn(it) }
    }

    override suspend fun sendPasswordReset(email: String) {
        maybeFail()
        resetRequests += AuthRules.normalizeEmail(email)
    }

    override suspend fun signOut() {
        mutableState.value = AuthState.SignedOut
    }

    override suspend fun reauthenticate(password: String) {
        maybeFail()
        val user = (state.value as? AuthState.SignedIn)?.user ?: throw AuthException(AuthErrorKind.WRONG_CREDENTIALS)
        if (accounts[user.email]?.second != password) throw AuthException(AuthErrorKind.WRONG_CREDENTIALS)
    }

    override suspend fun deleteCurrentUser() {
        maybeFail()
        val user = (state.value as? AuthState.SignedIn)?.user ?: throw AuthException(AuthErrorKind.WRONG_CREDENTIALS)
        accounts.remove(user.email)
        deleted += user.uid
        mutableState.value = AuthState.SignedOut
    }
}
