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
    INVALID_EMAIL,
    WEAK_PASSWORD,
    EMAIL_IN_USE,
    WRONG_CREDENTIALS,
    TOO_MANY_REQUESTS,
    NETWORK,
    REQUIRES_RECENT_LOGIN,
    NOT_CONFIGURED,
    UNKNOWN,
}

class AuthException(val kind: AuthErrorKind, cause: Throwable? = null) : Exception(kind.name, cause)

/**
 * Account identity. Screens and sync code depend on this interface only; [FirebaseAuthService] is the real one and
 * tests use a fake. Every suspend function throws [AuthException] on failure.
 */
interface AuthService {
    val state: StateFlow<AuthState>

    suspend fun signUp(email: String, password: String): AccountUser
    suspend fun signIn(email: String, password: String): AccountUser
    suspend fun sendPasswordReset(email: String)
    suspend fun signOut()

    /** Confirms the password again. Firebase refuses to delete an account whose sign-in is not recent. */
    suspend fun reauthenticate(password: String)

    /** Deletes the signed-in account itself. Call [reauthenticate] first. */
    suspend fun deleteCurrentUser()
}

/**
 * Used when the build has no Firebase configuration (for example iOS before `GoogleService-Info.plist` is added).
 * The welcome screens still work; the account step explains that sign-in is unavailable on this build.
 */
class UnavailableAuthService : AuthService {
    override val state: StateFlow<AuthState> = MutableStateFlow<AuthState>(AuthState.SignedOut).asStateFlow()
    private fun unavailable(): Nothing = throw AuthException(AuthErrorKind.NOT_CONFIGURED)
    override suspend fun signUp(email: String, password: String): AccountUser = unavailable()
    override suspend fun signIn(email: String, password: String): AccountUser = unavailable()
    override suspend fun sendPasswordReset(email: String) = unavailable()
    override suspend fun signOut() = Unit
    override suspend fun reauthenticate(password: String) = unavailable()
    override suspend fun deleteCurrentUser() = unavailable()
}

/** Client-side checks and user-facing copy. Firebase still validates everything on its side. */
object AuthRules {
    const val PASSWORD_MIN = 8
    private val emailShape = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Enter your email"
        !emailShape.matches(normalizeEmail(email)) -> "That doesn't look like an email address"
        else -> null
    }

    fun passwordError(password: String): String? = when {
        password.length < PASSWORD_MIN -> "Use at least $PASSWORD_MIN characters"
        password.none { it.isLetter() } || password.none { it.isDigit() } -> "Mix letters and numbers"
        else -> null
    }

    fun message(kind: AuthErrorKind): String = when (kind) {
        AuthErrorKind.INVALID_EMAIL -> "That email address isn't valid."
        AuthErrorKind.WEAK_PASSWORD -> "That password is too weak. Try a longer one."
        AuthErrorKind.EMAIL_IN_USE -> "An account already uses this email. Sign in instead."
        AuthErrorKind.WRONG_CREDENTIALS -> "Email or password is incorrect."
        AuthErrorKind.TOO_MANY_REQUESTS -> "Too many attempts. Wait a minute and try again."
        AuthErrorKind.NETWORK -> "You're offline. Check your connection and try again."
        AuthErrorKind.REQUIRES_RECENT_LOGIN -> "For your security, confirm your password and try again."
        AuthErrorKind.NOT_CONFIGURED -> "Accounts aren't set up in this build yet. The developer needs to add the Firebase configuration."
        AuthErrorKind.UNKNOWN -> "Something went wrong. Please try again."
    }
}
