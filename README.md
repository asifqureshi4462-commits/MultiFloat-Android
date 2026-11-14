# MultiFloat — Smart Multitasking Platform

**Tagline:** *Multiple Tools. One Screen. Smarter Multitasking.*

MultiFloat is a native Java + XML Android multitasking application built with Material Design 3, MVVM + Repository architecture, Room persistence, and a custom Android `WindowManager` floating window engine.

---

## Key Features

### 1. Floating Window Engine (`FloatingWindowService`)
- **Floating Bubble:** Draggable orb with live active-window badge, edge snapping, customizable size/transparency, and an inline quick-launcher & window switcher popover.
- **Intelligent Edge Sidebar:** Dockable on the Left or Right edge with customizable size/transparency, Favorite tools, Recent tools, Flashlight toggle, and System Settings shortcut.
- **Multi-Window Overlay System:** Open multiple simultaneous floating windows with draggable title bars, edge snapping, bottom-right resize handles, Minimize/Restore, Maximize/Restore, per-window transparency slider, active-window focus indicators, automatic keyboard focus handling, and session persistence.

### 2. 12 Built-In Mini Applications
Each mini-app works both inside a **Floating Window** and as a **Full-Screen Activity**:
1. **Calculator:** Basic & scientific modes (`sin`, `cos`, `tan`, `sqrt`, `log`, `ln`, `^`, `π`), Room calculation history, copy result.
2. **Quick Notes:** Create, edit, search, pin, delete, and debounced auto-save to Room.
3. **Mini Browser:** WebView with URL normalization, search fallback, back/forward/refresh/home, and progress indicator.
4. **Stopwatch & Timer:** Millisecond stopwatch with laps + countdown timer with 1m/5m/10m presets and custom minutes.
5. **Digital Clock:** Live clock, date, timezone, and 12h/24h format toggle.
6. **Text Editor:** Multiline editor with word/character counter, Room file save/load, copy/paste, and Android share.
7. **Clipboard Manager:** Room-persisted clipboard history, manual capture, 1-tap copy, and delete.
8. **To-Do List:** Tasks with priority tags (`High`, `Normal`, `Low`), completion toggle, and Room storage.
9. **QR Scanner:** Live CameraX + ZXing QR decoding with copy and URL opening (handles runtime `CAMERA` permission cleanly).
10. **Translator:** Language selector, swap, Google Cloud Translation v2 / LibreTranslate API integration, and graceful missing-API prompt.
11. **AI Assistant:** Floating chat interface with Room chat history, loading/error states, and Gemini / OpenAI-compatible API support.
12. **Music Controls:** System media playback dispatch (Play/Pause, Next, Previous, Volume) + optional active track metadata via `MediaSessionMonitorService`.

---

## Architecture

- **Language & UI:** 100% Java + XML Layouts + ViewBinding + Material Design 3 (`DayNight`)
- **Data Layer:** Room Database (`MultiFloatDatabase`) + DAOs + Repositories (`NotesRepository`, `TodoRepository`, `ClipboardRepository`, `CalculatorRepository`, `AiChatRepository`, `TranslatorRepository`, `TextFileRepository`, `ToolsRepository`)
- **Security:** `SecureKeyStore` encrypts user-configured AI and Translation API keys using `AndroidKeyStore` AES-256-GCM.
- **Services:** `FloatingWindowService` (`foregroundServiceType="specialUse"`) and optional `MediaSessionMonitorService`.

---

## Build Instructions

1. Open the project in Android Studio (JDK 11+, Android SDK 36, Min SDK 26).
2. Run unit and Robolectric tests:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```
3. Build a debug APK:
   ```bash
   ./gradlew :app:assembleDebug
   ```
   Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## Permission Setup Instructions

- **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`):** Required for floating bubbles, edge sidebar, and floating windows. Grant via the in-app **System Permissions** screen (`PermissionSetupActivity`).
- **Notifications (`POST_NOTIFICATIONS` on Android 13+):** Keeps the foreground engine notification visible with quick Minimize All / Close All / Stop controls.
- **Camera (`CAMERA`):** Requested at runtime only when using the QR Scanner or Flashlight.
- **Notification Access (Optional):** Enable in Android Notification Listener settings if you want the Music Controls mini-app to display current track title and artist.

---

## API Configuration Instructions

MultiFloat never hardcodes API keys. To use **AI Assistant** or **Translator**:
1. Open **General & API Settings** (`GeneralSettingsActivity`) from the Dashboard gear icon.
2. **AI Assistant:** Choose **Gemini API** (default model `gemini-2.5-flash`) or **OpenAI Compatible**, then enter your API key.
3. **Translator:** Enter a **Google Cloud Translation v2** endpoint + API key, or a **LibreTranslate** endpoint URL.
4. Tap **Save Settings & Encrypted Keys** to encrypt and store credentials on-device via `AndroidKeyStore`.

---

## Release APK Preparation

1. Set environment variables for your upload keystore (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`).
2. Enable `isMinifyEnabled = true` in `app/build.gradle.kts` if desired and verify ProGuard rules.
3. Run:
   ```bash
   ./gradlew :app:assembleRelease
   ```
   or generate a Play App Bundle:
   ```bash
   ./gradlew :app:bundleRelease
   ```
