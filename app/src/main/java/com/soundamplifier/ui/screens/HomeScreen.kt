package com.soundamplifier.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.soundamplifier.R
import com.soundamplifier.data.AccountLocalIds
import com.soundamplifier.data.FirestoreUserRepository
import com.soundamplifier.data.FirstLoginHomePreferences
import com.soundamplifier.data.HearingTestPreferences
import com.soundamplifier.ui.components.GlassCard
import com.soundamplifier.ui.components.SunoXDestructiveTextButton
import com.soundamplifier.ui.components.SunoXPrimaryButton
import com.soundamplifier.ui.components.SunoXSecondaryButtonLightAware
import androidx.compose.ui.platform.LocalContext
import com.soundamplifier.ui.theme.SunoXColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    onOpenNavigationDrawer: () -> Unit,
    onStartAudiogramTest: () -> Unit,
    onOpenAmplifier: () -> Unit = {},
    onLogout: () -> Unit = {},
    isDarkMode: Boolean = false,
    onThemeToggle: () -> Unit = {},
    firestoreUserRepository: FirestoreUserRepository = FirestoreUserRepository(),
    homeFirstLoginGateRevision: Int = 0,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    var firebaseUser by remember { mutableStateOf(firebaseAuth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseUser = it.currentUser }
        firebaseAuth.addAuthStateListener(listener)
        onDispose { firebaseAuth.removeAuthStateListener(listener) }
    }
    val accountId = firebaseUser?.uid ?: AccountLocalIds.GUEST

    var prefsEpoch by remember { mutableIntStateOf(0) }
    var skippedThisSession by remember { mutableStateOf(false) }

    val hearingCompleted = remember(accountId, prefsEpoch) {
        HearingTestPreferences.isCompleted(context, accountId)
    }
    val hearingDismissedForever = remember(accountId, prefsEpoch) {
        HearingTestPreferences.isDismissedForever(context, accountId)
    }
    val showHearingPrompt = !hearingCompleted &&
        !hearingDismissedForever &&
        !skippedThisSession

    var showHomeOpenAmplifier by remember(accountId, homeFirstLoginGateRevision) {
        mutableStateOf(!FirstLoginHomePreferences.isPending(context, accountId))
    }
    LaunchedEffect(accountId, homeFirstLoginGateRevision) {
        if (accountId == AccountLocalIds.GUEST) {
            showHomeOpenAmplifier = true
            return@LaunchedEffect
        }
        val pending = FirstLoginHomePreferences.isPending(context, accountId)
        if (!pending) {
            showHomeOpenAmplifier = true
            return@LaunchedEffect
        }
        val has = withContext(Dispatchers.IO) {
            firestoreUserRepository.hasAnyAudiogram(accountId)
        }
        if (has) {
            FirstLoginHomePreferences.clearPending(context, accountId)
        }
        showHomeOpenAmplifier = has
    }

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

    val barIconTint = if (isDarkMode) Color.White.copy(alpha = 0.8f) else SunoXColors.OnLightBackground
    val titleColor = if (isDarkMode) Color.White else SunoXColors.OnLightBackground
    val taglineColor = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color(0xFF5A5270)
    val promptTextColor = titleColor
    val linkMuted = if (isDarkMode) Color.White.copy(alpha = 0.65f) else Color(0xFF6E6688)

    val infinite = rememberInfiniteTransition(label = "logo_float")
    val logoBob by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo_bob",
    )
    val bobPx = with(density) { 6.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenNavigationDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.open_menu),
                    tint = barIconTint,
                )
            }
            IconButton(onClick = onThemeToggle) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = if (isDarkMode) {
                        stringResource(R.string.theme_light_mode)
                    } else {
                        stringResource(R.string.theme_dark_mode)
                    },
                    tint = barIconTint,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer { translationY = logoBob * bobPx }
                            .size(148.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color.White else Color.White.copy(alpha = 0.85f))
                            .border(
                                width = 2.dp,
                                color = SunoXColors.Primary.copy(alpha = 0.4f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.main_icon),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = unbounded,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            lineHeight = 34.sp,
                        ),
                        textAlign = TextAlign.Center,
                        color = titleColor,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.home_tagline_under_name),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal,
                        ),
                        color = taglineColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (showHearingPrompt) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_test_prompt),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Normal,
                                lineHeight = 24.sp,
                            ),
                            color = promptTextColor,
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        SunoXPrimaryButton(
                            text = stringResource(R.string.home_start_test),
                            onClick = onStartAudiogramTest,
                            height = 52.dp,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.home_test_prompt_skip),
                                style = MaterialTheme.typography.bodyMedium,
                                color = linkMuted,
                                modifier = Modifier.clickable { skippedThisSession = true },
                            )
                            Text(
                                text = stringResource(R.string.home_test_prompt_dismiss),
                                style = MaterialTheme.typography.bodyMedium,
                                color = linkMuted,
                                modifier = Modifier.clickable {
                                    HearingTestPreferences.setDismissedForever(context, accountId)
                                    prefsEpoch++
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (showHomeOpenAmplifier) {
                SunoXPrimaryButton(
                    text = stringResource(R.string.home_open_amplifier),
                    onClick = onOpenAmplifier,
                    height = 52.dp,
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
            SunoXSecondaryButtonLightAware(
                text = stringResource(R.string.home_take_test),
                onClick = onStartAudiogramTest,
                isDark = isDarkMode,
                height = 52.dp,
            )
            Spacer(modifier = Modifier.height(18.dp))
            SunoXDestructiveTextButton(
                text = stringResource(R.string.profile_logout),
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
