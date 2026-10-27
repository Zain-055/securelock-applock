package com.example.applock.ui.settings

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.applock.data.datastore.SecureLockPreferences
import com.example.applock.data.models.AppLockSettings
import com.example.applock.data.models.AuthType
import com.example.applock.data.models.ReLockPolicy
import com.example.applock.data.repository.SecurityRepository
import com.example.applock.monitoring.SecureLockDeviceAdminReceiver
import com.example.applock.security.BiometricHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppLockSettings = AppLockSettings(),
    val isBiometricHardwareAvailable: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val showChangeCredentialDialog: Boolean = false,
    val newCredentialInput: String = "",
    val confirmCredentialInput: String = "",
    val credentialError: String = "",
    val newAuthType: AuthType = AuthType.PIN
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = SecureLockPreferences(application)
    private val securityRepository = SecurityRepository(preferences)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val biometricAvail = BiometricHelper.isBiometricAvailable(getApplication())
        val isAdminActive = checkDeviceAdminActive()

        viewModelScope.launch {
            val currentSettings = securityRepository.settingsFlow.first()
            _uiState.value = _uiState.value.copy(
                settings = currentSettings,
                isBiometricHardwareAvailable = biometricAvail,
                isDeviceAdminActive = isAdminActive
            )
        }
    }

    fun toggleUninstallPrevention(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            securityRepository.setUninstallPreventionEnabled(enabled)
            if (enabled && !checkDeviceAdminActive()) {
                requestDeviceAdmin(context)
            }
            loadSettings()
        }
    }

    fun requestDeviceAdmin(context: Context) {
        val component = ComponentName(context, SecureLockDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enabling Device Admin prevents unauthorized users from uninstalling SecureLock without your PIN/Pattern."
            )
        }
        context.startActivity(intent)
    }

    private fun checkDeviceAdminActive(): Boolean {
        val dpm = getApplication<Application>().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(getApplication(), SecureLockDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(component)
    }

    fun setReLockPolicy(policy: ReLockPolicy) {
        viewModelScope.launch {
            securityRepository.setReLockPolicy(policy)
            loadSettings()
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            securityRepository.setBiometricEnabled(enabled)
            loadSettings()
        }
    }

    fun setThemePreference(theme: String) {
        viewModelScope.launch {
            securityRepository.setThemePreference(theme)
            loadSettings()
        }
    }

    fun openChangeCredentialDialog(type: AuthType) {
        _uiState.value = _uiState.value.copy(
            showChangeCredentialDialog = true,
            newAuthType = type,
            newCredentialInput = "",
            confirmCredentialInput = "",
            credentialError = ""
        )
    }

    fun closeChangeCredentialDialog() {
        _uiState.value = _uiState.value.copy(showChangeCredentialDialog = false)
    }

    fun saveNewCredential(credential: String, confirm: String) {
        if (credential.isBlank()) {
            _uiState.value = _uiState.value.copy(credentialError = "Credential cannot be empty")
            return
        }
        if (credential != confirm) {
            _uiState.value = _uiState.value.copy(credentialError = "Credentials do not match")
            return
        }

        viewModelScope.launch {
            securityRepository.saveCredential(_uiState.value.newAuthType, credential)
            _uiState.value = _uiState.value.copy(showChangeCredentialDialog = false)
            loadSettings()
        }
    }
}
