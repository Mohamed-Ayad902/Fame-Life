# Pulse architecture

## Live session data flow

```
Microphone → platform SpeechInput (Flow<SpeechEvent>) → TranscriptBuffer (dedupe, partial/final)
  → AudienceContext (last 5 utterances, last 8 comments, topic) → AiRequestPolicy (rate, budget, circuit breaker)
  → AudienceGenerator (remote backend, or LocalAudienceGenerator on device)
  → ResponseValidator (schema, length, blocklist, tone rules)
  → LiveSessionEngine + EventPolicy (pacing, viewers, gifts, cooldowns, totals)
  → LiveSnapshot (immutable) → Compose
```

- `LiveSessionController` owns the coroutines: a 200 ms tick loop, the speech collection and at most one generation
  request. `end()` cancels everything and is idempotent; responses that arrive after the end are dropped.
- If the remote generator fails, times out (6 s) or its circuit is open, the on-device generator answers, so the room
  never goes silent. The live screen shows which one is running.
- The engine is deterministic for a given seed and clock, which is what the tests rely on.

## Trusted backend (`backend/`)

The app never holds an AI provider key. A small Node server (no npm dependencies) runs on EC2 behind Caddy (HTTPS
via Let's Encrypt on an `sslip.io` hostname); `backend/README.md` covers deploy, limits and costs.

```
Mic → CloudSpeechInput (AudioRecord, 16 kHz) → UtteranceDetector (cuts sentences by loudness) → WAV
  → POST /v1/transcribe → Groq Whisper (detects Arabic/English itself) → {text, language} → TranscriptBuffer …
AudienceContext → AudienceRequest JSON → POST /v1/audience → Gemini (JSON schema output) → AudienceResponseDto
  → ResponseValidator (again, on device) → engine
```

1. Every call carries the user's Firebase ID token; the server verifies it against Google's certificates for
   `live-fame`, then applies per-user per-minute and per-day limits (in memory) before calling a provider.
2. Audio is held in memory until uploaded and is never stored; logs hold the route, status and a hashed uid only.
3. The app is wired in `di/KoinModules.kt`: when `pulse.backendUrl` is set in `local.properties` (read into
   `BuildConfig.PULSE_BACKEND_URL`) and Firebase is configured, `LiveServices` gets `CloudSpeechInput` and a
   `RemoteAudienceGenerator`. Without it the app falls back to the platform recognizer and the on-device audience.
4. If the backend fails or times out (6 s), the on-device generator answers for that turn, so chat never stalls.
5. Still planned: App Check, server-side entitlements and usage counters in Firestore (limits are in memory today),
   and an iOS client (URLSession + AVAudioEngine).

## Accounts and cloud data (Firebase)

Configured project: `live-fame`. Library: GitLive `firebase-kotlin-sdk` (`dev.gitlive:firebase-auth`,
`firebase-firestore`) in `commonMain`; on iOS the Firebase iOS SDK (11.x) is linked through Swift Package Manager in
`iosApp.xcodeproj`, as GitLive requires.

```
AuthService (domain/account)  ← FirebaseAuthService (data/cloud, Google only), UnavailableAuthService when Firebase isn't configured
GoogleTokenProvider           ← Credential Manager (Android) / GoogleSignIn via Swift bridge (iOS)
CloudStore (data/cloud)       ← FirestoreCloudStore
AccountSync (data/cloud)      — pulls on sign-in, mirrors local changes up, wipes the cache on sign-out
Profile/SessionHistory/NotificationRepository (data/local) — the device cache screens read (StateFlow)
```

- The repositories are the device cache. Every user change goes through `CloudSink` to `AccountSync`, which writes to
  Firestore in the background (Firestore queues offline writes itself). Loads from the cloud use `replace(...)`, which
  never echoes back.
- The cache is tagged with its owner uid (`account.owner.v1`). A different account signing in, or a sign-out, clears it.
- `App.kt` gates the root on `AuthState` + `SyncState`: splash → welcome/onboarding (signed out) → profile setup (signed
  in, no profile) → main. Offline with a cached profile still opens the app; offline with nothing cached shows a retry.
- Account deletion (Settings): re-confirm the same Google account → delete Firestore docs → delete the Firebase user →
  clear the device. Nothing local is removed if the cloud step fails.

| Path | Contents | Who writes |
| --- | --- | --- |
| `users/{uid}` | `CreatorProfile` | owner |
| `users/{uid}/sessions/{id}` | `SessionSummary`, one per finished live, trimmed to the plan's history size | owner |
| `users/{uid}/notifications/{id}` | `InAppNotification`, newest 100 | owner |
| `entitlements/{uid}` | verified plan | backend only |
| `usage/{uid}/…` | AI and session counters | backend only |

Rules live in `firestore.rules` (owner-only, field validation, server-only entitlements and usage); deploy with
`firebase deploy --only firestore --project live-fame`. No per-tick live data and no transcripts are written.

Rules are deployed. Still planned: Functions/Run (audience generation, purchase verification, entitlement and quota
writes), App Check, Analytics forwarding (`AnalyticsEvent` names already follow Firebase rules), Crashlytics, Apple
sign-in.

## Entitlements

`Entitlements(plan, source)` maps to `PlanLimits`. Today the source is always `DEFAULT_FREE`. Paid limits will require
`VERIFIED` (from the server after store purchase verification). Local counters, such as lives per day on the setup
screen, are a convenience only; the server is the authority.

## Local data

`TextStore` (SharedPreferences on Android, NSUserDefaults on iOS) holds versioned JSON for the profile, session
history and inbox: the offline cache of the signed-in account. Unreadable data is discarded and reported instead of
crashing. Sign-out and account deletion clear it.

## Splash

Android 12+ system splash (`core-splashscreen`, `Theme.Pulse.Starting`, `ic_splash_mark.xml`) and the iOS launch
screen (`UILaunchScreen` → `LaunchBackground` color + `LaunchMark` image) show the mark on `#080B14`. The in-app
`BrandSplash` then stays up for at least `BRAND_SPLASH_MIN_MS` and until auth and the account's data are known.

## Imagery

No photography is bundled. Feed, profile and live backdrops are procedural `Artwork` (8 palettes × 8 motifs), so every
creator is visibly fictional and nothing needs licensing. Fonts: Sora and Manrope (SIL OFL 1.1, see `docs/licenses`).
