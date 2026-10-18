package org.mayad.fame_life.data.cloud

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.FirebaseTooManyRequestsException
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.FirebaseUser
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
import org.mayad.fame_life.domain.account.AuthRules
import org.mayad.fame_life.domain.account.AuthService
import org.mayad.fame_life.domain.account.AuthState

/** Email and password accounts through Firebase Authentication (GitLive Kotlin Multiplatform SDK). */
class FirebaseAuthService(scope: CoroutineScope) : AuthService {
    private val auth = Firebase.auth

    // Firebase restores the persisted user before the first listener callback, so the initial value is usually final.
    override val state: StateFlow<AuthState> = auth.authStateChanged
        .map { it.toState() }
        .stateIn(scope, SharingStarted.Eagerly, auth.currentUser.toState())

    override suspend fun signUp(email: String, password: String): AccountUser = mapped {
        auth.createUserWithEmailAndPassword(AuthRules.normalizeEmail(email), password).user?.toAccount()
            ?: throw AuthException(AuthErrorKind.UNKNOWN)
    }

    override suspend fun signIn(email: String, password: String): AccountUser = mapped {
        auth.signInWithEmailAndPassword(AuthRules.normalizeEmail(email), password).user?.toAccount()
            ?: throw AuthException(AuthErrorKind.UNKNOWN)
    }

    override suspend fun sendPasswordReset(email: String) = mapped { auth.sendPasswordResetEmail(AuthRules.normalizeEmail(email)) }

    override suspend fun signOut() = mapped { auth.signOut() }

    override suspend fun reauthenticate(password: String) = mapped {
        val user = auth.currentUser ?: throw AuthException(AuthErrorKind.WRONG_CREDENTIALS)
        val email = user.email ?: throw AuthException(AuthErrorKind.UNKNOWN)
        user.reauthenticate(EmailAuthProvider.credential(email, password))
    }

    override suspend fun deleteCurrentUser() = mapped {
        (auth.currentUser ?: throw AuthException(AuthErrorKind.WRONG_CREDENTIALS)).delete()
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
        is FirebaseAuthWeakPasswordException -> AuthErrorKind.WEAK_PASSWORD
        is FirebaseAuthUserCollisionException -> AuthErrorKind.EMAIL_IN_USE
        is FirebaseAuthRecentLoginRequiredException -> AuthErrorKind.REQUIRES_RECENT_LOGIN
        is FirebaseAuthInvalidUserException -> AuthErrorKind.WRONG_CREDENTIALS
        // Firebase reports bad emails and wrong passwords with the same exception type; the message tells them apart.
        is FirebaseAuthInvalidCredentialsException ->
            if (e.message.orEmpty().contains("email", ignoreCase = true) && e.message.orEmpty().contains("badly formatted", ignoreCase = true)) {
                AuthErrorKind.INVALID_EMAIL
            } else {
                AuthErrorKind.WRONG_CREDENTIALS
            }
        is FirebaseAuthException -> AuthErrorKind.UNKNOWN
        else -> AuthErrorKind.UNKNOWN
    }
}
