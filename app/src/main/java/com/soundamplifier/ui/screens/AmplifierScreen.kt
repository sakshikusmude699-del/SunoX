package com.soundamplifier.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soundamplifier.viewmodel.AmplifierUiState
import kotlin.math.roundToInt

@Composable
fun AmplifierScreen(
    uiState: AmplifierUiState,
    onToggle: () -> Unit,
    onNoiseReductionChange: (Int) -> Unit,
    onMasterGainChange: (Float) -> Unit,
    onLowBoostChange: (Float) -> Unit,
    onHighBoostChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SmartHear+", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        // Profile info
        uiState.activeProfile?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Profile: ${it.label}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Power toggle
        Button(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isRunning)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (uiState.isRunning) "Stop Amplifier" else "Start Amplifier")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Noise Reduction
        SliderControl(
            label = "Noise Reduction",
            value = uiState.noiseReduction.toFloat(),
            valueRange = 0f..100f,
            displayValue = "${uiState.noiseReduction}%",
            onValueChange = { onNoiseReductionChange(it.roundToInt()) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Master Gain (boost quiet sounds)
        SliderControl(
            label = "Boost Quiet Sounds",
            value = uiState.masterGain,
            valueRange = 1f..5f,
            displayValue = "x${"%.1f".format(uiState.masterGain)}",
            onValueChange = onMasterGainChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Low Frequency Boost
        SliderControl(
            label = "Low Frequency Boost",
            value = uiState.lowBoost,
            valueRange = -12f..12f,
            displayValue = "${"%.0f".format(uiState.lowBoost)} dB",
            onValueChange = onLowBoostChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // High Frequency Boost
        SliderControl(
            label = "High Frequency Boost",
            value = uiState.highBoost,
            valueRange = -12f..12f,
            displayValue = "${"%.0f".format(uiState.highBoost)} dB",
            onValueChange = onHighBoostChange
        )
    }
}

@Composable
private fun SliderControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(displayValue, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
