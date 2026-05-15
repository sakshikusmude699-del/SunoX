package com.soundamplifier.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.soundamplifier.ui.theme.SunoXColors

@Composable
fun SunoXAmbientBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val base = if (isDark) SunoXColors.DeepBackground else SunoXColors.LightBackground
    val blob1 = if (isDark) SunoXColors.AmbientBlob1Dark else SunoXColors.AmbientBlob1Light
    val blob2 = if (isDark) SunoXColors.AmbientBlob2Dark else SunoXColors.AmbientBlob2Light

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(base),
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        if (w <= 0f || h <= 0f) return@BoxWithConstraints
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob1, Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.12f),
                    radius = w * 0.55f,
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.85f, h * 0.12f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob2, Color.Transparent),
                    center = Offset(w * 0.12f, h * 0.88f),
                    radius = w * 0.5f,
                ),
                radius = w * 0.5f,
                center = Offset(w * 0.12f, h * 0.88f),
            )
        }
    }
}
