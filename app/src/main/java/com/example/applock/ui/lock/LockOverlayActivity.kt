package com.example.applock.ui.lock

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.example.applock.data.models.AuthType
import com.example.applock.ui.theme.SecureLockTheme

class LockOverlayActivity : FragmentActivity() {

    companion object {
        const val EXTRA_LOCKED_PACKAGE_NAME = "extra_locked_package_name"
    }

    private val viewModel: LockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Security: Prevent screenshots and screen recording of sensitive authentication UI
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val packageName = intent.getStringExtra(EXTRA_LOCKED_PACKAGE_NAME) ?: ""
        if (packageName.isBlank()) {
            finish()
            return
        }

        viewModel.initialize(packageName)

        // Prevent bypass via Back button: redirect to home screen if user exits lock barrier
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        })

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            // Close lock screen once authenticated
            LaunchedEffect(uiState.isUnlocked) {
                if (uiState.isUnlocked) {
                    finish()
                }
            }

            // Auto-trigger biometric prompt if enabled
            LaunchedEffect(uiState.isBiometricAvailable) {
                if (uiState.isBiometricAvailable && !uiState.isUnlocked) {
                    viewModel.authenticateWithBiometric(this@LockOverlayActivity)
                }
            }

            SecureLockTheme {
                when (uiState.authType) {
                    AuthType.PIN -> {
                        PinLockView(
                            title = "${uiState.appLabel} Locked",
                            pinLength = uiState.pinLength,
                            enteredPin = uiState.enteredPin,
                            errorMessage = uiState.errorMessage,
                            lockoutSeconds = uiState.lockoutSecondsRemaining,
                            isBiometricAvailable = uiState.isBiometricAvailable,
                            onPinChange = { viewModel.onPinChanged(it) },
                            onPinSubmitted = { viewModel.submitPin(it) },
                            onBiometricClick = { viewModel.authenticateWithBiometric(this@LockOverlayActivity) }
                        )
                    }
                    AuthType.PATTERN -> {
                        PatternLockView(
                            title = "${uiState.appLabel} Locked",
                            errorMessage = uiState.errorMessage,
                            lockoutSeconds = uiState.lockoutSecondsRemaining,
                            isBiometricAvailable = uiState.isBiometricAvailable,
                            onPatternSubmitted = { viewModel.submitPattern(it) },
                            onBiometricClick = { viewModel.authenticateWithBiometric(this@LockOverlayActivity) }
                        )
                    }
                }
            }
        }
    }
}
