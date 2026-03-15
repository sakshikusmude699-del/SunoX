package com.soundamplifier.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onStartAmplifier: () -> Unit,
    onStartAudiogramTest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SmartHear+",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Personalized hearing enhancement",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onStartAmplifier,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Amplifier")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onStartAudiogramTest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Take Hearing Test (Audiogram)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = "Tip: Take the hearing test first to auto-configure your amplifier settings.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
