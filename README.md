# SunoX

Real-time sound amplification for Android. The app captures microphone audio, runs low-latency wide-dynamic-range processing in native code (C++ with **Oboe**), and plays enhanced output to headphones or a headset.

**Use headphones** whenever the amplifier is active on a **physical device** to reduce acoustic feedback. This app is a consumer audio tool, not a certified hearing aid or medical device.

| | |
|---|---|
| **Package** | `com.soundamplifier` |
| **Version** | 1.0.3 (`versionCode` 4) |
| **Min SDK** | 26 (Android 8.0) |
| **Target / compile SDK** | 34 |
| **Language** | Kotlin 1.9 + C++17 (NDK 25.1) |

---

## Table of contents

- [Features](#features)
- [Appearance & theme](#appearance--theme)
- [What it does not include](#what-it-does-not-include)
- [Requirements & permissions](#requirements--permissions)
- [Prerequisites for development](#prerequisites-for-development)
- [Firebase & configuration](#firebase--configuration)
- [Build, install, and Play Store](#build-install-and-play-store)
- [Running on emulator](#running-on-emulator)
- [User flow & navigation](#user-flow--navigation)
- [Architecture](#architecture)
- [Native audio pipeline](#native-audio-pipeline)
- [Local database (Room)](#local-database-room)
- [Data & cloud sync](#data--cloud-sync)
- [Localization](#localization)
- [Repository layout](#repository-layout)
- [Tech stack](#tech-stack)
- [Troubleshooting](#troubleshooting)
- [Future ideas](#future-ideas)

---

## Features

### Core audio

- **Live amplification** — Oboe-based capture and playback with sub–20 ms round-trip as a design target on capable hardware.
- **Presets** — Conversation, Music, Outdoors, Classroom, TV/Media (boost quiet sounds, master gain, low/high shelf); switching presets updates the processing path. Preset chips use a fixed width and single-line labels to avoid awkward wrapping on long names.
- **Custom presets** — Save the current slider/preset state under a name; list, apply (tap) and delete (long-press menu) from the amplifier screen; stored in Room and synced to Firestore when logged in.
- **Controls** — Boost quiet sounds, output level (1×–5×), low and high frequency boost. Moving sliders away from a built-in preset enters a **Custom** state.
- **Input level** — Meter when idle or while the session runs.
- **Waveform visualizer** — Waveform and spectrum while a session is active.
- **Microphone routing** — **Phone mic by default** when available; labels refresh when audio devices change (no in-app mic source picker).

### Accounts & data

- **Authentication** — **Google Sign-In** via **Firebase Auth**. Sign-in is **required** to leave the welcome experience; there is no guest / local-only shortcut.
- **Single active session (optional, server-enforced)** — The client updates a Firestore **`sessionToken`** on the user document so only one “active” client is recognized. For **hard** single-device behavior (other devices cannot refresh Firebase credentials), deploy the optional **Cloud Function** in **`functions/`**: it runs when **`sessionToken`** changes and calls **`admin.auth().revokeRefreshTokens(uid)`**. See **`functions/README.md`**. App UX still signs out stale clients via Firestore listeners.
- **Cloud backup / merge** — After sign-in, **Firestore** syncs user metadata, **audiogram profiles**, and **custom amplifier presets** between devices (merge strategy favors pulling missing remote data and uploading locals not yet on the server).
- **Hearing test & profiles** — Six frequencies per ear (250 Hz–8 kHz); thresholds stored in **Room**; **NAL-R–inspired** gain prescription drives a **six-band** EQ in the native path.

### App chrome

- **Settings** — App language (radio list uses theme **`onSurface`** so labels stay readable in dark mode), retake hearing test, last test timestamp when a profile exists.
- **FAQ & profile** — In-app FAQ; profile screen with display name (signed-in), audiogram history, and sign-out.
- **Per-app language** — Chosen locale applies across Compose and resources via **AppCompat** and `locales_config.xml`.

---

## Appearance & theme

- **Default** — **Follow system** light/dark. The first launch (or a cleared `smarthear_prefs`) uses the device theme until the user overrides it.
- **Override** — Sun/moon control on **Welcome**, **Home**, and **Amplifier** sets an **explicit** light or dark preference (persisted: `follow_system_theme`, `dark_mode`) and updates **`AppCompatDelegate`** so the activity theme stays aligned (`Theme.AppCompat.DayNight.NoActionBar`).
- **Compose** — `SunoXTheme` wraps the entire `NavHost` with a **lavender–violet** Material 3 **lightColorScheme** / **darkColorScheme** (e.g. primary `#B8A2F5`, dark surfaces `#0A0A0F` with light **`onSurface`** text). Screens use `MaterialTheme.colorScheme` for backgrounds, glass-style cards, and controls so they track the same mode as the rest of the app.

---

## What it does not include

- Live captions or speech-to-text
- User-selectable Bluetooth vs phone microphone in the UI
- A dedicated “noise reduction” slider (processing is EQ + compression / expansion–style dynamics as implemented in native code)
- Medical regulatory claims or clinical calibration
- Offline use of main app flows without Firebase sign-in (welcome only until you authenticate)

---

## Requirements & permissions

**Runtime**

- Android **8.0+** (API 26+)
- **Headphones** strongly recommended when the mic path is live

**Permissions (manifest)**

| Permission | Purpose |
|------------|---------|
| `RECORD_AUDIO` | Capture and process microphone input |
| `MODIFY_AUDIO_SETTINGS` | Audio routing / session behavior |
| `BLUETOOTH_CONNECT` | Android 12+: enumerate some Bluetooth audio devices |

Google Sign-In needs correct **SHA fingerprints** and OAuth client configuration in the **Firebase Console** for release builds.

---

## Prerequisites for development

- **JDK 17** (matches `compileOptions` / `kotlinOptions`)
- **Android SDK** with API **34** build tools
- **Android NDK** **25.1.8937393** (pinned in `app/build.gradle`)
- **CMake** **3.22.1** (Android Gradle Plugin drives `app/src/main/cpp`)

Recommended: **Android Studio** with a matching AGP line (project uses **AGP 8.2.0**, **Kotlin 1.9.22**) so SDK, NDK, and CMake stay consistent.

---

## Firebase & configuration

The app uses the **Google Services** Gradle plugin and expects **`app/google-services.json`** from your Firebase project (often gitignored; ensure your working tree has a valid file).

1. Create a Firebase project and add an Android app with package **`com.soundamplifier`**.
2. Enable **Authentication** → **Google** (the app does not use Firebase Phone / SMS sign-in).
3. Download **`google-services.json`** into **`app/`**.
4. For **Google Sign-In**, set the **Web client ID** in `res/values/strings.xml` as `default_web_client_id`. A placeholder or wrong value surfaces a “not configured” **Toast** instead of launching sign-in.

Firestore **security rules** and any **indexes** must match what **`FirestoreUserRepository`** reads and writes (user profile, **`sessionToken`** / session fields, audiograms, custom presets). Lock rules down before production.

### Cloud Functions (optional)

The repo includes **`functions/`** (TypeScript) wired from **`firebase.json`**. The exported trigger **`revokeRefreshTokensOnSessionChange`** watches **`users/{uid}`** and, when **`sessionToken`** changes, calls **`revokeRefreshTokens`** so other devices lose refresh capability. Requires the **Blaze** plan and **`firebase deploy --only functions`**. Details: **`functions/README.md`**.

---

## Build, install, and Play Store

### Debug APK

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Install on a connected device or booted emulator:

```bash
./gradlew :app:installDebug
```

### Play Store (Android App Bundle)

Google Play expects an **`.aab`**, not a raw APK, for normal uploads:

```bash
./gradlew :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

**Signing** — Copy **`keystore.properties.example`** to **`keystore.properties`** in the **repo root** (do not commit it) and point **`storeFile`**, **`storePassword`**, **`keyAlias`**, and **`keyPassword`** at your **upload** keystore. Without that file, **`release`** falls back to the **debug** keystore — fine for local sanity checks, **not** for production Play uploads. Bump **`versionCode`** (and usually **`versionName`**) in **`app/build.gradle`** for each store release.

Optional release APK: `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk` (same signing rules as the bundle).

---

## Running on emulator

The UI and navigation work on an **AVD**, but the **virtual microphone often produces silence or weak input** — **validate real audio on a physical phone**.

**Graphics:** If you start the emulator from the command line, avoid **`-gpu swiftshader_indirect`** on a typical desktop (software rendering can look glitchy). Prefer the AVD default (**Host / Automatic**) or launch from Android Studio.

**Cold boot:** After changing GPU or snapshot state, use **`-no-snapshot-load`** once so Quick Boot does not restore a bad snapshot.

---

## User flow & navigation

High-level **Navigation Compose** graph (all inside one **`MaterialTheme`** / `SunoXTheme`):

| Route | Purpose |
|-------|---------|
| `welcome` | Branding, **Google** sign-in, theme toggle |
| `home` | Open amplifier, hearing-test prompt / status, account action, theme toggle |
| `audiogram` | Guided threshold test → save → optional jump to amplifier with profile applied |
| `amplifier` | Main processing UI (drawer: home, profile, FAQ, settings, audiogram, sign-out), presets, sliders, visualizer, custom presets, theme toggle |
| `settings` | Language, last tested, retake hearing test |
| `profile` | Account details, Firestore audiogram list, sign-out |
| `faq` | Help content |

**Cold start:** If **Firebase** already has a user session, **`home`** is the start destination; otherwise **`welcome`**. There is **no** “continue as guest” path. If the user signs out (or is not logged in), any attempt to stay on **`home`** is corrected to **`welcome`**.

---

## Architecture

```
Compose UI (MainActivity, screens, visualizer)
        ↓
ViewModels (AmplifierViewModel, AudiogramViewModel, AuthViewModel)
        ↓
NativeAudioEngine (JNI) ↔ OboeAudioEngine + AudioProcessor (C++)
        ↑
MicSourceManager (device callbacks, preferred Oboe input)
        ↑
ProfileRepository / CustomPresetDao (Room)  +  FirestoreUserRepository (when logged in)
```

- **`SunoXApp`** — `Application` entry; calls **`LocaleManager.syncAppCompatLocales`** so per-app language is applied early.
- **`MainActivity`** — Builds **Room** on a **background dispatcher** before `setContent` to avoid ANR; wires **NavHost**, **ThemeManager** state, **Google Sign-In** launcher, and auth-driven navigation.
- **`ThemeManager`** — SharedPreferences in `smarthear_prefs`: theme follow-system flag, explicit dark flag, and night mode sync via **`AppCompatDelegate`**. Legacy **`guest_mode`** is stripped on init so older installs are not stuck in a removed flow.
- **`AccountLocalIds.localKey`** — Returns Firebase **uid** when signed in, or a stable **`__guest__`** sentinel only for legacy Room rows / internal migration—not for entering the app without auth.

Legacy Kotlin types (**`AudioEngine`**, **`AudioProcessor`**) may remain in the tree; the live amplifier uses **Oboe** and **`native-lib`**.

---

## Native audio pipeline

1. Input stream (Oboe, preferred device when set)
2. **WDRC-style path** — multi-band bandpass, prescribed gains, expansion + compression shaped by “boost quiet sounds”
3. Low / high shelf (native layer)
4. Master gain and soft limiting
5. Output stream

---

## Local database (Room)

| Item | Detail |
|------|--------|
| **Database name** | `sound-amplifier-db` |
| **Entities** | `AudiogramProfile`, `CustomPreset` |
| **Current schema version** | **4** |
| **Migrations** | `MIGRATION_2_3` (per-account `accountId` columns + composite indexes on `accountId`, `createdAt`); `MIGRATION_3_4` (idempotent index repair for databases that reached v3 without indexes) |
| **Fallback** | `fallbackToDestructiveMigration()` remains for **unhandled** version jumps during development—**production** upgrades should rely on explicit migrations |

Implementation files: `ProfileRepository.kt` (`@Database`), `RoomMigrations.kt`, generated DAOs under `build/generated/`.

---

## Data & cloud sync

| Data | Local | Cloud (Firebase user) |
|------|--------|------------------------|
| Audiogram profiles | Room (`audiogram_profiles`, keyed by `accountId`) | Firestore merge after login |
| Custom presets | Room (`custom_presets`, keyed by `accountId`) | Firestore merge after login |
| Hearing test prompt flags | `HearingTestPreferences` (SharedPreferences-style helpers) | — |

NAL-R–inspired gain derivation: **`thresholdsToGains()`** in `AudiogramProfile.kt`.

---

## Localization

Default **`values/strings.xml`** plus:

- **Hindi** (`values-hi`)
- **Marathi** (`values-mr`)
- **Telugu** (`values-te`)
- **Gujarati** (`values-gu`)
- **Tamil** (`values-ta`)

**`LocaleManager`** coordinates with **`AppCompatDelegate.setApplicationLocales`**; **`SettingsScreen`** offers radio choices that map to those codes.

---

## Repository layout

```
SoundAmplifier/
├── app/
│   ├── build.gradle
│   ├── google-services.json          # From Firebase (local / CI secret)
│   └── src/main/
│       ├── java/com/soundamplifier/
│       │   ├── ui/                   # MainActivity, screens, LocaleManager, visualizer
│       │   ├── viewmodel/
│       │   ├── audio/                # NativeAudioEngine, MicSourceManager, presets
│       │   ├── auth/                 # AuthViewModel (Firebase Google Sign-In)
│       │   └── data/                 # Room, migrations, Firestore, repositories
│       ├── cpp/                      # CMake, JNI, Oboe, DSP
│       ├── res/
│       │   └── values/themes.xml     # AppCompat DayNight host theme
│       └── AndroidManifest.xml
├── firebase.json                     # Firestore rules + Functions source (CLI deploy)
├── functions/                        # Cloud Functions (session revoke); see functions/README.md
├── keystore.properties.example       # Template for release signing (copy to keystore.properties)
├── firestore.rules
├── build.gradle                      # AGP, Kotlin, KSP, Google Services plugin versions
├── settings.gradle
└── README.md
```

Sign-in is **Firebase Auth** with **Google** only (no email or phone flows in the app).

---

## Tech stack

| Piece | Role |
|-------|------|
| Kotlin 1.9.22 · Java 17 | App, ViewModels |
| Android Gradle Plugin 8.2.0 | Build |
| Jetpack Compose (BOM 2024.02) · Material 3 | UI |
| Navigation Compose 2.7.6 | Graph |
| AppCompat 1.7.0 · DayNight theme | Locale + system UI mode |
| Room 2.6.1 · KSP | Audiograms & presets |
| Coroutines · Flow | Async UI state |
| Firebase BOM 33.5.1 · Auth · Firestore | Sign-in & cloud sync |
| Firebase Cloud Functions (optional, Node 18+) | `revokeRefreshTokens` on `sessionToken` change |
| Play services auth | Google Sign-In ID token |
| Oboe 1.7 (prefab) | Low-latency I/O |
| C++17 · NDK 25.1 | DSP and engine |

---

## Troubleshooting

| Issue | What to check |
|-------|----------------|
| Google Sign-In fails / “not configured” **Toast** | `default_web_client_id` in `strings.xml`, SHA-1/256 in Firebase, valid `google-services.json` |
| No sound on emulator | Expected limitation; test on hardware |
| Build / CMake / `package.xml` warnings | Align Android Studio, SDK command-line tools, and NDK versions |
| Glitchy emulator graphics | Prefer host GPU; cold boot with `-no-snapshot-load` |
| Room crash: “Migration didn't properly handle…” / missing **index** | Install a build with **migrations through v4** (`MIGRATION_3_4`); avoid shipping schema changes without a matching `Migration` if you remove `fallbackToDestructiveMigration` in production |
| App opens **`welcome`** after you used “guest” before | Guest mode was removed; sign in with Google; stale `guest_mode` is cleared in **`ThemeManager.init`** |

---

## Future ideas

- Stronger noise suppression (e.g. RNNoise)
- Feedback detection / cancellation
- Automatic scene / environment classification
- Optional “use system theme” / “follow system again” control after the user overrides appearance
- Play Console checklist, Play App Signing notes, and stricter Firestore rules documentation

---

*SunoX — Kotlin, Compose, Room, Firebase, Oboe, and JNI.*
#   S u n o X  
 