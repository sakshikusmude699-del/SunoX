package com.soundamplifier.ui.animation

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

private const val PressedScale = 0.96f

private val BounceSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

@Composable
private fun rememberAnimatorScaleDisabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

/**
 * Subtle press scale with spring recovery via [graphicsLayer].
 * Pass the same [interactionSource] you pass to Button/clickable.
 */
@Composable
fun Modifier.sunoBounceScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val reduce = rememberAnimatorScaleDisabled()
    val target = if (!enabled || reduce) 1f else if (pressed) PressedScale else 1f
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = if (reduce) tween(0) else BounceSpring,
        label = "suno_bounce_scale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
