package com.example.applock.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applock.ui.lock.PatternLockView
import com.example.applock.ui.lock.PinLockView
import com.example.applock.ui.theme.ErrorRed

@Composable
fun PatternSetupView(
    onPatternCreated: (String) -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: First draw, 2: Confirm draw
    var firstPattern by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var enteredPattern by remember { mutableStateOf("") }

    val subtitle = when {
        errorMessage.isNotEmpty() -> errorMessage
        step == 1 -> "Draw a new pattern (connect at least 4 dots)"
        else -> "Draw the same pattern again to confirm"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            PatternLockView(
                title = if (step == 1) "Create Pattern" else "Confirm Pattern",
                subtitle = subtitle,
                errorMessage = if (errorMessage.isNotEmpty()) errorMessage else "",
                onPatternSubmitted = { pattern ->
                    if (pattern.split("-").size < 4) {
                        errorMessage = "Connect at least 4 dots"
                        return@PatternLockView
                    }

                    if (step == 1) {
                        firstPattern = pattern
                        step = 2
                        errorMessage = ""
                    } else {
                        if (pattern == firstPattern) {
                            onPatternCreated(pattern)
                        } else {
                            errorMessage = "Patterns do not match. Start again."
                            step = 1
                            firstPattern = ""
                        }
                    }
                }
            )

            TextButton(
                onClick = onCancel,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("Cancel", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun PinSetupView(
    onPinCreated: (String) -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: First PIN, 2: Confirm PIN
    var firstPin by remember { mutableStateOf("") }
    var currentEnteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val subtitle = when {
        errorMessage.isNotEmpty() -> errorMessage
        step == 1 -> "Enter a 4 to 6 digit PIN"
        else -> "Re-enter the PIN to confirm"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            PinLockView(
                title = if (step == 1) "Create PIN" else "Confirm PIN",
                subtitle = subtitle,
                pinLength = if (step == 1) 6 else firstPin.length,
                enteredPin = currentEnteredPin,
                errorMessage = errorMessage,
                onPinChange = { currentEnteredPin = it },
                onPinSubmitted = { pin ->
                    if (pin.length < 4) {
                        errorMessage = "PIN must be at least 4 digits"
                        return@PinLockView
                    }

                    if (step == 1) {
                        firstPin = pin
                        step = 2
                        currentEnteredPin = ""
                        errorMessage = ""
                    } else {
                        if (pin == firstPin) {
                            onPinCreated(pin)
                        } else {
                            errorMessage = "PINs do not match. Start again."
                            step = 1
                            firstPin = ""
                            currentEnteredPin = ""
                        }
                    }
                }
            )

            TextButton(
                onClick = onCancel,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("Cancel", fontSize = 16.sp)
            }
        }
    }
}
