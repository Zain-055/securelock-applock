package com.example.applock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.applock.data.datastore.SecureLockPreferences
import com.example.applock.monitoring.LockStateManager
import com.example.applock.ui.apps.AppSelectionScreen
import com.example.applock.ui.home.DashboardScreen
import com.example.applock.ui.lock.LockOverlayActivity
import com.example.applock.ui.onboarding.OnboardingScreen
import com.example.applock.ui.settings.SettingsScreen
import com.example.applock.ui.theme.SecureLockTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferences: SecureLockPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = SecureLockPreferences(applicationContext)

        setContent {
            val settingsState by preferences.settingsFlow.collectAsState(initial = null)

            if (settingsState != null) {
                val settings = settingsState!!
                SecureLockTheme(themePreference = settings.themePreference) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(isOnboardingCompleted = settings.isOnboardingCompleted)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkSelfLock()
    }

    override fun onStop() {
        super.onStop()
        // Invalidate self-unlock session whenever user backgrounds or leaves SecureLock
        LockStateManager.resetSessions()
    }

    private fun checkSelfLock() {
        lifecycleScope.launch {
            try {
                val settings = preferences.settingsFlow.first()
                if (settings.isOnboardingCompleted) {
                    val selfPkg = packageName
                    val shouldLockSelf = LockStateManager.shouldLock(selfPkg, settings.reLockPolicy)
                    if (shouldLockSelf) {
                        val lockIntent = Intent(applicationContext, LockOverlayActivity::class.java).apply {
                            putExtra(LockOverlayActivity.EXTRA_LOCKED_PACKAGE_NAME, selfPkg)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(lockIntent)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun AppNavigation(isOnboardingCompleted: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (isOnboardingCompleted) "dashboard" else "onboarding"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate("select_apps") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                onNavigateToSelectApps = { navController.navigate("select_apps") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("select_apps") {
            AppSelectionScreen(
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
