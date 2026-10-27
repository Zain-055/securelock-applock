package com.example.applock.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.example.applock.data.datastore.SecureLockPreferences
import com.example.applock.data.models.AppInfo
import com.example.applock.monitoring.AppLockService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val preferences: SecureLockPreferences
) {

    suspend fun getInstalledApplications(filterQuery: String = ""): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val selfPackage = context.packageName

        // Intent to find launchable apps
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val launchableActivities = packageManager.queryIntentActivities(mainIntent, 0)
        val protectedPackages = preferences.settingsFlow.first().protectedPackages

        val appList = mutableListOf<AppInfo>()
        val foundPackages = mutableSetOf<String>()

        for (resolveInfo in launchableActivities) {
            val packageName = resolveInfo.activityInfo.packageName

            // Skip self, Knox/Secure Folder containers, and already added packages
            if (packageName == selfPackage ||
                AppLockService.isExcludedSystemContainer(packageName) ||
                foundPackages.contains(packageName)
            ) {
                continue
            }
            foundPackages.add(packageName)

            val appLabel = resolveInfo.loadLabel(packageManager).toString()
            if (filterQuery.isNotBlank() && !appLabel.contains(filterQuery, ignoreCase = true) && !packageName.contains(filterQuery, ignoreCase = true)) {
                continue
            }

            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val iconDrawable = try {
                resolveInfo.loadIcon(packageManager)
            } catch (e: Exception) {
                packageManager.defaultActivityIcon
            }

            appList.add(
                AppInfo(
                    label = appLabel,
                    packageName = packageName,
                    icon = iconDrawable,
                    isProtected = protectedPackages.contains(packageName),
                    isSystemApp = isSystem
                )
            )
        }

        // Clean up uninstalled apps from protected packages list
        val stalePackages = protectedPackages.filter { !foundPackages.contains(it) && !AppLockService.isExcludedSystemContainer(it) }
        if (stalePackages.isNotEmpty()) {
            val cleanedSet = protectedPackages.filter { foundPackages.contains(it) }.toSet()
            preferences.setProtectedPackages(cleanedSet)
        }

        // Sort alphabetically by app label
        appList.sortedBy { it.label.lowercase() }
    }

    suspend fun toggleAppProtection(packageName: String, isProtected: Boolean) {
        preferences.togglePackageProtected(packageName, isProtected)
    }
}
