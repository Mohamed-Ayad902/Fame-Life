package org.mayad.fame_life.core.platform

import org.mayad.fame_life.data.remote.PulseBackend
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

/** The trusted backend, or null when the build has no backend URL or the platform has no client yet. */
expect fun createPulseBackend(): PulseBackend?

/**
 * Speech recognition: through the backend's Whisper route when there is a [backend] (it detects the language),
 * otherwise the platform recognizer. Null where neither is implemented yet.
 */
expect fun createSpeechInput(backend: PulseBackend?): SpeechInput?

private val processStart = TimeSource.Monotonic.markNow()

/** Monotonic milliseconds for simulation timing; unaffected by wall-clock changes. */
fun monotonicMillis(): Long = processStart.elapsedNow().inWholeMilliseconds
