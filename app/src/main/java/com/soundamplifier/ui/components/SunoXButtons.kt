package com.soundamplifier.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soundamplifier.ui.theme.SunoXColors

@Composable
fun SunoXPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        label = "suno_primary_press",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                clip = false,
                ambientColor = SunoXColors.ShadowGlow,
                spotColor = SunoXColors.ShadowGlow,
            )
            .background(
                brush = Brush.linearGradient(
                    colors = SunoXColors.GradientPrimary,
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Color.White.copy(alpha = 0.5f),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            interactionSource = interaction,
        ) {
            Text(text, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
fun SunoXSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        label = "suno_secondary_press",
    )
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .background(Color.White.copy(alpha = 0.08f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.4f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        interactionSource = interaction,
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SunoXSecondaryButtonLightAware(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    if (isDark) {
        SunoXSecondaryButton(text, onClick, modifier, enabled, height)
    } else {
        val shape = RoundedCornerShape(16.dp)
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(targetValue = if (pressed) 0.98f else 1f, label = "sec_light")
        OutlinedButtonGlassLight(
            text = text,
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .scale(scale),
            shape = shape,
            interactionSource = interaction,
            enabled = enabled,
        )
    }
}

@Composable
private fun OutlinedButtonGlassLight(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    shape: RoundedCornerShape,
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .background(SunoXColors.GlassFillLight, shape)
            .border(1.dp, SunoXColors.Primary.copy(alpha = 0.35f), shape),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = SunoXColors.PrimaryDark,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource,
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SunoXDestructiveTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = SunoXColors.ErrorText,
        ),
    ) {
        Text(text, color = SunoXColors.ErrorText)
    }
}
