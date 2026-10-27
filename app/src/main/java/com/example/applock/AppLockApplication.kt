package com.example.applock

import android.app.Application
import com.example.applock.data.datastore.SecureLockPreferences
import com.example.applock.monitoring.AppLockService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppLockApplication : Application() {

    lateinit var preferences: SecureLockPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = SecureLockPreferences(this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = preferences.settingsFlow.first()
                if (settings.isProtectionEnabled && settings.isOnboardingCompleted && settings.protectedPackages.isNotEmpty()) {
                    AppLockService.startService(this@AppLockApplication)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
