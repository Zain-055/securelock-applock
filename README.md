# SecureLock AppLock MVP

**SecureLock** is a functional, lightweight, privacy-first Android AppLock MVP built with modern Jetpack Compose, Material 3, and official Android platform APIs (`UsageStatsManager`, `BiometricPrompt`, `DataStore`, `PBKDF2` hashing).

---

## Objective & Features

1. **Authentication**:
   - **PIN**: 4–6 digit numerical PIN.
   - **Pattern**: Standard 3×3 interactive grid canvas with gesture drawing and haptic feedback.
   - **Biometric**: `BiometricPrompt` fingerprint/biometric integration with fallback to PIN/Pattern.
   - **Security**: PBKDF2WithHmacSHA256 with 16-byte random salt. Constant-time byte comparison. `FLAG_SECURE` prevents screenshots/recording during authentication. Plaintext PINs/patterns are **never** stored or logged.
   - **Rate Limiting**: 5 consecutive failed attempts trigger a 30-second lockout timer.

2. **App Detection Engine (`AppLockManager` & Service)**:
   - Uses official Android `UsageStatsManager` inside a Foreground Service to detect foreground application changes.
   - Low battery consumption: Polling pauses when screen turns OFF.

3. **Re-Lock Policies**:
   - **Immediately**: Requires lock whenever protected app is foregrounded.
   - **After leaving the app**: Stays unlocked while inside app; locks immediately upon switching away.
   - **30 Seconds**: Remains unlocked for 30s after leaving app.
   - **1 Minute**: Remains unlocked for 60s after leaving app.

4. **100% Local & Offline**:
   - Zero network requests, zero cloud dependencies, zero analytics tracking.

5. **Reboot Recovery**:
   - `BootReceiver` restores protection service on device reboot if enabled.

---

## Project Structure

```
app/src/main/java/com/example/applock/
├── AppLockApplication.kt               // Application class
├── MainActivity.kt                      // Compose Navigation host
├── data/
│   ├── datastore/
│   │   └── SecureLockPreferences.kt   // Jetpack DataStore Preferences
│   ├── models/
│   │   ├── AppInfo.kt                 // App model (label, icon, pkg)
│   │   ├── AppLockSettings.kt         // Preferences data class
│   │   ├── AuthType.kt                // PIN / PATTERN
│   │   └── ReLockPolicy.kt            // Re-lock policies
│   └── repository/
│       ├── AppRepository.kt            // Package discovery & cleanup
│       └── SecurityRepository.kt       // Credential verification wrapper
├── security/
│   ├── CredentialHasher.kt            // PBKDF2 / SHA-256 with salt
│   └── BiometricHelper.kt             // BiometricPrompt wrapper
├── monitoring/
│   ├── AppLockService.kt              // Foreground service polling UsageStats
│   ├── BootReceiver.kt                // BOOT_COMPLETED receiver
│   └── LockStateManager.kt            // Re-lock session state manager
└── ui/
    ├── onboarding/
    │   ├── OnboardingScreen.kt        // First run setup wizard
    │   └── OnboardingViewModel.kt
    ├── home/
    │   ├── DashboardScreen.kt         // Material 3 Dashboard
    │   └── DashboardViewModel.kt
    ├── apps/
    │   ├── AppSelectionScreen.kt      // Search & select apps
    │   └── AppSelectionViewModel.kt
    ├── lock/
    │   ├── LockOverlayActivity.kt     // Full-screen Lock Barrier Activity
    │   ├── PinLockView.kt             // PIN Keypad UI
    │   ├── PatternLockView.kt         // 3x3 Pattern Canvas UI
    │   └── LockViewModel.kt
    ├── settings/
    │   ├── SettingsScreen.kt          // Re-Lock & Security settings
    │   └── SettingsViewModel.kt
    └── theme/                         // Material 3 Color, Type, Theme
```

---

## Setup & Build Instructions

1. Open project in **Android Studio (2024+)**.
2. Run `./gradlew assembleDebug` to build APK.
3. Run `./gradlew testDebugUnitTest` to execute test suite.
4. If Gradle hits a configuration-cache or JDK toolchain issue, set a local JDK 21/25 on `JAVA_HOME` and run `./gradlew --no-configuration-cache assembleDebug`.
5. Deploy to Android test device (Android 10 - 15).
6. Grant required **Usage Access** and **Display Over Apps** permissions during onboarding.
