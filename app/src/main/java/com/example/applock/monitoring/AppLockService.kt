package com.example.applock.monitoring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.applock.MainActivity
import com.example.applock.R
import com.example.applock.data.datastore.SecureLockPreferences
import com.example.applock.ui.lock.LockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppLockService : Service() {

    companion object {
        const val CHANNEL_ID = "secure_lock_protection_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SERVICE = "com.example.applock.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.applock.action.STOP_SERVICE"

        private val UNINSTALL_PREVENTION_PACKAGES = setOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller"
        )

        private val SYSTEM_EXCLUDED_PACKAGES = setOf(
            "com.samsung.knox.securefolder",
            "com.sec.android.app.secprotect",
            "com.samsung.android.knox.containercore",
            "com.samsung.knox.kss",
            "com.sec.knox.app.container",
            "com.android.systemui",
            "com.sec.android.app.launcher"
        )

        fun isExcludedSystemContainer(packageName: String): Boolean {
            if (packageName.isBlank()) return true
            if (SYSTEM_EXCLUDED_PACKAGES.contains(packageName)) return true
            val lower = packageName.lowercase()
            return lower.contains("knox") || lower.contains("securefolder") || lower.contains("secprotect")
        }

        fun startService(context: Context) {
            val intent = Intent(context, AppLockService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AppLockService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private lateinit var preferences: SecureLockPreferences
    private lateinit var usageStatsManager: UsageStatsManager

    private var isScreenOn = true
    private var monitoringJob: Job? = null
    private var lastLockLaunchTimestamp: Long = 0L
    private var lastLockLaunchPackage: String? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    LockStateManager.resetSessions()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = SecureLockPreferences(applicationContext)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoringLoop()

        return START_STICKY
    }

    private fun startMonitoringLoop() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            var previousForegroundPkg = ""

            while (isActive) {
                if (isScreenOn) {
                    try {
                        val settings = preferences.settingsFlow.first()
                        if (settings.isProtectionEnabled) {
                            val activeProtectedPackages = if (settings.isUninstallPreventionEnabled) {
                                settings.protectedPackages + UNINSTALL_PREVENTION_PACKAGES
                            } else {
                                settings.protectedPackages
                            }

                            if (activeProtectedPackages.isNotEmpty()) {
                                val currentForegroundPkg = getForegroundPackageName()

                                // Exclude self, system UI, and Samsung Knox / Secure Folder containers
                                if (currentForegroundPkg.isNotBlank() &&
                                    currentForegroundPkg != applicationContext.packageName &&
                                    !isExcludedSystemContainer(currentForegroundPkg)
                                ) {
                                    if (currentForegroundPkg != previousForegroundPkg) {
                                        LockStateManager.onForegroundPackageChanged(
                                            currentForegroundPkg,
                                            settings.reLockPolicy
                                        )
                                        previousForegroundPkg = currentForegroundPkg
                                    }

                                    if (activeProtectedPackages.contains(currentForegroundPkg)) {
                                        val shouldLock = LockStateManager.shouldLock(
                                            currentForegroundPkg,
                                            settings.reLockPolicy
                                        )
                                        if (shouldLock) {
                                            launchLockScreen(currentForegroundPkg)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay(500) // Poll interval 500ms
            }
        }
    }

    private fun getForegroundPackageName(): String {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10_000
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime) ?: return ""

        val event = UsageEvents.Event()
        var lastResumedPkg = ""

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastResumedPkg = event.packageName
            }
        }
        return lastResumedPkg
    }

    private fun launchLockScreen(packageName: String) {
        val now = SystemClock.elapsedRealtime()
        if (packageName == lastLockLaunchPackage && now - lastLockLaunchTimestamp < 2_000L) {
            return
        }

        lastLockLaunchPackage = packageName
        lastLockLaunchTimestamp = now

        val intent = Intent(applicationContext, LockOverlayActivity::class.java).apply {
            putExtra(LockOverlayActivity.EXTRA_LOCKED_PACKAGE_NAME, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SecureLock Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors background app launches to protect selected applications."
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SecureLock Active")
            .setContentText("Protected applications are monitored")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
