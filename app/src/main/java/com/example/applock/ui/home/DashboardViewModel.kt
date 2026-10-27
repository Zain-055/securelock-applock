package com.example.applock.ui.home

import android.app.Application
import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.applock.data.datastore.SecureLockPreferences
import com.example.applock.data.models.AppInfo
import com.example.applock.data.repository.AppRepository
import com.example.applock.data.repository.SecurityRepository
import com.example.applock.monitoring.AppLockService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isProtectionEnabled: Boolean = true,
    val protectedAppsCount: Int = 0,
    val protectedAppsList: List<AppInfo> = emptyList(),
    val hasUsageAccessPermission: Boolean = true,
    val hasOverlayPermission: Boolean = true,
    val isLoading: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = SecureLockPreferences(application)
    private val securityRepository = SecurityRepository(preferences)
    private val appRepository = AppRepository(application, preferences)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val settings = securityRepository.settingsFlow.first()
            val allApps = appRepository.getInstalledApplications()
            val protectedApps = allApps.filter { it.isProtected }

            val hasUsageAccess = checkUsageAccessPermission()
            val hasOverlay = checkOverlayPermission()

            _uiState.value = DashboardUiState(
                isProtectionEnabled = settings.isProtectionEnabled,
                protectedAppsCount = protectedApps.size,
                protectedAppsList = protectedApps,
                hasUsageAccessPermission = hasUsageAccess,
                hasOverlayPermission = hasOverlay,
                isLoading = false
            )

            // Start or stop background service based on state
            if (settings.isProtectionEnabled && protectedApps.isNotEmpty() && hasUsageAccess) {
                AppLockService.startService(getApplication())
            }
        }
    }

    fun toggleProtection(enabled: Boolean) {
        viewModelScope.launch {
            securityRepository.setProtectionEnabled(enabled)
            if (enabled) {
                AppLockService.startService(getApplication())
            } else {
                AppLockService.stopService(getApplication())
            }
            refreshDashboard()
        }
    }

    fun toggleAppProtection(packageName: String, isProtected: Boolean) {
        viewModelScope.launch {
            appRepository.toggleAppProtection(packageName, isProtected)
            refreshDashboard()
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
