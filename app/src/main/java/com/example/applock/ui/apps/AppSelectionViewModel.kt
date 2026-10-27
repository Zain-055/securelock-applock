package com.example.applock.ui.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.applock.data.datastore.SecureLockPreferences
import com.example.applock.data.models.AppInfo
import com.example.applock.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppSelectionUiState(
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class AppSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = SecureLockPreferences(application)
    private val appRepository = AppRepository(application, preferences)

    private val _uiState = MutableStateFlow(AppSelectionUiState())
    val uiState: StateFlow<AppSelectionUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps(query: String = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, searchQuery = query)
            val appList = appRepository.getInstalledApplications(query)
            _uiState.value = _uiState.value.copy(
                apps = appList,
                isLoading = false
            )
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(searchQuery = newQuery)
        loadApps(newQuery)
    }

    fun toggleAppProtection(appInfo: AppInfo, isProtected: Boolean) {
        viewModelScope.launch {
            appRepository.toggleAppProtection(appInfo.packageName, isProtected)
            loadApps()
        }
    }
}
