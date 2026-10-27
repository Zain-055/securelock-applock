package com.example.applock.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.applock.data.datastore.SecureLockPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val preferences = SecureLockPreferences(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = preferences.settingsFlow.first()
                    if (settings.isProtectionEnabled && settings.isOnboardingCompleted && settings.protectedPackages.isNotEmpty()) {
                        AppLockService.startService(context.applicationContext)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
