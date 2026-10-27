package com.example.applock.ui.lock

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applock.ui.theme.ErrorRed

@Composable
fun PinLockView(
    title: String,
    subtitle: String = "Enter PIN to continue",
    pinLength: Int = 4,
    enteredPin: String,
    errorMessage: String = "",
    lockoutSeconds: Int = 0,
    isBiometricAvailable: Boolean = false,
    onPinChange: (String) -> Unit,
    onPinSubmitted: (String) -> Unit,
    onBiometricClick: () -> Unit = {}
) {
    val context = LocalContext.current

    fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(30)
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (lockoutSeconds > 0) "Too many attempts. Try again in $lockoutSeconds s" else subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (lockoutSeconds > 0 || errorMessage.isNotEmpty()) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (errorMessage.isNotEmpty() && lockoutSeconds == 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = ErrorRed,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // PIN Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                val targetCount = maxOf(pinLength, enteredPin.length)
                for (i in 0 until targetCount) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .border(
                                width = 2.dp,
                                color = if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    )
                }
            }

            // Numeric Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                val isKeypadEnabled = lockoutSeconds == 0

                val keyRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                for (row in keyRows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (key in row) {
                            KeypadButton(
                                text = key,
                                enabled = isKeypadEnabled,
                                isBiometricAvailable = isBiometricAvailable,
                                onClick = {
                                    triggerHaptic()
                                    when (key) {
                                        "DEL" -> {
                                            if (enteredPin.isNotEmpty()) {
                                                onPinChange(enteredPin.dropLast(1))
                                            }
                                        }
                                        "BIO" -> {
                                            onBiometricClick()
                                        }
                                        else -> {
                                            if (enteredPin.length < pinLength) {
                                                val newPin = enteredPin + key
                                                onPinChange(newPin)
                                                if (newPin.length == pinLength) {
                                                    onPinSubmitted(newPin)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    enabled: Boolean,
    isBiometricAvailable: Boolean,
    onClick: () -> Unit
) {
    if (text == "BIO" && !isBiometricAvailable) {
        Box(modifier = Modifier.size(72.dp))
        return
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (text) {
            "DEL" -> {
                Text(
                    text = "⌫",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            }
            "BIO" -> {
                Text(
                    text = "👆",
                    fontSize = 24.sp
                )
            }
            else -> {
                Text(
                    text = text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            }
        }
    }
}
