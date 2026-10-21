# Pulse (Fame Life): handover

This document is for whoever runs or continues the project next. It explains, in plain words, what exists, where it
lives, what it costs, how to change things, and what is left to do. Read [`README.md`](README.md) first for what the
app is and how the code is laid out.

Status as of 21 October 2026.

---

## 1. One-minute summary

- **What it is:** a phone app (Android + iOS from one Kotlin codebase) where you "go live" to a pretend audience that
  reacts to what you say. Nothing is real or broadcast, and the app says so.
- **Three moving parts:** the app (`shared/`, `androidApp/`, `iosApp/`), our small server (`backend/`, on AWS EC2),
  and Firebase (Google's login + database, project `live-fame`).
- **Works today on Android:** Google sign-in, profile, feed, live sessions with real speech-to-text and AI comments
  (through the server), summaries, badges, levels, inbox, settings, account deletion, the new game-style Home.
- **Built but not switched on yet:** the points economy (daily streak, paying points to go live, reward ads). The
  code is done in the app and the server, but the server on AWS hasn't been updated and still needs a Firebase
  service account (section 5).
- **Not built:** billing / Pro purchases, Apple sign-in, iOS camera and speech, points on iOS.

---

## 2. Accounts and services you need access to

| Service | What it's used for | Where |
| --- | --- | --- |
| GitHub repo `Mohamed-Ayad902/Fame-Life` | the code | github.com |
| Firebase project **live-fame** | sign-in, Firestore database, security rules | console.firebase.google.com |
| AWS account (EC2) | runs our backend server | AWS console → EC2 |
| Groq | speech to text (Whisper) | console.groq.com (API key on the server only) |
| Google AI Studio (Gemini) | writes the audience comments | aistudio.google.com (API key on the server only) |
| AdMob (not created yet) | reward ads that give points | admob.google.com |
| Google Play Console / App Store Connect (later) | publishing | – |

**Secrets** (API keys, the Firebase service account, the EC2 `.pem` SSH key) are never in the repo. On the server
they live in `/etc/pulse/env` and `/etc/pulse/service-account.json`, readable only by the service. Keep the `.pem`
file safe; it is the only way to log in to the server.

---

## 3. Where the data is (and who can change it)

All saved data is in **Firestore** (Firebase console → Firestore Database). Each user is identified by a `uid`
(their Firebase account id).

| Path in Firestore | Holds | Written by |
| --- | --- | --- |
| `users/{uid}` | profile: name, username, avatar, interests, fame points, lives count | the app |
| `users/{uid}/sessions/{id}` | one recap per finished live | the app |
| `users/{uid}/notifications/{id}` | inbox items | the app |
| `wallets/{uid}` | points balance, streak day, last claim | **server only** |
| `wallets/{uid}/ledger/{id}` | every grant or spend (streak, live, ad), so nothing is charged twice | server only |
| `config/economy` | admin prices and rewards (section 4) | you, in the console |
| `entitlements/{uid}`, `usage/{uid}` | future paid plan and AI counters | server only |

The rules that enforce "who writes what" are in `firestore.rules`. After editing them, deploy:

```bash
firebase deploy --only firestore --project live-fame
```

**Not stored anywhere:** voice recordings (each sentence is sent to the server, forwarded to Groq for text, then
dropped), camera video (never leaves the phone), live comments while live (only the final recap is saved).

**On the phone:** a cached copy of the profile, lives and inbox, so the app opens offline. It is wiped on sign-out.

---

## 4. Changing points prices and rewards (no code needed)

Create or edit the document **`config/economy`** in the Firebase console (Firestore → Start collection `config` →
document id `economy`). Every field is optional; a missing or wrong value falls back to the default below. Changes
reach phones immediately and the server within a minute.

| Field | Type | Default | Meaning |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | `false` makes lives free and hides points |
| `startingBalance` | number | `60` | points a new user starts with |
| `streakRewards` | array of numbers | `[20, 25, 30, 40, 50, 60, 100]` | points for day 1, day 2, …; after the last day it starts over; missing a day resets to day 1 |
| `defaultLiveCost` | number | `30` | price of a live when its style has no own price |
| `liveCosts` | map | `{aurora: 30, noir: 50}` | price per live style id |
| `adReward` | number | `25` | points for one watched reward ad |
| `adsPerDay` | number | `5` | max rewarded ads per user per day |
| `adUnits` | array of strings | `[]` | allowed AdMob unit ids (empty = any of this app's units) |

The same defaults are written in two places that must stay equal: `backend/src/economy.mjs` (`DEFAULT_CONFIG`) and
`shared/.../domain/economy/Economy.kt` (`EconomyConfig.Default`).

---

## 5. The server (backend)

### What it does

| Route | Called by | Does |
| --- | --- | --- |
| `POST /v1/transcribe` | app, while live | sends one spoken sentence to Groq, returns the text |
| `POST /v1/audience` | app, while live | asks Gemini for comments, returns them |
| `POST /v1/points/checkin` | app, daily reward | gives today's streak points once per day |
| `POST /v1/points/spend` | app, before a live | charges the live's price once per session |
| `GET /v1/ads/admob-ssv` | AdMob's servers | credits points after a verified ad view |
| `GET /healthz` | anyone | "I'm alive" |

Every `/v1` call must carry the user's Firebase login token; the server checks it before doing anything, and applies
per-user limits so nobody can burn through the free AI quotas.

### Current state (checked 21 Oct 2026)

- The server at the address in `local.properties` (`pulse.backendUrl`) is **up** (`/healthz` answers 200).
- It runs an **older version without the points routes** (`/v1/points/checkin` answers 404). Until it's updated the
  app shows no points and lives are free.

### Update the server and turn on points

1. Firebase console → Project settings → Service accounts → **Generate new private key**. Save the JSON file.
2. From the repo root on your computer (replace the IP and key file name):
   ```bash
   scp -i <your-key>.pem -r backend ec2-user@<server-ip>:~/
   scp -i <your-key>.pem <service-account>.json ec2-user@<server-ip>:~/
   ssh -t -i <your-key>.pem ec2-user@<server-ip> 'sudo bash backend/deploy/setup.sh --service-account ~/<service-account>.json'
   ```
   The script installs/updates the server, keeps the existing Groq/Gemini keys, stores the service account safely
   and deletes your uploaded copy.
3. Deploy the Firestore rules (section 3) so phones can read their wallet.
4. Check: `curl -X POST https://<backend>/v1/points/checkin` should answer **401** (needs login), not 404.

### Everyday server tasks

| Task | Command (on the server) |
| --- | --- |
| See live logs | `sudo journalctl -u pulse -f` |
| Restart | `sudo systemctl restart pulse` |
| Replace AI keys | `sudo bash backend/deploy/setup.sh --keys` |
| Change AI models | edit `GROQ_MODEL` / `GEMINI_MODEL` in `/etc/pulse/env`, then restart |

Logs never contain what users said or comment text, only the route, status, timing and a scrambled user id.

---

## 6. Reward ads (AdMob), when you're ready

1. Create the app and a **Rewarded** ad unit in AdMob.
2. In the ad unit, turn on **server-side verification** with callback URL `https://<backend>/v1/ads/admob-ssv`.
3. Put the ids in `local.properties`: `pulse.admobAppId=ca-app-pub-…~…` and `pulse.admobRewardedUnitId=ca-app-pub-…/…`.
4. Optionally list the unit id in `config/economy.adUnits`.

Watching an ad on the phone gives nothing by itself; only AdMob's signed callback to our server adds the points.
Until step 3, the "watch an ad" offer is hidden and Google's test app id is used.

---

## 7. Money and limits

- **Groq and Gemini:** free tiers with per-minute and per-day limits that the providers can change. When a limit is
  hit the server answers "busy" and the app falls back to its simpler on-device comments. Gemini's free tier lets
  Google use requests to improve its products: switch to a paid key before real users.
- **AWS:** the EC2 instance and its public IP are billed unless covered by the Free Tier.
- **Firebase:** Spark/Blaze pricing; Firestore reads and writes are small (no per-second live data is saved).
- **No real money flows through the app yet.** Billing is not wired; everyone is on Free.

---

## 8. Building and releasing

- Android debug: `./gradlew :androidApp:assembleDebug`, install with `./gradlew :androidApp:installDebug`.
- iOS: open `iosApp/iosApp.xcodeproj` in Xcode and Run.
- Tests: `./gradlew :shared:testAndroidHostTest` (app logic, 112 tests) and `cd backend && npm test` (server).
- **Before a Play Store release:** create a release keystore and register its SHA-1 in Firebase
  (`firebase apps:android:sha:create <appId> <sha1> --project live-fame`), or Google sign-in won't work in the
  release build. Use real AdMob ids. Switch Gemini to a paid key.
- **Before an App Store release:** add Apple sign-in (Apple requires it when Google sign-in exists), finish iOS camera
  and speech, add an iOS client for the backend.

---

## 9. What's next (suggested order)

1. Update the server and switch on points (section 5).
2. Create AdMob and turn on reward ads (section 6).
3. Billing: Play Billing / StoreKit, verified by the server, written to `entitlements/{uid}`.
4. iOS: backend client (speech, audience, points), camera preview, speech recognition.
5. Apple sign-in, App Check, Crashlytics, Analytics.
6. Move the server's per-user limits from memory to Firestore (`usage/{uid}`), so they survive restarts.

---

## 10. Rules that must not be broken

These protect users and the business. The full list is in `CLAUDE.md`.

- Never put API keys or secrets in the app.
- Never make simulated viewers, comments, gifts or followers look real. Keep the "Simulated" labels.
- The app never writes points, plans or usage counters. Only the server does.
- Never upload camera frames or store voice recordings.
- Ask for camera and microphone only on the live setup screen; the app must still work if the user says no.
- No buttons that do nothing, no fake prices, fake urgency or fake reviews.

---

## 11. Where to look when something breaks

| Symptom | Look at |
| --- | --- |
| Google sign-in fails on a new build | SHA-1 of that signing key isn't in Firebase (section 8) |
| No AI comments, only simple ones | server down or AI quota hit: `journalctl -u pulse -f` |
| No points shown | server not updated, no service account, `config/economy.enabled = false`, or iOS |
| "Not enough points" never appears | same as above: without the economy, lives are free |
| App can't read/write Firestore | `firestore.rules` not deployed, or user not signed in |
| Build fails after pulling | run `./gradlew --stop`, then build again; check JDK 17 |
