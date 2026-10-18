package org.mayad.fame_life.core.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.account.AuthException
import org.mayad.fame_life.domain.account.GoogleTokenProvider
import org.mayad.fame_life.domain.account.GoogleTokens
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implemented in Swift (iosApp/iosApp/GoogleSignInBridge.swift) with the GoogleSignIn SDK, which has no Kotlin
 * binding. Exactly one of the callbacks is called.
 */
interface IosGoogleSignIn {
    fun signIn(onSuccess: (idToken: String, accessToken: String) -> Unit, onCancel: () -> Unit, onError: (message: String) -> Unit)
}

/** Set once from iOSApp.swift at launch. */
object PulseIos {
    var googleSignIn: IosGoogleSignIn? = null
}

private object BridgedGoogle : GoogleTokenProvider {
    override suspend fun requestTokens(): GoogleTokens {
        val bridge = PulseIos.googleSignIn ?: throw AuthException(AuthErrorKind.NOT_CONFIGURED)
        return suspendCancellableCoroutine { cont ->
            bridge.signIn(
                onSuccess = { id, access -> if (cont.isActive) cont.resume(GoogleTokens(id, access)) },
                onCancel = { if (cont.isActive) cont.resumeWithException(AuthException(AuthErrorKind.CANCELLED)) },
                onError = { if (cont.isActive) cont.resumeWithException(AuthException(AuthErrorKind.UNKNOWN, Exception(it))) },
            )
        }
    }
}

actual fun createGoogleTokenProvider(): GoogleTokenProvider = BridgedGoogle
