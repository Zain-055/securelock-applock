package com.example.applock.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.applock.data.models.AppLockSettings
import com.example.applock.data.models.AuthType
import com.example.applock.data.models.ReLockPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_lock_prefs")

class SecureLockPreferences(private val context: Context) {

    companion object {
        val KEY_PROTECTION_ENABLED = booleanPreferencesKey("is_protection_enabled")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val KEY_AUTH_TYPE = stringPreferencesKey("auth_type")
        val KEY_CREDENTIAL_HASH = stringPreferencesKey("credential_hash")
        val KEY_CREDENTIAL_SALT = stringPreferencesKey("credential_salt")
        val KEY_PIN_LENGTH = intPreferencesKey("pin_length")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val KEY_UNINSTALL_PREVENTION_ENABLED = booleanPreferencesKey("is_uninstall_prevention_enabled")
        val KEY_RELOCK_POLICY = stringPreferencesKey("relock_policy")
        val KEY_PROTECTED_PACKAGES = stringSetPreferencesKey("protected_packages")
        val KEY_THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val KEY_FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val KEY_LOCKOUT_UNTIL = longPreferencesKey("lockout_until")
    }

    val settingsFlow: Flow<AppLockSettings> = context.dataStore.data.map { prefs ->
        AppLockSettings(
            isProtectionEnabled = prefs[KEY_PROTECTION_ENABLED] ?: true,
            isOnboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            authType = try {
                AuthType.valueOf(prefs[KEY_AUTH_TYPE] ?: AuthType.PIN.name)
            } catch (e: Exception) {
                AuthType.PIN
            },
            credentialHash = prefs[KEY_CREDENTIAL_HASH] ?: "",
            credentialSalt = prefs[KEY_CREDENTIAL_SALT] ?: "",
            pinLength = prefs[KEY_PIN_LENGTH] ?: 4,
            isBiometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: false,
            isUninstallPreventionEnabled = prefs[KEY_UNINSTALL_PREVENTION_ENABLED] ?: false,
            reLockPolicy = try {
                ReLockPolicy.valueOf(prefs[KEY_RELOCK_POLICY] ?: ReLockPolicy.AFTER_LEAVING.name)
            } catch (e: Exception) {
                ReLockPolicy.AFTER_LEAVING
            },
            protectedPackages = prefs[KEY_PROTECTED_PACKAGES] ?: emptySet(),
            themePreference = prefs[KEY_THEME_PREFERENCE] ?: "SYSTEM"
        )
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROTECTION_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun saveCredential(authType: AuthType, hash: String, salt: String, pinLength: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TYPE] = authType.name
            prefs[KEY_CREDENTIAL_HASH] = hash
            prefs[KEY_CREDENTIAL_SALT] = salt
            prefs[KEY_PIN_LENGTH] = pinLength
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setUninstallPreventionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_UNINSTALL_PREVENTION_ENABLED] = enabled
        }
    }

    suspend fun setReLockPolicy(policy: ReLockPolicy) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RELOCK_POLICY] = policy.name
        }
    }

    suspend fun togglePackageProtected(packageName: String, isProtected: Boolean) {
        context.dataStore.edit { prefs ->
            val currentSet = prefs[KEY_PROTECTED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (isProtected) {
                currentSet.add(packageName)
            } else {
                currentSet.remove(packageName)
            }
            prefs[KEY_PROTECTED_PACKAGES] = currentSet
        }
    }

    suspend fun setProtectedPackages(packages: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROTECTED_PACKAGES] = packages
        }
    }

    suspend fun setThemePreference(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_PREFERENCE] = theme
        }
    }

    suspend fun setFailedAttempts(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FAILED_ATTEMPTS] = value
        }
    }

    suspend fun setLockoutUntil(timestampMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOCKOUT_UNTIL] = timestampMs
        }
    }

    suspend fun clearFailedAttemptState() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_FAILED_ATTEMPTS)
            prefs.remove(KEY_LOCKOUT_UNTIL)
        }
    }

    suspend fun getFailedAttempts(): Int = context.dataStore.data.map { prefs ->
        prefs[KEY_FAILED_ATTEMPTS] ?: 0
    }.first()

    suspend fun getLockoutUntil(): Long = context.dataStore.data.map { prefs ->
        prefs[KEY_LOCKOUT_UNTIL] ?: 0L
    }.first()
}
