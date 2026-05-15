package com.soundamplifier.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soundamplifier.ui.theme.SunoXColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevationShadow: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val fill = if (isLight) SunoXColors.GlassFillLight else SunoXColors.GlassFillDark
    val borderC = if (isLight) SunoXColors.GlassBorderLight else SunoXColors.GlassBorderDark
    val ambient = Color.Black.copy(alpha = if (isLight) 0.12f else 0.22f)

    Box(
        modifier = modifier
            .shadow(
                elevation = elevationShadow,
                shape = shape,
                clip = false,
                ambientColor = ambient,
                spotColor = ambient,
            )
            .clip(shape)
            .background(fill)
            .border(1.dp, borderC, shape),
        content = content,
    )
}
