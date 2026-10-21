# Pulse (Fame Life)

Pulse is a phone app that lets anyone feel what it's like to be a popular creator. You scroll a social-style feed,
tap the glowing button to "go live" with your own camera, and talk. A **pretend audience** listens to what you say
and reacts: comments, questions, gifts, viewers joining. When you end the live you get a recap and earn fame points.

Nothing is broadcast. The viewers aren't real, the camera never leaves your phone, and the app says "Simulated"
wherever a number could be mistaken for real reach. Keep it that way.

New here? Read this file top to bottom, then [`HANDOVER.md`](HANDOVER.md) (accounts, servers, money, what to do
next). The rules every contributor and coding agent follows are in [`CLAUDE.md`](CLAUDE.md); deeper design notes are
in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and [`backend/README.md`](backend/README.md).

---

## 1. The app in plain words

### What the user sees

| Screen | What it's for |
| --- | --- |
| Splash, welcome, intro | Explains the idea and that everything is simulated |
| Sign in | "Continue with Google" (the only sign-in option today) |
| Profile setup | Pick a display name, username, avatar look and interests |
| **Home** | A game-style dashboard: your level, points, badges, a featured live, live rooms, trending topics, a feed |
| Discover | Browse creators, rooms and posts by category |
| Post / creator profile | Fake creators' posts and comments (all made-up content shipped with the app) |
| Inbox | In-app notifications (level ups, finished lives) |
| Your profile | Your lives, best peak, badges, fame level |
| **Live setup** | Choose a title, audience size and mood, look and feel; asks for camera and microphone here only |
| **Live session** | Your camera, a moving comment stream, viewer count, gifts, reactions |
| Live summary | Peak viewers, comments, gifts, points earned, level progress |
| Daily reward | A day 1, day 2, day 3… calendar; come back daily to collect points |
| Settings, paywall | Account, privacy, delete account; the Pro plan (not for sale yet) |

### The two kinds of "points"

- **Fame points** grow your level (New Creator → Rising → … → Superstar). You earn them by finishing lives. They
  are just for fun and can't be spent.
- **Wallet points** (the gold coin in the Home header) are spent to go live. You get them from the daily streak and,
  later, by watching a reward ad. Admins decide the prices. They are never real money.

### The three parts of the system

```
 ┌──────────────────────┐       ┌───────────────────────────┐       ┌─────────────────────────┐
 │  The phone app        │       │  Our backend server        │       │  Firebase (Google cloud) │
 │  Android + iOS        │──────▶│  backend/ (Node, on AWS)   │──────▶│  Login + database        │
 │  shared/ (Kotlin)     │       │  - turns speech into text  │       │  - who you are (Auth)    │
 │                       │       │  - writes audience comments│       │  - your profile, lives   │
 │                       │◀──────│  - gives / takes points    │       │  - your points wallet    │
 └──────────┬───────────┘       └─────────────┬─────────────┘       └─────────────────────────┘
            │  reads your own data directly    │ calls AI providers with secret keys
            └─────────────────────────────────▶│  Groq (speech to text), Gemini (comments)
```

**Why is there a server at all if we have Firebase?** Two jobs can't be done safely inside the app:

1. **Secret keys.** Talking to the AI services (Groq, Gemini) needs a paid key. Anything inside an app can be pulled
   out by anyone, so the key lives only on the server.
2. **Points.** If the phone could change its own balance, anyone could give themselves a million points or pretend
   they watched an ad. So the server is the only one allowed to add or remove points. Firestore's security rules
   block the app from writing them.

The server is small (plain Node.js, no extra packages) and stores nothing itself: all saved data goes to Firebase.

### Where every piece of data lives

| Data | Stored in | Who can write it |
| --- | --- | --- |
| Your account (Google login) | Firebase Authentication | Firebase, when you sign in |
| Profile (name, avatar, fame points, lives count) | Firestore `users/{uid}` | the app (only your own) |
| Past lives (one recap per live) | Firestore `users/{uid}/sessions/` | the app (only your own) |
| Inbox notifications | Firestore `users/{uid}/notifications/` | the app (only your own) |
| **Points balance and streak** | Firestore `wallets/{uid}` | **the server only** |
| Every points change (history) | Firestore `wallets/{uid}/ledger/` | the server only |
| Prices and rewards (admin settings) | Firestore `config/economy` | admins, in the Firebase console |
| Paid plan (future) | Firestore `entitlements/{uid}` | the server only |
| A copy of profile, lives and inbox for offline use | on the phone (`TextStore`) | the app |
| Your voice | **nowhere**: sent sentence by sentence to the server, passed to Groq, thrown away | – |
| Camera video | **nowhere**: it never leaves the phone | – |
| The fake feed (creators, posts, rooms) | built into the app (`data/content/SeedContent.kt`) | developers |

Open the Firebase console (project **live-fame**) → Firestore Database to see the real documents.

### What happens during a live, step by step

1. **Setup.** You pick options. If the points system is on, the server charges the live's price first.
2. **You talk.** The phone listens and cuts your speech into sentences.
3. **Speech to text.** Each sentence goes to our server, which asks Groq Whisper what you said (Arabic or English).
4. **The audience reacts.** The server sends the recent text to Gemini, which suggests a few comments and maybe a gift.
5. **Safety check.** The app checks every suggestion (length, bad words, tone) and drops anything wrong.
6. **The engine decides.** A rule engine on the phone decides when each comment appears, how many viewers there
   are, and when gifts land. The AI only *suggests*; the engine *decides*. That keeps the numbers believable.
7. **No internet?** If the server is slow or down, a built-in generator on the phone writes simple comments instead,
   so the room never goes quiet.
8. **End.** You get a summary, earn fame points, and the recap is saved to Firestore.

---

## 2. Run it

### What you need

- Android Studio (recent), with a phone in developer mode or an emulator "with Google Play".
- For iOS: a Mac with Xcode 16 or newer.
- JDK 17 or newer (Android Studio's bundled one is fine).

### Settings file: `local.properties`

This file sits in the project root, is not committed, and tells the app where things are:

| Key | What it does | If missing |
| --- | --- | --- |
| `sdk.dir` | Android SDK folder (Android Studio fills it in) | build fails |
| `pulse.backendUrl` | address of our server, e.g. `https://1-2-3-4.sslip.io` | no AI server: on-device comments, no points |
| `pulse.admobAppId` | AdMob app id for reward ads | Google's test id is used |
| `pulse.admobRewardedUnitId` | AdMob rewarded ad unit | "watch an ad" offer is hidden |

### Android

```bash
./gradlew :androidApp:assembleDebug          # build the APK
./gradlew :androidApp:installDebug           # install on the connected phone or emulator
```

Or open the folder in Android Studio and press Run on `androidApp`. Firebase is connected through
`androidApp/google-services.json`.

### iOS

1. Open `iosApp/iosApp.xcodeproj` in Xcode. The first open downloads the Firebase iOS SDK and GoogleSignIn; give it
   a minute.
2. Pick an iPhone simulator and press Run. Xcode runs Gradle for you to build the shared Kotlin code.

From the terminal:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' CODE_SIGNING_ALLOWED=NO build
```

If `xcodebuild` says it "requires Xcode", run `sudo xcode-select -s /Applications/Xcode.app` once, or prefix the
command with `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`.

### Tests

```bash
./gradlew :shared:testAndroidHostTest        # all app logic tests, on your computer
cd backend && npm test                       # server tests, no keys needed
```

---

## 3. How the code is organised

Almost all code lives in **`shared/`** and runs on both Android and iOS. `androidApp/` and `iosApp/` are thin shells
that start it.

```
androidApp/                 Android shell: MainActivity, manifest, google-services.json
iosApp/                     iOS shell: iOSApp.swift (starts Firebase), Google sign-in bridge, Xcode project
backend/                    Our server (Node.js): routes, AI calls, points, deploy script
docs/                       ARCHITECTURE.md (deep dive), licenses/
firestore.rules             Who may read/write what in Firestore (deploy with the Firebase CLI)
shared/src/
  commonMain/composeResources/  Fonts and all text (values/ = English, values-ar/ = Arabic)
  commonMain/kotlin/org/mayad/fame_life/
    App.kt                  Decides the first screen: splash → onboarding → profile setup → main
    app/navigation/         The list of screens (AppRoute) and how to move between them (AppNavHost)
    di/                     Koin "wiring": which real or fake class each part of the app gets
    core/designsystem/      The look: Pulse colors, sizes, fonts, motion, glossy icons, HUD pieces (Hud.kt)
    core/platform/          Things each phone does differently: storage, camera, mic, speech, ads
    core/ui/                Turns codes (levels, badges, errors) into translated text
    domain/                 The rules, pure Kotlin, no screens and no Firebase:
      simulation/           the live engine (viewers, comments, gifts, timing)
      ai/                   speech buffer, AI request limits, the safety check, on-device audience
      economy/              points: streak rules, prices, the PointsService interface
      progression/          fame levels and badges
      entitlement/          Free vs Pro limits
      account/ profile/     sign-in contract, profile validation
    data/
      content/              The made-up feed (creators, posts, rooms)
      local/                What screens read: profile, lives, inbox (the phone's copy)
      cloud/                Firebase sign-in and Firestore sync (AccountSync)
      economy/              Reads the wallet from Firestore, asks the server to claim/spend
      remote/               Talks to our backend (speech, audience)
    feature/                One folder per area of the app (home, live, profile, points, settings, …)
  androidMain/ iosMain/     The Android and iOS versions of core/platform
  commonTest/               Tests
```

### Words you'll see in the code

| Word | Means |
| --- | --- |
| **Kotlin Multiplatform / Compose Multiplatform** | Write the app once in Kotlin, run it on Android and iOS |
| **Screen / Content / ViewModel** | Each screen has 3 files: the ViewModel holds the data, `XScreen` connects it, `XContent` draws it |
| **State** | Everything a screen shows, in one object. Change the state and the screen redraws |
| **Repository** | A box that holds one kind of data (profile, lives) and tells screens when it changes |
| **Koin** | Hands each class the things it needs, so tests can swap in fakes |
| **Firestore** | Google's cloud database. Data is "documents" inside "collections", like folders and files |
| **Firestore rules** | The guard that decides who can read or write each document |
| **Backend / server** | Our own program on AWS that holds secret keys and owns points |
| **Firebase ID token** | Proof the app sends to the server that says "this really is user X" |
| **Pulse** | The design system: every color, size and font comes from `Pulse.*` |
| **Simulated** | Made-up by the app. Always labelled so nobody thinks it's real |

### How to add a screen

1. Make `feature/<name>/` with `XViewModel.kt` (state + actions), `XScreen.kt` (connects ViewModel and navigation)
   and `XContent.kt` (draws the state, plus previews at the end of the file).
2. Put all text in `composeResources/values/strings_<feature>.xml` **and** the Arabic in `values-ar/`.
3. Register the ViewModel in `di/ViewModelModule.kt` (`viewModelOf(::XViewModel)`).
4. Add a route in `app/navigation/AppRoute.kt` and an `entry<AppRoute.X> { … }` in `AppNavHost`.
5. Follow `CLAUDE.md`: no hard-coded sizes, colors or text; every button works or is visibly disabled; label
   simulated numbers; respect reduced motion.

---

## 4. What isn't built yet

- **Billing.** Everyone is on Free. Pro must come from a store purchase verified by the server.
- **Points on iOS.** iOS has no backend client yet, so no balance, no ads, and lives are free there.
- **iOS camera and speech.** The iOS live screen shows honest fallbacks.
- **Sign-in options.** Google only. Apple sign-in (needed for the App Store) is not done.
- **Firebase extras.** Analytics, Crashlytics, App Check, Remote Config.
- **iOS unit tests** fail to link today (`framework 'FirebaseCore' not found`); the same tests run on Android.

See [`HANDOVER.md`](HANDOVER.md) for the full status and next steps.
