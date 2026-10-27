package com.example.applock.monitoring

import com.example.applock.data.models.ReLockPolicy
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages active unlock sessions and re-lock timeouts per protected application package.
 */
object LockStateManager {

    // Maps package name -> timestamp (System.currentTimeMillis()) when user left the app
    private val appLeftTimestamps = ConcurrentHashMap<String, Long>()

    // Maps package name -> Boolean indicating whether package is currently in unlocked session
    private val unlockedPackages = ConcurrentHashMap<String, Boolean>()

    // Track last seen foreground package
    private var lastForegroundPackage: String? = null

    /**
     * Call when user successfully authenticates for a package.
     */
    fun onAppUnlocked(packageName: String) {
        unlockedPackages[packageName] = true
        appLeftTimestamps.remove(packageName)
    }

    /**
     * Call when foreground detection discovers a new foreground package.
     */
    fun onForegroundPackageChanged(newPackage: String, reLockPolicy: ReLockPolicy) {
        val previousPkg = lastForegroundPackage
        if (previousPkg != null && previousPkg != newPackage) {
            // User left previousPkg
            appLeftTimestamps[previousPkg] = System.currentTimeMillis()

            if (reLockPolicy == ReLockPolicy.AFTER_LEAVING || reLockPolicy == ReLockPolicy.IMMEDIATELY) {
                // Instantly invalidate unlock session when leaving app
                unlockedPackages.remove(previousPkg)
            }
        }
        lastForegroundPackage = newPackage
    }

    /**
     * Evaluates whether [packageName] needs to present the lock screen.
     */
    fun shouldLock(packageName: String, reLockPolicy: ReLockPolicy): Boolean {
        if (!unlockedPackages.containsKey(packageName)) {
            return true
        }

        return when (reLockPolicy) {
            ReLockPolicy.IMMEDIATELY -> {
                // Under IMMEDIATELY policy, user must authenticate every time foreground is re-detected
                // unless it's currently the same continuous foreground session
                if (lastForegroundPackage != packageName) {
                    unlockedPackages.remove(packageName)
                    true
                } else {
                    false
                }
            }
            ReLockPolicy.AFTER_LEAVING -> {
                // If unlocked session was invalidated on leave, shouldLock returns true
                !unlockedPackages.getOrDefault(packageName, false)
            }
            ReLockPolicy.SECONDS_30, ReLockPolicy.MINUTE_1 -> {
                val leftTimestamp = appLeftTimestamps[packageName]
                if (leftTimestamp != null) {
                    val timeSinceLeft = System.currentTimeMillis() - leftTimestamp
                    if (timeSinceLeft > reLockPolicy.timeoutMillis) {
                        unlockedPackages.remove(packageName)
                        appLeftTimestamps.remove(packageName)
                        return true
                    }
                }
                false
            }
        }
    }

    /**
     * Clears all session states (e.g., when screen turns off or protection is disabled).
     */
    fun resetSessions() {
        unlockedPackages.clear()
        appLeftTimestamps.clear()
        lastForegroundPackage = null
    }
}
