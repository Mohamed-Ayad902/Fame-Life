package org.mayad.fame_life.data.cloud

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.FirebaseTooManyRequestsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.mayad.fame_life.domain.account.AccountUser
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.account.AuthException
import org.mayad.fame_life.domain.account.AuthService
import org.mayad.fame_life.domain.account.AuthState
import org.mayad.fame_life.domain.account.GoogleTokenProvider

/** Google accounts through Firebase Authentication (GitLive Kotlin Multiplatform SDK). */
class FirebaseAuthService(scope: CoroutineScope, private val google: GoogleTokenProvider) : AuthService {
    private val auth = Firebase.auth

    // Firebase restores the persisted user before the first listener callback, so the initial value is usually final.
    override val state: StateFlow<AuthState> = auth.authStateChanged
        .map { it.toState() }
        .stateIn(scope, SharingStarted.Eagerly, auth.currentUser.toState())

    override suspend fun signInWithGoogle(): AccountUser {
        val tokens = google.requestTokens()
        return mapped {
            auth.signInWithCredential(GoogleAuthProvider.credential(tokens.idToken, tokens.accessToken)).user?.toAccount()
                ?: throw AuthException(AuthErrorKind.UNKNOWN)
        }
    }

    override suspend fun signOut() = mapped { auth.signOut() }

    override suspend fun reauthenticateWithGoogle() {
        val user = auth.currentUser ?: throw AuthException(AuthErrorKind.WRONG_ACCOUNT)
        val tokens = google.requestTokens()
        mapped { user.reauthenticate(GoogleAuthProvider.credential(tokens.idToken, tokens.accessToken)) }
    }

    override suspend fun deleteCurrentUser() = mapped {
        (auth.currentUser ?: throw AuthException(AuthErrorKind.WRONG_ACCOUNT)).delete()
    }

    private fun FirebaseUser?.toState(): AuthState = if (this == null) AuthState.SignedOut else AuthState.SignedIn(toAccount())
    private fun FirebaseUser.toAccount() = AccountUser(uid = uid, email = email)

    private suspend fun <T> mapped(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: AuthException) {
        throw e
    } catch (e: Exception) {
        throw AuthException(classify(e), e)
    }

    private fun classify(e: Exception): AuthErrorKind = when (e) {
        is FirebaseNetworkException -> AuthErrorKind.NETWORK
        is FirebaseTooManyRequestsException -> AuthErrorKind.TOO_MANY_REQUESTS
        is FirebaseAuthRecentLoginRequiredException -> AuthErrorKind.REQUIRES_RECENT_LOGIN
        // Re-authenticating with a different Google account reports a user mismatch.
        is FirebaseAuthInvalidUserException -> AuthErrorKind.WRONG_ACCOUNT
        else -> if (e.message.orEmpty().contains("mismatch", ignoreCase = true)) AuthErrorKind.WRONG_ACCOUNT else AuthErrorKind.UNKNOWN
    }
}
