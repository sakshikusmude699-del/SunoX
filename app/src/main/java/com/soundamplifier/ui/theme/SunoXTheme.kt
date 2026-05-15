package com.soundamplifier.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SunoXDarkScheme = darkColorScheme(
    primary = SunoXColors.Primary,
    onPrimary = SunoXColors.OnPrimary,
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color.White,
    secondary = SunoXColors.PrimaryDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5B21B6),
    onSecondaryContainer = Color.White,
    tertiary = SunoXColors.Primary,
    onTertiary = Color.White,
    background = SunoXColors.DeepBackground,
    onBackground = SunoXColors.OnDarkBackground,
    surface = SunoXColors.DeepBackground,
    onSurface = SunoXColors.OnDarkBackground,
    surfaceVariant = Color(0x0DFFFFFF),
    onSurfaceVariant = SunoXColors.OnDarkSurfaceVariantText,
    error = SunoXColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFF5C2020),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color.White.copy(alpha = 0.20f),
    outlineVariant = Color.White.copy(alpha = 0.10f),
    scrim = Color.Black.copy(alpha = 0.50f),
    inverseSurface = Color(0xFFE8E0F5),
    inverseOnSurface = Color(0xFF1A1025),
    inversePrimary = SunoXColors.PrimaryDark,
)

private val SunoXLightScheme = lightColorScheme(
    primary = SunoXColors.Primary,
    onPrimary = SunoXColors.OnPrimary,
    primaryContainer = Color(0xFFE9D5FF),
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = SunoXColors.PrimaryDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDD6FE),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = SunoXColors.PrimaryDark,
    onTertiary = Color.White,
    background = SunoXColors.LightBackground,
    onBackground = SunoXColors.OnLightBackground,
    surface = SunoXColors.LightBackground,
    onSurface = SunoXColors.OnLightBackground,
    surfaceVariant = Color(0x33FFFFFF),
    onSurfaceVariant = SunoXColors.OnLightMuted,
    error = SunoXColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = SunoXColors.Primary.copy(alpha = 0.35f),
    outlineVariant = SunoXColors.Primary.copy(alpha = 0.20f),
    scrim = Color.Black.copy(alpha = 0.40f),
    inverseSurface = SunoXColors.DeepBackground,
    inverseOnSurface = Color.White,
    inversePrimary = SunoXColors.Primary,
)

private val SunoXTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

val LocalSunoXDarkTheme = staticCompositionLocalOf { true }

@Composable
fun SunoXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) SunoXDarkScheme else SunoXLightScheme
    CompositionLocalProvider(LocalSunoXDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = scheme,
            typography = SunoXTypography,
            content = content,
        )
    }
}
