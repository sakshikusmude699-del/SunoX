package com.soundamplifier.ui.screens

import android.Manifest
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.soundamplifier.R
import com.soundamplifier.audio.AmplifierPreset
import com.soundamplifier.data.CustomPreset
import com.soundamplifier.audio.Presets
import com.soundamplifier.ui.preset.PresetIcons
import com.soundamplifier.viewmodel.AmplifierUiState
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Design system colors
private val PrimaryPurple = Color(0xFF7C5CBF)
private val LightPurple = Color(0xFF9B7FD4)
private val PurpleTintBg = Color(0x1A7C5CBF)
private val GreenAccent = Color(0xFF4CAF50)
private val BackgroundColor = Color(0xFFF7F6FB)
private val CardColor = Color(0xB3FFFFFF)
private val CardBorder = Color(0x0F000000)
private val SubtleText = Color(0xFF8A8A9A)
private val PurpleInactiveTrack = Color(0x147C5CBF)

/** Wide enough for longest built-in preset label (e.g. Conversation, TV / Media) on one line. */
private val PresetChipWidth = 112.dp

private fun hasHeadphoneLikeOutput(audioManager: AudioManager): Boolean =
    try {
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any(::isHeadphoneLikeOutputDevice)
    } catch (_: SecurityException) {
        false
    }

private fun isHeadphoneLikeOutputDevice(device: AudioDeviceInfo): Boolean = when (device.type) {
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true
    else -> Build.VERSION.SDK_INT >= 31 &&
        (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
}

@Composable
fun AmplifierScreen(
    uiState: AmplifierUiState,
    onToggle: () -> Unit,
    onBoostQuietSoundsChange: (Int) -> Unit,
    onMasterGainChange: (Float) -> Unit,
    onLowBoostChange: (Float) -> Unit,
    onHighBoostChange: (Float) -> Unit,
    onApplyPreset: (AmplifierPreset) -> Unit = {},
    builtInPresets: List<AmplifierPreset> = Presets.ALL,
    customPresets: List<CustomPreset> = emptyList(),
    onApplyCustomPreset: (CustomPreset) -> Unit = {},
    onDeleteCustomPreset: (CustomPreset) -> Unit = {},
    onSaveCurrentAsPreset: (String) -> Unit = {},
    toastMessages: kotlinx.coroutines.flow.Flow<String>? = null,
    isDarkMode: Boolean = false,
    onThemeToggle: () -> Unit = {},
    onOpenNavigationDrawer: () -> Unit = {},
    onNavigatePresets: () -> Unit = {},
    onResumeMicLabels: () -> Unit = {}
) {
    val context = LocalContext.current
    val localeLang = LocalConfiguration.current.locales[0].language
    LaunchedEffect(localeLang) {
        onResumeMicLabels()
    }
    val audioManager = remember(context) { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    var outputDevicesEpoch by remember { mutableStateOf(0) }
    DisposableEffect(audioManager) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                outputDevicesEpoch++
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                outputDevicesEpoch++
            }
        }
        val handler = Handler(Looper.getMainLooper())
        audioManager.registerAudioDeviceCallback(callback, handler)
        onDispose {
            audioManager.unregisterAudioDeviceCallback(callback)
        }
    }
    val headphoneOutputConnected = remember(outputDevicesEpoch, uiState.micOptions) {
        hasHeadphoneLikeOutput(audioManager)
    }
    LaunchedEffect(toastMessages) {
        toastMessages?.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val unbounded = FontFamily(
        Font(
            googleFont = GoogleFont("Unbounded"),
            fontProvider = GoogleFont.Provider(
                providerAuthority = "com.google.android.gms.fonts",
                providerPackage = "com.google.android.gms",
                certificates = R.array.com_google_android_gms_fonts_certs
            ),
            weight = FontWeight.Bold
        )
    )

    val scrollState = rememberScrollState()
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var savePresetName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
            // 1. Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenNavigationDrawer,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.open_menu),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GreenAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.main_icon),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            modifier = Modifier.fillMaxSize(0.76f)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.amplifier_brand),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = unbounded,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.amplifier_subtitle),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onThemeToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = if (isDarkMode) {
                            stringResource(R.string.theme_light_mode)
                        } else {
                            stringResource(R.string.theme_dark_mode)
                        },
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Main control card
        val cardShape = RoundedCornerShape(20.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, cardShape, ambientColor = Color(0x0F000000), spotColor = Color(0x0F000000))
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), cardShape)
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Text(
                    text = stringResource(R.string.input_level),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                    Text(
                        text = if (uiState.isRunning) stringResource(R.string.status_active)
                        else stringResource(R.string.status_inactive),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = if (uiState.isRunning) GreenAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                InputLevelBar(
                    level = uiState.inputLevel,
                    isRunning = uiState.isRunning,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!headphoneOutputConnected) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.amplifier_no_headphone_warning),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                PlayStopButton(
                    isRunning = uiState.isRunning,
                    onClick = onToggle,
                    startLabel = stringResource(R.string.cd_start),
                    stopLabel = stringResource(R.string.cd_stop)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.isRunning) stringResource(R.string.tap_to_stop)
                    else stringResource(R.string.tap_to_start),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Sliders card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, cardShape, ambientColor = Color(0x0F000000), spotColor = Color(0x0F000000))
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), cardShape)
                .padding(20.dp)
        ) {
            Column {
                PremiumSlider(
                    label = stringResource(R.string.boost_quiet_sounds),
                    value = uiState.boostQuietSounds.toFloat(),
                    valueRange = 0f..100f,
                    displayValue = "${uiState.boostQuietSounds}%",
                    onValueChange = { onBoostQuietSoundsChange(it.roundToInt()) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                PremiumSlider(
                    label = stringResource(R.string.output_level),
                    value = uiState.masterGain,
                    valueRange = 1f..5f,
                    displayValue = "x${"%.1f".format(uiState.masterGain)}",
                    onValueChange = onMasterGainChange
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.frequency_boost),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FrequencyBoostCard(
                        label = stringResource(R.string.freq_low),
                        value = uiState.lowBoost,
                        onValueChange = onLowBoostChange,
                        modifier = Modifier.weight(1f)
                    )
                    FrequencyBoostCard(
                        label = stringResource(R.string.freq_high),
                        value = uiState.highBoost,
                        onValueChange = onHighBoostChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val canSavePreset =
            uiState.activePresetId == null && uiState.hasUserChangedSettings
        Text(
            text = stringResource(R.string.amplifier_presets_heading),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        PresetSelector(
            presets = builtInPresets,
            activePresetId = uiState.activePresetId,
            showCustom = canSavePreset,
            onPresetSelected = onApplyPreset,
            onBuiltinLongPress = onNavigatePresets,
            customLabel = stringResource(R.string.preset_custom),
            customPresets = customPresets,
            onApplyCustomPreset = onApplyCustomPreset,
            onDeleteCustomPreset = onDeleteCustomPreset,
            hearingProfileName = stringResource(R.string.hearing_test_my_profile),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (canSavePreset) {
                stringResource(R.string.save_preset_row_ready)
            } else {
                stringResource(R.string.save_preset_row_help)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                savePresetName = ""
                showSavePresetDialog = true
            },
            enabled = canSavePreset,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PrimaryPurple,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            ),
        ) {
            Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (canSavePreset) PrimaryPurple
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.save_preset_row_button),
                color = if (canSavePreset) PrimaryPurple
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }

        if (showSavePresetDialog) {
            AlertDialog(
                onDismissRequest = { showSavePresetDialog = false },
                title = { Text(stringResource(R.string.save_preset_title)) },
                text = {
                    OutlinedTextField(
                        value = savePresetName,
                        onValueChange = { savePresetName = it },
                        placeholder = { Text(stringResource(R.string.save_preset_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = savePresetName.trim()
                            if (name.isNotEmpty()) {
                                onSaveCurrentAsPreset(name)
                                showSavePresetDialog = false
                                savePresetName = ""
                            }
                        },
                        enabled = savePresetName.trim().isNotEmpty()
                    ) {
                        Text(stringResource(R.string.save_preset_save), color = PrimaryPurple)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSavePresetDialog = false }) {
                        Text(stringResource(R.string.save_preset_cancel))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetSelector(
    presets: List<AmplifierPreset>,
    activePresetId: String?,
    showCustom: Boolean,
    onPresetSelected: (AmplifierPreset) -> Unit,
    onBuiltinLongPress: () -> Unit = {},
    customLabel: String,
    customPresets: List<CustomPreset>,
    onApplyCustomPreset: (CustomPreset) -> Unit,
    onDeleteCustomPreset: (CustomPreset) -> Unit,
    hearingProfileName: String,
    modifier: Modifier = Modifier
) {
    val chipShape = RoundedCornerShape(12.dp)
    var menuPresetId by remember { mutableStateOf<Int?>(null) }
    var pendingDelete by remember { mutableStateOf<CustomPreset?>(null) }
    // Include locale in LazyRow keys so chips re-fetch stringResource() after language change
    // (otherwise item slots can keep stale English text).
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag().orEmpty()
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = presets,
            key = { preset -> "${preset.id}_$localeTag" },
        ) { preset ->
            val presetName = stringResource(preset.nameRes)
            val selected = activePresetId == preset.id
            val icon = PresetIcons.vector(preset.icon)
            Box(
                modifier = Modifier
                    .width(PresetChipWidth)
                    .height(80.dp)
                    .clip(chipShape)
                    .background(
                        if (selected) PurpleTintBg
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) PrimaryPurple else MaterialTheme.colorScheme.outlineVariant,
                        shape = chipShape
                    )
                    .combinedClickable(
                        onClick = { onPresetSelected(preset) },
                        onLongClick = onBuiltinLongPress,
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = presetName,
                        modifier = Modifier.size(24.dp),
                        tint = if (selected) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = presetName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = if (selected) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (showCustom) {
            item(key = "preset_custom_$localeTag") {
                Box(
                    modifier = Modifier
                        .width(PresetChipWidth)
                        .height(80.dp)
                        .clip(chipShape)
                        .background(PurpleTintBg)
                        .border(1.5.dp, PrimaryPurple, chipShape)
                        .clickable { /* Custom = no preset, user already in custom mode */ }
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = customLabel,
                            modifier = Modifier.size(24.dp),
                            tint = PrimaryPurple
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = customLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = PrimaryPurple,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        item(key = "spacer_before_custom_$localeTag") {
            Spacer(modifier = Modifier.width(12.dp))
        }
        items(
            items = customPresets,
            key = { cp -> "custom_${cp.id}_$localeTag" },
        ) { preset ->
            val customId = "custom_${preset.id}"
            val selected = activePresetId == customId
            val chipIcon =
                if (preset.name == hearingProfileName) {
                    PresetIcons.vector("hearing")
                } else {
                    PresetIcons.vector(preset.iconKey)
                }
            Box {
                Box(
                    modifier = Modifier
                        .width(PresetChipWidth)
                        .height(80.dp)
                        .clip(chipShape)
                        .background(
                            if (selected) PurpleTintBg
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) PrimaryPurple else MaterialTheme.colorScheme.outlineVariant,
                            shape = chipShape
                        )
                        .combinedClickable(
                            onClick = { onApplyCustomPreset(preset) },
                            onLongClick = {
                                if (preset.builtInPresetId == null) {
                                    menuPresetId = preset.id
                                }
                            },
                        )
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = chipIcon,
                            contentDescription = preset.name,
                            modifier = Modifier.size(24.dp),
                            tint = if (selected) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = if (selected) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                DropdownMenu(
                    expanded = menuPresetId == preset.id,
                    onDismissRequest = { menuPresetId = null }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.save_preset_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuPresetId = null
                            pendingDelete = preset
                        }
                    )
                }
            }
        }
    }

    pendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.save_preset_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomPreset(preset)
                        pendingDelete = null
                    }
                ) {
                    Text(
                        stringResource(R.string.save_preset_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.save_preset_cancel))
                }
            }
        )
    }
}

@Composable
private fun InputLevelBar(
    level: Float,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val displayLevel = when {
        !isRunning -> 0f
        else -> min(1f, max(0.02f, level * 5f))
    }
    val animatedLevel by animateFloatAsState(
        targetValue = displayLevel,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "level"
    )
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedLevel)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(GreenAccent, Color(0xFF81C784))
                    )
                )
        )
    }
}

@Composable
private fun PlayStopButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    startLabel: String,
    stopLabel: String
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isRunning) GreenAccent else PrimaryPurple,
        label = "buttonColor"
    )
    Box(
        modifier = Modifier
            .size(72.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(36.dp),
                ambientColor = PrimaryPurple.copy(alpha = 0.3f),
                spotColor = PrimaryPurple.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        if (isRunning) GreenAccent else LightPurple,
                        buttonColor
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            contentDescription = if (isRunning) stopLabel else startLabel,
            modifier = Modifier.size(36.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun PremiumSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = PrimaryPurple
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = PrimaryPurple,
                inactiveTrackColor = PurpleInactiveTrack
            )
        )
    }
}

@Composable
private fun RowScope.FrequencyBoostCard(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(PurpleTintBg)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), cardShape)
            .clickable { showDialog = true }
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.db_format, value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.frequency_boost_format, label)) },
            text = {
                Column {
                    PremiumSlider(
                        label = label,
                        value = value,
                        valueRange = -12f..12f,
                        displayValue = stringResource(R.string.db_format, value),
                        onValueChange = onValueChange
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.done), color = PrimaryPurple)
                }
            }
        )
    }
}
