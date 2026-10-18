# Pulse (Fame Life)

Pulse is a mobile app that lets anyone feel what it's like to be a popular creator. You browse a social-style feed,
tap the glowing button to go live with your own camera, and talk. A **simulated** audience listens to what you say
and reacts: comments, questions, gifts, viewer spikes. When you end the live you get a recap and earn fame points.

Nothing is broadcast. The viewers aren't real, the camera never leaves your phone, and the app says so wherever a
number could be mistaken for real reach. Keep it that way. The rules every contributor follows are in
[`CLAUDE.md`](CLAUDE.md); the deeper design notes are in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

- **Platforms:** Android and iOS from one Kotlin codebase (Kotlin Multiplatform + Compose Multiplatform).
- **Accounts and data:** Firebase Authentication with Google sign-in, and Cloud Firestore, project `live-fame`.
- **Working name:** "Pulse". Brand strings live in `AppConfig` (`shared/.../app/AppContainer.kt`).

---

## 1. Run it

### What you need

- Android Studio (recent), with an emulator or a phone in developer mode.
- For iOS: a Mac with Xcode 16 or newer.
- JDK 17 or newer (Android Studio's bundled one is fine).

### Android

```bash
./gradlew :androidApp:assembleDebug          # build the APK
./gradlew :androidApp:installDebug           # install on the running emulator/phone
```

Or open the folder in Android Studio and press Run on the `androidApp` configuration.
Firebase is already connected on Android through `androidApp/google-services.json`.

### iOS

1. `iosApp/iosApp/GoogleService-Info.plist` is already in the repo (iOS bundle id `org.mayad.famelife`;
   iOS bundle ids can't contain underscores, so it differs from the Android package `org.mayad.fame_life`).
2. Open `iosApp/iosApp.xcodeproj` in Xcode. The first open downloads the Firebase iOS SDK and GoogleSignIn (Swift
   Package Manager); give it a minute.
3. Pick an iPhone simulator and press Run. Xcode runs Gradle for you to build the shared Kotlin code.

From the terminal instead:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' CODE_SIGNING_ALLOWED=NO build
```

If `xcodebuild` says it "requires Xcode", your Mac points at the Command Line Tools. Either run
`sudo xcode-select -s /Applications/Xcode.app` once, or prefix commands with
`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`.

### First launch

Splash → welcome → "How a live works" → "Before you start" (what's simulated) → Continue with Google → pick your
name and look → interests → home. On Android, Google sign-in needs a Google account on the device or emulator
(an emulator image "with Google Play"). Next time you open the app you go straight home.

---

## 2. Folder map

```
androidApp/                 Android host: MainActivity, manifest, splash theme, google-services.json
iosApp/                     iOS host: iOSApp.swift (configures Firebase), launch screen, Xcode project
shared/src/
  commonMain/composeResources/  Fonts and strings (values/ English, values-ar/ Arabic)
  commonMain/kotlin/org/mayad/fame_life/
    App.kt                  Root: splash → onboarding → profile setup → main, based on sign-in state
    app/                    AppContainer (creates everything), Navigator + Route (screen stack), AppConfig
    core/designsystem/      Pulse theme (colors, type, adaptive dimens, shapes, motion), cascade + scroll motion,
                            previews, glossy 3D icons, shared components
    core/ui/                Domain values to localized labels
    core/preview/           SampleData for previews
    core/platform/          "expect" functions each platform implements: storage, camera, mic, speech, share
    core/analytics/         Typed analytics events (never carry names, transcripts or comment text)
    domain/                 Pure Kotlin rules, no UI and no Firebase
      account/              AuthService interface, AuthRules (validation + error copy)
      simulation/           The live engine: viewers, comments, gifts, pacing, cooldowns, summary
      ai/                   Speech transcript buffer, AI request policy, response validator, on-device audience
      entitlement/          Free/Pro limits (Pro only from a server-verified source)
      progression/ profile/ Fame levels, profile validation
    data/
      content/              Fictional seed feed: creators, posts, lives (stable ids, no randomness per render)
      local/                Repositories screens read from (profile, sessions, inbox) = the device cache
      cloud/                FirebaseAuthService, FirestoreCloudStore, AccountSync (keeps cache and cloud in step)
    feature/                One folder per area: onboarding, home, discover, post, profile, live, notifications, settings, shell
  androidMain/ iosMain/     The platform implementations of core/platform
  commonTest/               Tests, shared by both platforms
docs/ARCHITECTURE.md        Data flow, backend boundary, Firestore layout, entitlements
firestore.rules             Who may read/write what in Firestore
firebase.json, .firebaserc  Firebase CLI config (rules deploy)
_removed_template/          Old template files, not built, not committed
```

---

## 3. How a live session flows through the code

```
LiveSetupScreen ──(navigator.push(Route.Live))──▶ LiveSessionScreen
                                                    │ creates
                                                    ▼
Microphone ─▶ SpeechInput (platform) ─▶ LiveSessionController ─▶ AudienceGenerator ─▶ ResponseValidator
                                          │  (every 200 ms tick)     (on-device today;      (rejects bad output)
                                          ▼                           backend later)
                                     LiveSessionEngine + EventPolicy  ◀── validated comment ideas
                                          │  decides viewers, gifts, timing, totals
                                          ▼
                                     LiveSessionState (StateFlow) ─▶ Compose UI (comments, viewer count, gifts)
                                          │
                                   end() │
                                          ▼
                                     SessionSummary ─▶ history.save / profiles.recordSession / inbox ─▶ AccountSync ─▶ Firestore
                                          │
                                          ▼
                                     LiveSummaryScreen (recap, level progress)
```

Where to look:

| Step | File |
| --- | --- |
| Setup (title, audience size, mood, camera/mic permission) | `feature/live/LiveSetupScreen.kt` |
| The live screen and saving the result | `feature/live/LiveSessionScreen.kt` (`recordSession`) |
| Coroutines, speech, AI requests, start/pause/end | `domain/simulation/LiveSessionController.kt` |
| Viewer counts, comment pacing, gifts, totals | `domain/simulation/LiveSessionEngine.kt`, `EventPolicy.kt`, `ViewerCountEngine.kt` |
| What the audience says | `domain/ai/LocalAudienceGenerator.kt` (on device), `AudienceProtocol.kt` (future backend) |
| Safety net on AI output | `domain/ai/ResponseValidator.kt` |

The golden rule: **the AI proposes, the engine decides.** The AI can suggest a comment or a gift; only the engine
decides if and when it shows, and what the totals are.

---

## 4. How Firebase is wired

- **Start-up:** Android initializes Firebase from `google-services.json` (`PulseAndroid.init` in
  `shared/src/androidMain/.../PlatformServices.android.kt`). iOS calls `FirebaseApp.configure()` in
  `iosApp/iosApp/iOSApp.swift` when `GoogleService-Info.plist` is bundled.
- **Choosing real or "not configured":** `AppContainer` checks `isFirebaseConfigured()`. If false it uses
  `UnavailableAuthService`, so the app runs and says plainly that sign-in isn't set up.
- **Accounts (Google only):** `domain/account/Auth.kt` defines `AuthService` (sign in with Google, sign out,
  re-confirm, delete) and `GoogleTokenProvider` (shows the platform account picker). `FirebaseAuthService` turns the
  Google token into a Firebase credential. The picker is `GoogleSignIn.android.kt` (Credential Manager) on Android
  and `GoogleSignInBridge.swift` (GoogleSignIn SDK) on iOS, handed to Kotlin through `PulseIos.googleSignIn`.
  Errors become `AuthErrorKind`, which `AuthRules.message` turns into friendly copy (closing the picker shows none).
- **Google sign-in keys:** Android needs each signing key's SHA-1 registered in Firebase (debug is done; add the
  release key with `firebase apps:android:sha:create <appId> <sha1> --project live-fame`, then re-download
  `google-services.json`). iOS needs the `REVERSED_CLIENT_ID` URL scheme in `Info.plist` (done).
- **Data:** screens never talk to Firestore. They read the repositories in `data/local/` (plain `StateFlow`s).
  `AccountSync` (`data/cloud/AccountSync.kt`):
  - on sign-in, loads `users/{uid}`, the newest sessions and the inbox into those repositories;
  - whenever a repository changes, writes the change to Firestore;
  - on sign-out (or a different account), wipes the device cache.
- **Firestore layout:** `users/{uid}` (profile), `users/{uid}/sessions/{id}`, `users/{uid}/notifications/{id}`.
  `entitlements/{uid}` and `usage/{uid}` are reserved for the server; the app never writes them.
- **Rules:** `firestore.rules`, already deployed. After editing, redeploy with
  `firebase deploy --only firestore --project live-fame`.
- **Library:** [GitLive firebase-kotlin-sdk](https://github.com/GitLiveApp/firebase-kotlin-sdk) 2.7.0 in
  `commonMain`. On iOS it needs the real Firebase iOS SDK linked, which the Xcode project does through Swift Package
  Manager (`firebase-ios-sdk` 11.x: FirebaseCore, FirebaseAuth, FirebaseFirestore).

---

## 5. How to add a screen

Every screen has three tiers, its strings in resources, and previews. Example `feature/badges/BadgesScreen.kt`:

```kotlin
// Tier 1: stateful. Reads the container, logs analytics, navigates.
@Composable
fun BadgesScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    val profile = container.profiles.profile.collectAsState().value ?: return
    BadgesContent(points = profile.famePoints, onBack = { navigator.pop() })
}

// Tier 2: stateless. Only parameters in, callbacks out, so it previews in any state.
@Composable
fun BadgesContent(points: Int, onBack: () -> Unit) {
    val cascade = rememberCascadeState()
    AtmosphereBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader(stringResource(Res.string.badges_title), onBack = onBack)
            // Tier 3: pick the layout by window width, not by device.
            if (Pulse.width.split) { /* side-by-side panes, e.g. SplitPanes(...) */ } else { /* one column */ }
        }
    }
}

@ScreenPreviews
@Composable
private fun BadgesPreview() = PreviewContainer { BadgesContent(points = SampleData.profile.famePoints, onBack = {}) }
```

Then:
1. Add the strings to `composeResources/values/strings_<feature>.xml` **and** the Arabic to `values-ar/`.
2. Add a route in `app/Navigation.kt` and map it in `PulseNavHost` (`App.kt`); open it with `navigator.push(...)`.

Rules (from `CLAUDE.md`): sizes from `Pulse.dimens`, colors from `Pulse.colors`, shapes from `Pulse.shapes`, no
literal dp/sp/colors/strings; glossy `GlossyIcon` tiles on feature surfaces; every button works or is visibly
disabled with a reason; respect `Pulse.reducedMotion`; label simulated numbers with `SimulatedTag`; enter lists with
`cascadeIn`; read scroll/animation values only in `graphicsLayer` or draw lambdas.

## 6. Tests

```bash
./gradlew :shared:testAndroidHostTest        # all shared tests, on your computer (fast)
```

Tests live in `shared/src/commonTest/`. They cover the simulation engine, AI pipeline, progression, local
repositories, auth validation and account sync. Firebase is never called in tests: `FakeAuthService` and
`FakeCloudStore` stand in for it. To test new logic, put it in `domain/` or `data/` (not in a composable) and use
these fakes.

`./gradlew :shared:iosSimulatorArm64Test` currently **fails to link** (`framework 'FirebaseCore' not found`) because
the iOS test binary needs the Firebase frameworks on its search path. The same tests run on the Android host above.

---

## 7. What isn't built yet

- **AI backend.** The audience runs on the device (`LocalAudienceGenerator`). The protocol for a server-side LLM is
  ready (`AudienceProtocol.kt`); the server isn't. Never put an API key in the app.
- **Billing.** Everyone is on Free. Pro must come from a server-verified purchase written to `entitlements/{uid}`.
- **iOS camera and speech.** The iOS live screen shows honest fallbacks; AVFoundation and SFSpeechRecognizer aren't
  wired.
- **Sign-in options.** Google only. Apple sign-in (required by App Store rules when other social logins exist)
  and unique usernames are not done.
- **Firebase extras.** Analytics forwarding, Crashlytics, App Check, Remote Config.
- **iOS unit tests** (see Tests).

---

## 8. Firebase setup status

- Google sign-in provider: enabled in the console.
- Firestore rules and indexes: deployed.
- Android app: debug SHA-1 registered, `google-services.json` includes the web client id.
- iOS app `org.mayad.famelife`: registered, `GoogleService-Info.plist` committed.
- Still to do before a store release: register the release keystore SHA-1; add Apple sign-in for iOS.
