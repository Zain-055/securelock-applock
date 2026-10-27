package com.example.applock.ui.onboarding

import android.app.Application
import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.applock.data.datastore.SecureLockPreferences
import com.example.applock.data.models.AuthType
import com.example.applock.data.repository.SecurityRepository
import com.example.applock.security.BiometricHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 1, // 1: Welcome, 2: Choose Method, 3: Create Credential, 4: Biometric, 5: Permission, 6: Complete
    val selectedAuthType: AuthType = AuthType.PIN,
    val credentialInput: String = "",
    val confirmInput: String = "",
    val errorMessage: String = "",
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val hasUsageAccessPermission: Boolean = false,
    val hasOverlayPermission: Boolean = false
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = SecureLockPreferences(application)
    private val securityRepository = SecurityRepository(preferences)

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        val biometricAvail = BiometricHelper.isBiometricAvailable(getApplication())
        val hasUsageAccess = checkUsageAccessPermission()
        val hasOverlay = checkOverlayPermission()

        _uiState.value = _uiState.value.copy(
            isBiometricAvailable = biometricAvail,
            hasUsageAccessPermission = hasUsageAccess,
            hasOverlayPermission = hasOverlay
        )
    }

    fun nextStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = _uiState.value.currentStep + 1,
            errorMessage = ""
        )
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 1) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }

    fun selectAuthType(authType: AuthType) {
        _uiState.value = _uiState.value.copy(selectedAuthType = authType)
    }

    fun createCredential(credential: String, confirm: String) {
        if (credential.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Credential cannot be empty")
            return
        }
        if (credential != confirm) {
            _uiState.value = _uiState.value.copy(errorMessage = "Credentials do not match")
            return
        }

        viewModelScope.launch {
            securityRepository.saveCredential(_uiState.value.selectedAuthType, credential)
            _uiState.value = _uiState.value.copy(
                credentialInput = credential,
                confirmInput = confirm,
                errorMessage = "",
                currentStep = 4 // Move to Biometric step
            )
        }
    }

    fun setBiometricChoice(enable: Boolean) {
        viewModelScope.launch {
            securityRepository.setBiometricEnabled(enable)
            _uiState.value = _uiState.value.copy(
                isBiometricEnabled = enable,
                currentStep = 5 // Move to Permissions step
            )
        }
    }

    fun completeOnboarding(onFinish: () -> Unit) {
        viewModelScope.launch {
            securityRepository.setOnboardingCompleted(true)
            onFinish()
        }
    }

    private fun checkUsageAccessPermission(): Boolean {
        val appOpsManager = getApplication<Application>().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                getApplication<Application>().packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                getApplication<Application>().packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(getApplication())
    }
}
