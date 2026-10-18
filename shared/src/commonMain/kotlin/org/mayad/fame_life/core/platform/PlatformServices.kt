package org.mayad.fame_life.core.platform

import org.mayad.fame_life.domain.account.GoogleTokenProvider
import org.mayad.fame_life.domain.ai.SpeechInput
import kotlin.time.TimeSource

expect fun createTextStore(): TextStore

/** The platform's Google account picker (Credential Manager on Android, GoogleSignIn on iOS). */
expect fun createGoogleTokenProvider(): GoogleTokenProvider

/** True when the host app initialized Firebase (Android: google-services.json; iOS: GoogleService-Info.plist). */
expect fun isFirebaseConfigured(): Boolean

/** True when the user asked the OS to reduce motion; decorative animation must then be skipped. */
expect fun isReduceMotionEnabled(): Boolean

/** Opens the platform share sheet. Never posts anywhere by itself. */
expect fun shareText(text: String)

/** Platform speech recognition, or null where it is not implemented yet. */
expect fun createSpeechInput(): SpeechInput?

private val processStart = TimeSource.Monotonic.markNow()

/** Monotonic milliseconds for simulation timing; unaffected by wall-clock changes. */
fun monotonicMillis(): Long = processStart.elapsedNow().inWholeMilliseconds
