package com.example.applock.data.models

data class AppLockSettings(
    val isProtectionEnabled: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val authType: AuthType = AuthType.PIN,
    val credentialHash: String = "",
    val credentialSalt: String = "",
    val pinLength: Int = 4,
    val isBiometricEnabled: Boolean = false,
    val isUninstallPreventionEnabled: Boolean = false,
    val reLockPolicy: ReLockPolicy = ReLockPolicy.AFTER_LEAVING,
    val protectedPackages: Set<String> = emptySet(),
    val themePreference: String = "SYSTEM" // SYSTEM, LIGHT, DARK
)
