package com.soundamplifier.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundamplifier.R

private val LoginBorderLight = Color(0xFFE1BEE7)
private val LoginButtonLabelPurple = Color(0xFF9575CD)
@Composable
fun GoogleLoginScreen(
    onGoogleSignIn: () -> Unit,
    isDarkMode: Boolean = false,
    onThemeToggle: () -> Unit = {},
) {
    val unbounded = FontFamily(
        Font(
            googleFont = GoogleFont("Unbounded"),
            fontProvider = GoogleFont.Provider(
                providerAuthority = "com.google.android.gms.fonts",
                providerPackage = "com.google.android.gms",
                certificates = R.array.com_google_android_gms_fonts_certs,
            ),
            weight = FontWeight.Bold,
        ),
    )

    val titleColor = if (isDarkMode) {
        Color.White
    } else {
        Color(0xFF212121)
    }
    val taglineColor = if (isDarkMode) {
        Color.White.copy(alpha = 0.7f)
    } else {
        Color(0xFF757575)
    }
    val termsColor = if (isDarkMode) {
        Color.White.copy(alpha = 0.65f)
    } else {
        Color(0xFF9E9E9E)
    }

    val logoCircleBg = if (isDarkMode) {
        com.soundamplifier.ui.theme.SunoXColors.GlassFillDark
    } else {
        Color.White
    }
    val buttonBg = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val buttonBorder = if (isDarkMode) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    } else {
        LoginBorderLight
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = if (isDarkMode) {
                            stringResource(R.string.theme_light_mode)
                        } else {
                            stringResource(R.string.theme_dark_mode)
                        },
                        tint = if (isDarkMode) Color.White.copy(alpha = 0.8f) else titleColor,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(logoCircleBg)
                        .border(
                            2.dp,
                            com.soundamplifier.ui.theme.SunoXColors.Primary.copy(alpha = 0.4f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.main_icon),
                        contentDescription = stringResource(R.string.cd_app_logo),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = unbounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                    ),
                    color = titleColor,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.home_tagline_under_name),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Normal,
                    ),
                    color = taglineColor,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(36.dp))

                OutlinedButton(
                    onClick = onGoogleSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.dp, buttonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = buttonBg,
                        contentColor = LoginButtonLabelPurple,
                    ),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_google),
                        contentDescription = stringResource(R.string.cd_google_logo),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.welcome_login_google),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = LoginButtonLabelPurple,
                    )
                }
            }

            Text(
                text = stringResource(R.string.welcome_terms),
                style = MaterialTheme.typography.bodySmall,
                color = termsColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp, start = 8.dp, end = 8.dp),
            )
        }
    }
}
