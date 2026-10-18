package org.mayad.fame_life.domain.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The signed-in account. Only the uid is used for data ownership; the email is shown in Settings. */
data class AccountUser(val uid: String, val email: String?)

sealed interface AuthState {
    /** The platform SDK has not reported the persisted session yet. */
    data object Checking : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: AccountUser) : AuthState
}

/** Why an account operation failed, independent of the SDK's exception types. */
enum class AuthErrorKind {
    /** The user closed the Google account picker. Not an error worth shouting about. */
    CANCELLED,
    /** No Google account on the device (Android) or Google sign-in is unavailable. */
    NO_GOOGLE_ACCOUNT,
    /** Re-confirmation used a different Google account than the signed-in one. */
    WRONG_ACCOUNT,
    TOO_MANY_REQUESTS,
    NETWORK,
    REQUIRES_RECENT_LOGIN,
    NOT_CONFIGURED,
    UNKNOWN,
}

class AuthException(val kind: AuthErrorKind, cause: Throwable? = null) : Exception(kind.name, cause)

/** What Google returns after the user picks an account. Passed straight to Firebase, never stored. */
data class GoogleTokens(val idToken: String, val accessToken: String? = null)

/**
 * Shows the platform's Google account picker and returns tokens. Android uses Credential Manager; iOS uses the
 * GoogleSignIn SDK through a small Swift bridge. Throws [AuthException] ([AuthErrorKind.CANCELLED] when dismissed).
 */
fun interface GoogleTokenProvider {
    suspend fun requestTokens(): GoogleTokens
}

/**
 * Account identity: Google accounts only. Screens and sync code depend on this interface; [FirebaseAuthService]
 * is the real one and tests use a fake. Every suspend function throws [AuthException] on failure.
 */
interface AuthService {
    val state: StateFlow<AuthState>

    /** Picks a Google account and signs in to Firebase with it. New accounts are created on first sign-in. */
    suspend fun signInWithGoogle(): AccountUser
    suspend fun signOut()

    /** Confirms the same Google account again. Firebase refuses to delete an account whose sign-in is not recent. */
    suspend fun reauthenticateWithGoogle()

    /** Deletes the signed-in account itself. Call [reauthenticateWithGoogle] first. */
    suspend fun deleteCurrentUser()
}

/**
 * Used when the build has no Firebase configuration. The welcome screens still work; the account step explains
 * that sign-in is unavailable on this build.
 */
class UnavailableAuthService : AuthService {
    override val state: StateFlow<AuthState> = MutableStateFlow<AuthState>(AuthState.SignedOut).asStateFlow()
    private fun unavailable(): Nothing = throw AuthException(AuthErrorKind.NOT_CONFIGURED)
    override suspend fun signInWithGoogle(): AccountUser = unavailable()
    override suspend fun signOut() = Unit
    override suspend fun reauthenticateWithGoogle() = unavailable()
    override suspend fun deleteCurrentUser() = unavailable()
}
