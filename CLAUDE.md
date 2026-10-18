# Pulse (Fame Life) — instructions for coding agents

Pulse is a private, AI-powered social fame simulator: users browse a simulated feed, go live with their own
camera, talk, and a **simulated** audience reacts. Nothing is broadcast. Read this file before changing code.

## Stack and layout

- Kotlin Multiplatform + Compose Multiplatform. `shared` holds all UI and logic; `androidApp` and `iosApp` only host it.
- One shared module, organised by feature under `shared/src/commonMain/kotlin/org/mayad/fame_life/`:
  - `app/` — `AppContainer` (manual DI), typed `Navigator` + `Route`, `AppConfig` brand strings.
  - `core/designsystem/` — tokens (`PulseTheme`, `PulseTokens`, `PulseTypography`), glyphs, 3D `GlossyIcon`, `GiftArt`, procedural `Artwork`, shared components.
  - `core/platform/` — `expect` boundaries: storage, share, reduce motion, speech, camera, permissions.
  - `domain/` — pure Kotlin: `simulation/` (engine, controller, event policy), `ai/` (transcript, protocol, validator, generators), `account/` (`AuthService`, `AuthRules`), `entitlement/`, `progression/`, `profile/`.
  - `data/` — fictional seed content (`content/`), the device-cache repositories (`local/`), and Firebase (`cloud/`: `FirebaseAuthService`, `FirestoreCloudStore`, `AccountSync`).
  - `feature/` — screens: shell, home, discover, post, profile, onboarding (splash, intro, account, profile setup), notifications, settings, live.
- Platform code lives in `androidMain` / `iosMain` (`core/platform/*.android.kt`, `*.ios.kt`).
- No DI or navigation library on purpose. Add one only when the current approach demonstrably hurts.

## Skills

Consult these when they apply:
- `live-fame-mobile-architecture-skill` — boundaries, simulation vs AI, backend, entitlements, lifecycle, testing.
- `live-fame-creative-ui-skill` and `creative-mobile-ui-engineering-skill` — visual language, motion, live-screen rules.

## Non-negotiable rules

- **Never put provider secrets or API keys in the app.** LLM calls go through a trusted backend (see `docs/ARCHITECTURE.md`). `AppContainer.audienceGenerator` stays null until that backend exists.
- **Never imply simulated viewers, followers, comments or gifts are real.** Keep `SimulatedTag` / disclosure copy wherever numbers could be mistaken for real reach, including shared text.
- **The AI proposes, the engine decides.** Viewer counts, gift timing, cooldowns and totals come from `LiveSessionEngine` / `EventPolicy`. AI output always passes `ResponseValidator` first.
- **Never trust client-side plan flags, counters or viewer numbers** for anything that costs money. Only a server-verified entitlement (`EntitlementSource.VERIFIED`) may unlock paid AI limits, and the server must re-check.
- **The client never writes entitlements or usage counters.** `firestore.rules` makes `entitlements/` and `usage/` server-write-only; keep it that way and keep `users/{uid}` owner-only.
- Screens never call Firebase directly. Go through `AuthService`, the repositories and `AccountSync`; tests use `FakeAuthService` / `FakeCloudStore`.
- Do not upload camera frames. Do not store raw audio. Speech text is the only thing that may leave the device, and only to our backend.
- Ask for camera/mic only from the live setup screen, and keep the app usable when they are denied.
- No dead buttons. A control either works, or is visibly disabled with an explanation.
- No invented prices, fake urgency, fabricated analytics, testimonials or test results.
- Analytics events (`AnalyticsEvent`) never carry transcripts, comment text or names.
- Respect `LocalReduceMotion`: every decorative animation needs a calm fallback.

## Design tokens

Dark-first. Colors come from `Pulse.colors` (background `#080B14`, surfaces `#101526/#151D32/#1D2843`, blue `#4C7DFF`,
violet `#945CFF`, cyan `#55E5F2`, text `#F7F8FC/#A5AEC4/#717C96`, success `#52D6A0`, warning `#FFC66D`, error `#FF647C`).
Spacing `Space`, shapes `Shapes`, motion `Motion`. Never hard-code a new color or duration in a screen when a token fits.
Icons on feature surfaces are glossy 3D tiles (`GlossyIcon`), not flat glyphs; small inline icons use `PulseIcon`.

## Build and test

- Android: `./gradlew :androidApp:assembleDebug`
- Shared tests on Android host: `./gradlew :shared:testAndroidHostTest`
- iOS framework: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`; iOS app: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro' CODE_SIGNING_ALLOWED=NO build`.
  If `xcode-select` points at the Command Line Tools, prefix with `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`.
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test` currently fails to link (`framework 'FirebaseCore' not found`):
  GitLive needs the Firebase iOS frameworks on the test binary's search path, which only Xcode's SPM build has today.
  The same common tests run on the Android host.
- JVM target is 17 (GitLive is built for 17).
- Don't name Kotlin constants after C macros (`NAME_MAX`, `PATH_MAX`, …): they break the exported Objective-C header.
- Report exactly which of these you ran and what happened. Never claim a build or test passed without running it.

## Status of integrations

- Firebase project `live-fame`. Auth is **Google sign-in only** (no email/password). Android: Credential Manager +
  `googleid`, using `default_web_client_id` from `google-services.json`; the debug keystore SHA-1 is registered.
  iOS: GoogleSignIn (SPM) through `iosApp/iosApp/GoogleSignInBridge.swift` → `PulseIos.googleSignIn`; URL scheme =
  `REVERSED_CLIENT_ID`. iOS bundle id is `org.mayad.famelife` (no underscores allowed on iOS).
  A release keystore needs its own SHA-1 added (`firebase apps:android:sha:create`) before Google sign-in works there.
  Analytics, Crashlytics, App Check, Apple sign-in: not yet.
- Firestore rules: `firestore.rules`; deployed. Redeploy after changes with `firebase deploy --only firestore --project live-fame`.
- Audience backend (LLM): protocol defined (`AudienceProtocol.kt`), server not built. The on-device generator runs instead.
- Billing: not wired. Everyone is on Free; the paywall says so.
- iOS: camera preview and speech recognition are not implemented yet (honest fallbacks are shown).
