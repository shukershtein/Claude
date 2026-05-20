# Android App

Native Kotlin + Jetpack Compose. Phase 1: single push-to-talk button, Hebrew STT/TTS, talks to the backend.

## Setup

1. **Bootstrap the Gradle wrapper.** This scaffold doesn't ship the `gradle-wrapper.jar` binary; either:
   - Open the `android/` directory in Android Studio (it generates the wrapper on first sync), or
   - Run `gradle wrapper` from this directory if you have a system Gradle.
2. **Configure backend URL and device token.** Create `android/local.properties`:
   ```properties
   BACKEND_URL=http://10.0.2.2:8080
   DEVICE_TOKEN=the-same-string-you-put-in-backend/.env
   ```
   - `10.0.2.2` is the emulator's alias for your host machine's `localhost`.
   - For a real phone on the same Wi-Fi, use your machine's LAN IP, e.g. `http://192.168.1.42:8080`.
   - For a deployed VPS, use `https://your-domain.tld` (and remove `usesCleartextTraffic="true"` from the manifest).
3. **Install Hebrew TTS voice.** On your Android device: Settings → System → Languages → Text-to-speech → install Hebrew. Without this, the agent will fall back to whatever default voice is configured.

## Build and run

```bash
./gradlew :app:installDebug
```

Or hit Run in Android Studio with a connected device or emulator.

## How it works (Phase 1)

1. Hold the big button → `SpeechRecognizer` listens with locale `he-IL`.
2. Release the button → final transcript is shown and POSTed to the backend.
3. Backend reply is read aloud via `TextToSpeech` (locale `he-IL`).

`conversationId` is a UUID generated per app launch, so closing the app starts a fresh conversation. Phase 2 wires persistent identifiers + semantic recall so it actually remembers across launches.

## What's not here yet

- Wake word (Phase 2)
- Foreground service / always-on listening (Phase 2)
- Tools — calendar, web search, etc. (Phase 3)
- RTL polish on the transcript view (Phase 3)
