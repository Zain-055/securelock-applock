package com.example.applock.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.applock.data.models.AuthType
import com.example.applock.ui.components.PatternSetupView
import com.example.applock.ui.components.PinSetupView
import com.example.applock.ui.theme.ErrorRed

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh permission status when returning to screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (uiState.currentStep) {
            1 -> WelcomeStep(onNext = { viewModel.nextStep() })
            2 -> ChooseAuthMethodStep(
                selectedType = uiState.selectedAuthType,
                onSelect = { viewModel.selectAuthType(it) },
                onNext = { viewModel.nextStep() }
            )
            3 -> {
                if (uiState.selectedAuthType == AuthType.PATTERN) {
                    PatternSetupView(
                        onPatternCreated = { pattern ->
                            viewModel.createCredential(pattern, pattern)
                        },
                        onCancel = { viewModel.previousStep() }
                    )
                } else {
                    PinSetupView(
                        onPinCreated = { pin ->
                            viewModel.createCredential(pin, pin)
                        },
                        onCancel = { viewModel.previousStep() }
                    )
                }
            }
            4 -> BiometricOptInStep(
                isAvailable = uiState.isBiometricAvailable,
                onChoice = { enable -> viewModel.setBiometricChoice(enable) }
            )
            5 -> PermissionStep(
                hasUsageAccess = uiState.hasUsageAccessPermission,
                hasOverlay = uiState.hasOverlayPermission,
                onGrantUsageAccess = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                onGrantOverlay = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) },
                onNext = { viewModel.nextStep() }
            )
            6 -> SetupCompleteStep(
                onFinish = { viewModel.completeOnboarding(onOnboardingComplete) }
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 64.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🔒", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "SECURELOCK",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Protect your private applications with PIN, Pattern, or Biometric authentication.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(52.dp)
        ) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChooseAuthMethodStep(
    selectedType: AuthType,
    onSelect: (AuthType) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Text(
                text = "Choose Security Method",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select primary credential type for locking apps",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(AuthType.PIN) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedType == AuthType.PIN) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == AuthType.PIN,
                        onClick = { onSelect(AuthType.PIN) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("PIN", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("4 to 6 digit numerical PIN", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(AuthType.PATTERN) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedType == AuthType.PATTERN) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == AuthType.PATTERN,
                        onClick = { onSelect(AuthType.PATTERN) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Pattern", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Standard 3x3 touch grid pattern", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(52.dp)
        ) {
            Text("Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BiometricOptInStep(
    isAvailable: Boolean,
    onChoice: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("👆", fontSize = 40.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enable Biometric?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isAvailable) "Use your fingerprint/biometric for faster unlocking."
                else "Biometric hardware is not currently available or configured on this device. You can skip this step.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            if (isAvailable) {
                Button(
                    onClick = { onChoice(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Enable Biometric", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { onChoice(false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Skip", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun PermissionStep(
    hasUsageAccess: Boolean,
    hasOverlay: Boolean,
    onGrantUsageAccess: () -> Unit,
    onGrantOverlay: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(
                text = "Required Permission",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SecureLock uses Usage Access to determine when one of your protected applications becomes active. Your usage information is processed locally.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Usage Access", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (hasUsageAccess) "Granted ✓" else "Not Granted",
                                color = if (hasUsageAccess) MaterialTheme.colorScheme.primary else ErrorRed,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (!hasUsageAccess) {
                            Button(onClick = onGrantUsageAccess) {
                                Text("Grant Access")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Display Over Apps", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (hasOverlay) "Granted ✓" else "Not Granted",
                                color = if (hasOverlay) MaterialTheme.colorScheme.primary else ErrorRed,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (!hasOverlay) {
                            Button(onClick = onGrantOverlay) {
                                Text("Grant Access")
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(52.dp)
        ) {
            Text(
                text = if (hasUsageAccess) "Continue" else "Continue Without Protection",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SetupCompleteStep(onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 64.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🎉", fontSize = 44.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Setup Complete",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SecureLock is ready to protect your private applications.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(52.dp)
        ) {
            Text("Select Apps", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
