package com.example.applock.ui.lock

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applock.ui.theme.ErrorRed
import com.example.applock.ui.theme.SuccessGreen
import kotlin.math.hypot

@Composable
fun PatternLockView(
    title: String,
    subtitle: String = "Draw pattern to unlock",
    errorMessage: String = "",
    lockoutSeconds: Int = 0,
    isBiometricAvailable: Boolean = false,
    onPatternSubmitted: (String) -> Unit,
    onBiometricClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentTouchPosition by remember { mutableStateOf<Offset?>(null) }
    var isErrorState by remember { mutableStateOf(false) }

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
                    color = if (lockoutSeconds > 0 || errorMessage.isNotEmpty() || isErrorState) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
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

            // Interactive 3x3 Pattern Canvas
            val primaryColor = MaterialTheme.colorScheme.primary
            val lineColor = when {
                isErrorState -> ErrorRed
                else -> primaryColor
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(lockoutSeconds) {
                            if (lockoutSeconds > 0) return@pointerInput

                            detectDragGestures(
                                onDragStart = { offset ->
                                    selectedNodes.clear()
                                    isErrorState = false
                                    currentTouchPosition = offset
                                    val node = findHitNode(offset, size.width.toFloat(), size.height.toFloat())
                                    if (node != null) {
                                        selectedNodes.add(node)
                                        triggerHaptic()
                                    }
                                },
                                onDrag = { change, _ ->
                                    currentTouchPosition = change.position
                                    val node = findHitNode(change.position, size.width.toFloat(), size.height.toFloat())
                                    if (node != null && !selectedNodes.contains(node)) {
                                        selectedNodes.add(node)
                                        triggerHaptic()
                                    }
                                },
                                onDragEnd = {
                                    currentTouchPosition = null
                                    if (selectedNodes.isNotEmpty()) {
                                        val patternString = selectedNodes.joinToString("-")
                                        onPatternSubmitted(patternString)
                                    }
                                },
                                onDragCancel = {
                                    currentTouchPosition = null
                                    selectedNodes.clear()
                                }
                            )
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val colStep = width / 3f
                    val rowStep = height / 3f

                    // Calculate center offsets for all 9 nodes (0 to 8)
                    val nodeOffsets = List(9) { index ->
                        val row = index / 3
                        val col = index % 3
                        Offset(
                            x = colStep * col + colStep / 2f,
                            y = rowStep * row + rowStep / 2f
                        )
                    }

                    // Draw connecting lines between selected nodes
                    for (i in 0 until selectedNodes.size - 1) {
                        val start = nodeOffsets[selectedNodes[i]]
                        val end = nodeOffsets[selectedNodes[i + 1]]
                        drawLine(
                            color = lineColor,
                            start = start,
                            end = end,
                            strokeWidth = 12.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw line from last selected node to current touch position
                    if (selectedNodes.isNotEmpty() && currentTouchPosition != null) {
                        val start = nodeOffsets[selectedNodes.last()]
                        drawLine(
                            color = lineColor.copy(alpha = 0.6f),
                            start = start,
                            end = currentTouchPosition!!,
                            strokeWidth = 10.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw 9 nodes
                    val outerRadius = 24.dp.toPx()
                    val innerRadius = 8.dp.toPx()

                    for (i in 0..8) {
                        val center = nodeOffsets[i]
                        val isSelected = selectedNodes.contains(i)

                        val nodeColor = when {
                            isSelected && isErrorState -> ErrorRed
                            isSelected -> primaryColor
                            else -> Color.Gray.copy(alpha = 0.4f)
                        }

                        // Outer ring if selected
                        if (isSelected) {
                            drawCircle(
                                color = nodeColor.copy(alpha = 0.25f),
                                radius = outerRadius * 1.4f,
                                center = center
                            )
                        }

                        // Node circle
                        drawCircle(
                            color = nodeColor,
                            radius = if (isSelected) outerRadius else innerRadius,
                            center = center
                        )
                    }
                }
            }

            // Bottom Actions (Biometric or Clear)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                if (isBiometricAvailable) {
                    Button(
                        onClick = onBiometricClick,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text("👆 Use Biometric")
                    }
                } else if (selectedNodes.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            selectedNodes.clear()
                            currentTouchPosition = null
                        }
                    ) {
                        Text("Clear Pattern")
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

private fun findHitNode(point: Offset, totalWidth: Float, totalHeight: Float): Int? {
    val colStep = totalWidth / 3f
    val rowStep = totalHeight / 3f
    val touchRadius = colStep * 0.38f

    for (i in 0..8) {
        val row = i / 3
        val col = i % 3
        val nodeX = colStep * col + colStep / 2f
        val nodeY = rowStep * row + rowStep / 2f

        val distance = hypot(point.x - nodeX, point.y - nodeY)
        if (distance <= touchRadius) {
            return i
        }
    }
    return null
}
