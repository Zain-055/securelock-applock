package com.example.applock.data.repository

import com.example.applock.data.datastore.SecureLockPreferences
import com.example.applock.data.models.AuthType
import com.example.applock.data.models.ReLockPolicy
import com.example.applock.security.CredentialHasher
import kotlinx.coroutines.flow.first

class SecurityRepository(
    private val preferences: SecureLockPreferences
) {

    val settingsFlow = preferences.settingsFlow

    suspend fun saveCredential(authType: AuthType, credential: String) {
        val result = CredentialHasher.hashCredential(credential)
        val length = if (authType == AuthType.PIN) credential.length else 4
        preferences.saveCredential(
            authType = authType,
            hash = result.hashHex,
            salt = result.saltHex,
            pinLength = length
        )
    }

    suspend fun verifyCredential(candidate: String): Boolean {
        val settings = settingsFlow.first()
        if (settings.credentialHash.isBlank() || settings.credentialSalt.isBlank()) {
            return false
        }
        return CredentialHasher.verifyCredential(
            candidate = candidate,
            storedHashHex = settings.credentialHash,
            storedSaltHex = settings.credentialSalt
        )
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        preferences.setBiometricEnabled(enabled)
    }

    suspend fun setUninstallPreventionEnabled(enabled: Boolean) {
        preferences.setUninstallPreventionEnabled(enabled)
    }

    suspend fun setReLockPolicy(policy: ReLockPolicy) {
        preferences.setReLockPolicy(policy)
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        preferences.setProtectionEnabled(enabled)
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        preferences.setOnboardingCompleted(completed)
    }

    suspend fun setThemePreference(theme: String) {
        preferences.setThemePreference(theme)
    }
}
