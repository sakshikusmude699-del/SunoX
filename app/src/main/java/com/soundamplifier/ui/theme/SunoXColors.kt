package com.soundamplifier.ui.theme

import androidx.compose.ui.graphics.Color

object SunoXColors {
    val DeepBackground = Color(0xFF0A0A0F)
    val LightBackground = Color(0xFFF5F3FF)

    /** Softer lavender–violet than the prior fuchsia-500 pair; still enough contrast for white labels. */
    val Primary = Color(0xFFB8A2F5)
    val PrimaryDark = Color(0xFF9B7EE8)

    val OnPrimary = Color.White
    val OnDarkBackground = Color.White
    val OnDarkSurfaceVariantText = Color(0xB3FFFFFF)

    val GlassFillDark = Color.White.copy(alpha = 0.05f)
    val GlassBorderDark = Color.White.copy(alpha = 0.10f)
    val GlassFillLight = Color.White.copy(alpha = 0.60f)
    val GlassBorderLight = Color.White.copy(alpha = 0.30f)

    val Error = Color(0xFFFF6464)
    val ErrorText = Color(0xFFFF6464).copy(alpha = 0.8f)

    val GradientPrimary = listOf(Primary, PrimaryDark)
    val ShadowGlow = Primary.copy(alpha = 0.30f)

    val OnLightBackground = Color(0xFF1A1025)
    val OnLightMuted = Color(0xFF5A5270)

    val AmbientBlob1Dark = Primary.copy(alpha = 0.08f)
    val AmbientBlob2Dark = PrimaryDark.copy(alpha = 0.06f)
    val AmbientBlob1Light = Primary.copy(alpha = 0.12f)
    val AmbientBlob2Light = PrimaryDark.copy(alpha = 0.10f)
}
