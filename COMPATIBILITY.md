# SecureLock Android Compatibility & Platform Documentation

This document describes the tested behaviors, platform mechanics, official API usage, and known Android/OEM limitations for **SecureLock AppLock MVP**.

---

## Technical Compatibility Matrix

| Feature | Android 10 / 11 (API 29–30) | Android 12 / 13 (API 31–33) | Android 14 / 15 (API 34–35) | OEM Specific Limitations |
| :--- | :--- | :--- | :--- | :--- |
| **App Detection** | `UsageStatsManager.queryEvents` | `UsageStatsManager.queryEvents` | `UsageStatsManager.queryEvents` | Requires user grant in `Settings > Usage Access`. High-power saving modes on Xiaomi/Samsung may suspend service if unexempted. |
| **Lock Screen Overlay** | Full-screen `LockOverlayActivity` + `SYSTEM_ALERT_WINDOW` | Full-screen `LockOverlayActivity` + `SYSTEM_ALERT_WINDOW` | Full-screen `LockOverlayActivity` + `SYSTEM_ALERT_WINDOW` | Android 10+ restricts Background Activity Launch unless app holds `SYSTEM_ALERT_WINDOW` or runs foreground service. |
| **Biometric Prompt** | `BiometricPrompt` (Fingerprint/Face) | `BiometricPrompt` | `BiometricPrompt` | Fallback to PIN/Pattern always supported. |
| **Background Execution** | Foreground Service (`FOREGROUND_SERVICE`) | Foreground Service (`FOREGROUND_SERVICE`) | Foreground Service (`FOREGROUND_SERVICE_SPECIAL_USE`) | Android 14+ requires explicit `foregroundServiceType="specialUse"` and property declaration. |
| **Reboot Recovery** | `BOOT_COMPLETED` BroadcastReceiver | `BOOT_COMPLETED` BroadcastReceiver | `BOOT_COMPLETED` BroadcastReceiver | Devices with aggressive autostart management (Xiaomi MIUI/HyperOS, Vivo FuntouchOS) require enabling "Autostart". |

---

## Security Threat Model

### SecureLock Protects Against:
- Casual access to protected applications by someone using an unlocked device.
- Unintended or unauthorized opening of private apps.
- Data privacy leakage on shared or borrowed phones.

### SecureLock Does NOT Protect Against:
- Root access / compromised Android OS.
- Device Owner / ADB debugging level administrative commands.
- Hardware-level physical chip exploitation.
- Revocation of Usage Access permission by device owner.

---

## Implemented vs Android-Limited Classification

### IMPLEMENTED AND TESTED
- Real PIN & Pattern creation, confirmation, and changing.
- PBKDF2 credential hashing with unique random salts.
- Constant-time verification & 30s lockout after 5 failed attempts.
- Biometric authentication via `BiometricPrompt`.
- `UsageStatsManager` foreground application polling.
- `FLAG_SECURE` window configuration preventing screenshots.
- DataStore persistence for settings and protected apps list.
- Re-lock policies: `Immediately`, `After leaving app`, `30 seconds`, `1 minute`.
- Offline local execution.

### ANDROID-LIMITED / OEM-SPECIFIC
- **Background Activity Launch Restrictions**: Android 10+ restricts background activity launches unless the service holds `SYSTEM_ALERT_WINDOW` or starts the intent from an active interaction window.
- **Battery Optimization Policies**: OEMs (Samsung OneUI, Xiaomi HyperOS, Huawei HarmonyOS, OPPO ColorOS) aggressively kill background services. Users should set SecureLock battery usage to "Unrestricted" in System Settings.
