# 🎧 SunoX  
### Real-Time Sound Amplifier for Android

<p align="center">
  <img src="screenshots/logo.jpeg" width="140" alt="SunoX Logo"/>
</p>

<p align="center">
  <b>Low-latency sound amplification app built using Kotlin, Jetpack Compose, Firebase, Oboe & Native C++ DSP.</b>
</p>

<p align="center">

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple)
![Min SDK](https://img.shields.io/badge/MinSDK-26-orange)
![Firebase](https://img.shields.io/badge/Firebase-Enabled-yellow)
![NDK](https://img.shields.io/badge/NDK-C++17-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

</p>

---

# 📱 About The Project

**SunoX** is a real-time Android sound amplification application that captures microphone audio, processes it using a native low-latency DSP pipeline, and plays enhanced audio directly to headphones or hearing devices.

The application is designed to improve hearing experiences in:
- Daily conversations
- Classrooms
- TV & media listening
- Outdoor environments

⚠️ **Use headphones while amplification is active to reduce acoustic feedback.**  
This application is **not a certified medical hearing aid**.

---

# ✨ Features

## 🎤 Real-Time Audio Amplification
- Ultra low-latency audio processing using **Oboe**
- Native DSP engine written in **C++17**
- Live microphone capture & playback
- Dynamic range compression & amplification

---

## 🎚️ Smart Audio Controls
- Boost quiet sounds
- Adjustable output gain
- Low-frequency enhancement
- High-frequency enhancement
- Built-in sound presets

### Available Presets
- Conversation
- Music
- Outdoors
- Classroom
- TV / Media

---

## 💾 Custom Presets
- Save personalized sound settings
- Apply presets instantly
- Delete presets anytime
- Firebase Firestore sync support

---

## 👂 Hearing Test & Audiogram
- Frequency-based hearing test
- Audiogram profile storage
- Personalized gain adjustment
- NAL-R inspired sound enhancement

---

## 🔐 Authentication & Cloud Sync
- Google Sign-In using Firebase Auth
- Firestore cloud backup
- Cross-device synchronization
- Optional single-device session management

---

## 🌍 Multi-Language Support

Supported languages:
- English
- Hindi
- Marathi
- Gujarati
- Tamil
- Telugu

---

# 📸 App Screenshots

> Add your screenshots inside a folder named `screenshots` in your repository.

---

## 🔓 Welcome Screen

<p align="center">
  <img src="screenshots/welcome.png" width="250"/>
</p>

---

## 🏠 Home Screen

<p align="center">
  <img src="screenshots/home.png" width="250"/>
</p>

---

## 🎛️ Amplifier Screen

<p align="center">
  <img src="screenshots/amplifier.png" width="250"/>
</p>

---

## 👂 Hearing Test Screen

<p align="center">
  <img src="screenshots/hearing-test.png" width="250"/>
</p>

---

## ⚙️ Settings Screen

<p align="center">
  <img src="screenshots/settings.png" width="250"/>
</p>

---

# 🛠️ Tech Stack

| Technology | Usage |
|------------|-------|
| Kotlin | Android Development |
| Jetpack Compose | UI Framework |
| Firebase Auth | Authentication |
| Firestore | Cloud Database |
| Room Database | Local Storage |
| Oboe | Low Latency Audio |
| C++17 / JNI | Native DSP Processing |
| Coroutines & Flow | Async Programming |
| Material 3 | UI Design |

---

# 🧱 Architecture

```text
Compose UI
    ↓
ViewModels
    ↓
NativeAudioEngine (JNI)
    ↓
Oboe Audio Engine + DSP
    ↓
Room Database + Firebase
```

---

# 📂 Project Structure

```bash
SoundAmplifier/
│
├── app/
│   ├── src/main/java/com/soundamplifier/
│   │   ├── ui/
│   │   ├── viewmodel/
│   │   ├── audio/
│   │   ├── auth/
│   │   └── data/
│   │
│   ├── cpp/
│   ├── res/
│   └── AndroidManifest.xml
│
├── functions/
├── firebase.json
├── firestore.rules
└── README.md
```

---

# ⚙️ Requirements

- Android 8.0+ (API 26)
- JDK 17
- Android Studio
- Android SDK 34
- Android NDK 25.1
- CMake 3.22+

---

# 🔥 Firebase Setup

1. Create a Firebase project
2. Add Android app:
   ```text
   com.soundamplifier
   ```

3. Enable:
   - Firebase Authentication
   - Google Sign-In
   - Firestore Database

4. Download:
   ```text
   google-services.json
   ```

5. Place it inside:
   ```text
   app/
   ```

---

# 🚀 Installation

## Clone Repository

```bash
git clone https://github.com/yourusername/SunoX.git
```

---

## Build Debug APK

```bash
./gradlew assembleDebug
```

---

## Install on Device

```bash
./gradlew installDebug
```

---

# 📦 Build Release AAB

```bash
./gradlew bundleRelease
```

Generated file:

```text
app/build/outputs/bundle/release/app-release.aab
```

---

# 🔒 Permissions Used

| Permission | Purpose |
|------------|---------|
| RECORD_AUDIO | Microphone input |
| MODIFY_AUDIO_SETTINGS | Audio routing |
| BLUETOOTH_CONNECT | Bluetooth audio devices |

---

# ⚠️ Important Notes

- Physical device testing is recommended
- Emulator microphones may not work properly
- Headphones are strongly recommended
- This app is not medically certified

---

# 🧪 Future Improvements

- AI-based noise suppression
- Feedback cancellation
- Environment detection
- Better audio visualization
- Smart automatic presets

---

# 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a new branch
3. Commit your changes
4. Open a Pull Request

---

# 📜 License

This project is licensed under the MIT License.

---

# 👩‍💻 Developed By

### Sakshi Kusmude

Built with ❤️ using Kotlin, Compose, Firebase & Native Audio DSP.

---

# ⭐ Support

If you like this project:

🌟 Star the repository  
🍴 Fork it  
📢 Share it with others
