package com.example.applock.ui.lock

import android.app.Application
import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.applock.data.datastore.SecureLockPreferences
import com.example.applock.data.models.AuthType
import com.example.applock.data.repository.SecurityRepository
import com.example.applock.monitoring.LockStateManager
import com.example.applock.security.BiometricHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LockUiState(
    val packageName: String = "",
    val appLabel: String = "App Locked",
    val authType: AuthType = AuthType.PIN,
    val pinLength: Int = 4,
    val enteredPin: String = "",
    val errorMessage: String = "",
    val failedAttempts: Int = 0,
    val lockoutSecondsRemaining: Int = 0,
    val isBiometricAvailable: Boolean = false,
    val isUnlocked: Boolean = false
)

class LockViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = SecureLockPreferences(application)
    private val securityRepository = SecurityRepository(preferences)

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private var lockoutJob: Job? = null

    fun initialize(packageName: String) {
        val pm = getApplication<Application>().packageManager
        val label = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val biometricAvail = BiometricHelper.isBiometricAvailable(getApplication())

        viewModelScope.launch {
            val settings = securityRepository.settingsFlow.first()
            val failedAttempts = preferences.getFailedAttempts()
            val lockoutUntil = preferences.getLockoutUntil()
            val remainingSeconds = ((lockoutUntil - System.currentTimeMillis()) / 1000L)
                .coerceAtLeast(0L)
                .toInt()

            _uiState.value = _uiState.value.copy(
                packageName = packageName,
                appLabel = label,
                authType = settings.authType,
                pinLength = settings.pinLength,
                failedAttempts = failedAttempts,
                lockoutSecondsRemaining = remainingSeconds,
                isBiometricAvailable = settings.isBiometricEnabled && biometricAvail
            )
        }
    }

    fun onPinChanged(newPin: String) {
        if (_uiState.value.lockoutSecondsRemaining > 0) return
        _uiState.value = _uiState.value.copy(
            enteredPin = newPin,
            errorMessage = ""
        )
    }

    fun submitPin(pin: String) {
        if (_uiState.value.lockoutSecondsRemaining > 0) return
        verifyCredential(pin)
    }

    fun submitPattern(pattern: String) {
        if (_uiState.value.lockoutSecondsRemaining > 0) return
        verifyCredential(pattern)
    }

    private fun verifyCredential(candidate: String) {
        viewModelScope.launch {
            val isValid = securityRepository.verifyCredential(candidate)
            if (isValid) {
                onUnlockSuccess()
            } else {
                onUnlockFailed()
            }
        }
    }

    fun authenticateWithBiometric(activity: FragmentActivity) {
        if (!_uiState.value.isBiometricAvailable || _uiState.value.lockoutSecondsRemaining > 0) return

        BiometricHelper.authenticate(
            activity = activity,
            title = "${_uiState.value.appLabel} Locked",
            subtitle = "Use biometric to unlock",
            onSuccess = {
                onUnlockSuccess()
            },
            onError = { _, errString ->
                _uiState.value = _uiState.value.copy(errorMessage = errString.toString())
            },
            onFailed = {
                onUnlockFailed()
            }
        )
    }

    private fun onUnlockSuccess() {
        val pkg = _uiState.value.packageName
        if (pkg.isNotBlank()) {
            LockStateManager.onAppUnlocked(pkg)
        }
        viewModelScope.launch {
            preferences.clearFailedAttemptState()
        }
        _uiState.value = _uiState.value.copy(
            isUnlocked = true,
            enteredPin = "",
            errorMessage = "",
            failedAttempts = 0,
            lockoutSecondsRemaining = 0
        )
    }

    private fun onUnlockFailed() {
        val newAttempts = _uiState.value.failedAttempts + 1
        if (newAttempts >= 5) {
            val lockoutUntil = System.currentTimeMillis() + 30_000L
            startLockoutTimer(30)
            viewModelScope.launch {
                preferences.setFailedAttempts(0)
                preferences.setLockoutUntil(lockoutUntil)
            }
            _uiState.value = _uiState.value.copy(
                enteredPin = "",
                errorMessage = "Too many failed attempts. Locked out for 30 seconds.",
                failedAttempts = 0,
                lockoutSecondsRemaining = 30
            )
        } else {
            viewModelScope.launch {
                preferences.setFailedAttempts(newAttempts)
            }
            _uiState.value = _uiState.value.copy(
                enteredPin = "",
                errorMessage = "Incorrect credential. Attempt $newAttempts of 5.",
                failedAttempts = newAttempts
            )
        }
    }

    private fun startLockoutTimer(seconds: Int) {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _uiState.value = _uiState.value.copy(lockoutSecondsRemaining = remaining)
                delay(1000)
                remaining--
            }
            preferences.setLockoutUntil(0L)
            _uiState.value = _uiState.value.copy(
                lockoutSecondsRemaining = 0,
                errorMessage = ""
            )
        }
    }
}
