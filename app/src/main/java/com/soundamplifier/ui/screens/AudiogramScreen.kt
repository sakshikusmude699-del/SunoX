package com.soundamplifier.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soundamplifier.audio.AudiogramTest
import com.soundamplifier.data.AUDIOGRAM_FREQUENCIES
import com.soundamplifier.viewmodel.AudiogramUiState
import com.soundamplifier.viewmodel.TestState

@Composable
fun AudiogramScreen(
    uiState: AudiogramUiState,
    onStart: () -> Unit,
    onHeard: () -> Unit,
    onCannotHear: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hearing Test",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = uiState.message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Audiogram chart
        if (uiState.testState != TestState.IDLE) {
            AudiogramChart(
                leftThresholds = uiState.leftThresholds,
                rightThresholds = uiState.rightThresholds,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        when (uiState.testState) {
            TestState.IDLE -> {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text("Start Hearing Test")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Use headphones in a quiet environment for accurate results.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TestState.TESTING -> {
                val freq = AudiogramTest.FREQUENCIES[uiState.currentFrequencyIndex]
                Text(
                    text = "Frequency: $freq Hz",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onHeard,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("I Can Hear It")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCannotHear,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Can't Hear It")
                }
            }

            TestState.DONE -> {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Go to Amplifier")
                }
            }
        }
    }
}

@Composable
fun AudiogramChart(
    leftThresholds: List<Float>,
    rightThresholds: List<Float>,
    modifier: Modifier = Modifier
) {
    val leftColor = Color(0xFF1565C0)
    val rightColor = Color(0xFFC62828)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val freqCount = AUDIOGRAM_FREQUENCIES.size
        val maxDb = 120f

        // Draw grid
        drawGrid(w, h, freqCount)

        // Plot left ear (X markers)
        plotThresholds(leftThresholds, leftColor, w, h, freqCount, maxDb)

        // Plot right ear (O markers)
        plotThresholds(rightThresholds, rightColor, w, h, freqCount, maxDb)
    }
}

private fun DrawScope.drawGrid(w: Float, h: Float, freqCount: Int) {
    val gridColor = Color.LightGray
    // Horizontal lines every 20dB
    for (i in 0..6) {
        val y = h * i / 6f
        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
    }
    // Vertical lines per frequency
    for (i in 0 until freqCount) {
        val x = w * i / (freqCount - 1).toFloat()
        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
    }
}

private fun DrawScope.plotThresholds(
    thresholds: List<Float>,
    color: Color,
    w: Float,
    h: Float,
    freqCount: Int,
    maxDb: Float
) {
    val points = thresholds.mapIndexed { i, db ->
        Offset(
            x = w * i / (freqCount - 1).toFloat(),
            y = h * (db / maxDb)
        )
    }

    // Draw connecting lines
    for (i in 0 until points.size - 1) {
        drawLine(color, points[i], points[i + 1], strokeWidth = 2f)
    }

    // Draw dots
    points.forEach { drawCircle(color, radius = 6f, center = it) }
}
