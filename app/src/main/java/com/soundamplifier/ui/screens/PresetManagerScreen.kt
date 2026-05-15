package com.soundamplifier.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.soundamplifier.R
import com.soundamplifier.audio.AmplifierPreset
import com.soundamplifier.data.CustomPreset
import com.soundamplifier.ui.preset.PresetIcons
import com.soundamplifier.viewmodel.AmplifierViewModel

private val DefaultBuiltInIds = setOf("conversation", "music", "outdoors")

private data class BuiltInEditState(
    val id: String,
    @StringRes val nameRes: Int,
    val showDefaultBadge: Boolean,
    val iconKey: String,
    val boostPct: Float,
    val master: Float,
    val low: Float,
    val high: Float,
) {
    fun copy(
        iconKey: String = this.iconKey,
        boostPct: Float = this.boostPct,
        master: Float = this.master,
        low: Float = this.low,
        high: Float = this.high,
    ): BuiltInEditState = BuiltInEditState(
        id, nameRes, showDefaultBadge, iconKey, boostPct, master, low, high,
    )
}

private fun AmplifierPreset.toBuiltInEditState(showDefaultBadge: Boolean) = BuiltInEditState(
    id = id,
    nameRes = nameRes,
    showDefaultBadge = showDefaultBadge,
    iconKey = icon,
    boostPct = boostQuietSounds * 100f,
    master = masterGain,
    low = lowBoostDb,
    high = highBoostDb,
)

private data class UserEditState(
    val id: Int,
    val name: String,
    val iconKey: String,
    val boostPct: Float,
    val master: Float,
    val low: Float,
    val high: Float,
    val markedDelete: Boolean,
    val lockDelete: Boolean,
) {
    fun copy(
        name: String = this.name,
        iconKey: String = this.iconKey,
        boostPct: Float = this.boostPct,
        master: Float = this.master,
        low: Float = this.low,
        high: Float = this.high,
        markedDelete: Boolean = this.markedDelete,
    ): UserEditState = UserEditState(
        id, name, iconKey, boostPct, master, low, high, markedDelete, lockDelete,
    )

    companion object {
        fun from(preset: CustomPreset, lockDelete: Boolean) = UserEditState(
            id = preset.id,
            name = preset.name,
            iconKey = preset.iconKey,
            boostPct = preset.boostQuietSounds * 100f,
            master = preset.masterGain,
            low = preset.lowBoostDb,
            high = preset.highBoostDb,
            markedDelete = false,
            lockDelete = lockDelete,
        )
    }
}

data class PresetEditorModel(
    val builtInId: String?,
    val existingUserId: Int?,
    val initialName: String,
    val initialIcon: String,
    val initialBoost: Float,
    val initialMaster: Float,
    val initialLow: Float,
    val initialHigh: Float,
    val nameLocked: Boolean,
) {
    companion object {
        fun newUser() = PresetEditorModel(
            builtInId = null,
            existingUserId = null,
            initialName = "",
            initialIcon = "tune",
            initialBoost = 0.5f,
            initialMaster = 2f,
            initialLow = 0f,
            initialHigh = 4f,
            nameLocked = false,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PresetManagerScreen(
    viewModel: AmplifierViewModel,
    onBack: () -> Unit,
    onOpenNavigationDrawer: () -> Unit = {},
) {
    val builtIns by viewModel.builtInPresetsDisplay.collectAsState()
    val userPresets by viewModel.userOnlyCustomPresets.collectAsState()
    val hearingProfileName = stringResource(R.string.hearing_test_my_profile)

    var editor by remember { mutableStateOf<PresetEditorModel?>(null) }
    var pendingResetBuiltIn by remember { mutableStateOf<String?>(null) }

    var editMode by remember { mutableStateOf(false) }
    var builtInDrafts by remember { mutableStateOf<List<BuiltInEditState>>(emptyList()) }
    var userDrafts by remember { mutableStateOf<List<UserEditState>>(emptyList()) }

    val handleBack = {
        if (editMode) {
            editMode = false
            builtInDrafts = emptyList()
            userDrafts = emptyList()
        } else {
            onBack()
        }
    }

    fun enterEditMode() {
        builtInDrafts = builtIns.map { it.toBuiltInEditState(it.id in DefaultBuiltInIds) }
        userDrafts = usersSorted(userPresets).map { UserEditState.from(it, it.name == hearingProfileName) }
        editMode = true
    }

    fun saveEdits() {
        val boostMasterTol = 0.0001f
        builtInDrafts.forEach { d ->
            val orig = builtIns.find { it.id == d.id } ?: return@forEach
            val changed = d.iconKey != orig.icon ||
                kotlin.math.abs(d.boostPct / 100f - orig.boostQuietSounds) > boostMasterTol ||
                kotlin.math.abs(d.master - orig.masterGain) > boostMasterTol ||
                kotlin.math.abs(d.low - orig.lowBoostDb) > 0.001f ||
                kotlin.math.abs(d.high - orig.highBoostDb) > 0.001f
            if (changed) {
                viewModel.upsertBuiltInOverride(
                    d.id,
                    d.iconKey,
                    d.boostPct / 100f,
                    d.master,
                    d.low,
                    d.high,
                )
            }
        }
        userDrafts.forEach { d ->
            if (d.markedDelete) {
                userPresets.find { it.id == d.id }?.let { viewModel.deleteCustomPreset(it) }
                return@forEach
            }
            val orig = userPresets.find { it.id == d.id } ?: return@forEach
            val nm = d.name.trim()
            if (nm.isEmpty()) return@forEach
            val boost = d.boostPct / 100f
            val changed = orig.name != nm || orig.iconKey != d.iconKey ||
                kotlin.math.abs(orig.boostQuietSounds - boost) > boostMasterTol ||
                kotlin.math.abs(orig.masterGain - d.master) > boostMasterTol ||
                kotlin.math.abs(orig.lowBoostDb - d.low) > 0.001f ||
                kotlin.math.abs(orig.highBoostDb - d.high) > 0.001f
            if (changed) {
                viewModel.updateUserPreset(
                    orig.copy(
                        name = nm,
                        iconKey = d.iconKey,
                        boostQuietSounds = boost,
                        masterGain = d.master,
                        lowBoostDb = d.low,
                        highBoostDb = d.high,
                    ),
                )
            }
        }
        editMode = false
        builtInDrafts = emptyList()
        userDrafts = emptyList()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preset_manager_title)) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenNavigationDrawer) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.open_menu),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = {
            if (!editMode) {
                FloatingActionButton(
                    onClick = { editor = PresetEditorModel.newUser() },
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.preset_add_new))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            if (!editMode) {
                items(builtIns, key = { "built_${it.id}" }) { preset ->
                    val isDefaultFamily = preset.id in DefaultBuiltInIds
                    PresetCardReadOnly(
                        title = stringResource(preset.nameRes),
                        summary = summaryForBuiltIn(preset),
                        iconKey = preset.icon,
                        showDefaultBadge = isDefaultFamily,
                    )
                }
                items(usersSorted(userPresets), key = { "usr_${it.id}" }) { preset ->
                    PresetCardReadOnly(
                        title = preset.name,
                        summary = summaryForUser(preset),
                        iconKey = preset.iconKey,
                        showDefaultBadge = false,
                    )
                }
            } else {
                items(builtInDrafts, key = { "ed_b_${it.id}" }) { draft ->
                    BuiltInPresetEditCard(
                        state = draft,
                        onStateChange = { new ->
                            builtInDrafts = builtInDrafts.map { if (it.id == new.id) new else it }
                        },
                        onResetClick = { pendingResetBuiltIn = draft.id },
                    )
                }
                items(userDrafts, key = { "ed_u_${it.id}" }) { draft ->
                    UserPresetEditCard(
                        state = draft,
                        onStateChange = { new ->
                            userDrafts = userDrafts.map { if (it.id == new.id) new else it }
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (editMode) {
                    Button(
                        onClick = { saveEdits() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(R.string.preset_save_changes))
                    }
                } else {
                    Button(
                        onClick = { enterEditMode() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(R.string.preset_edit_presets))
                    }
                }
                Spacer(modifier = Modifier.height(if (editMode) 24.dp else 88.dp))
            }
        }
    }

    editor?.let { model ->
        PresetEditorDialog(
            model = model,
            onDismiss = { editor = null },
            onSave = { m, name, icon, boost, master, low, high ->
                when {
                    m.builtInId != null -> viewModel.upsertBuiltInOverride(m.builtInId, icon, boost, master, low, high)
                    m.existingUserId != null -> {
                        val orig = userPresets.find { it.id == m.existingUserId } ?: return@PresetEditorDialog
                        viewModel.updateUserPreset(
                            orig.copy(
                                name = name,
                                iconKey = icon,
                                boostQuietSounds = boost,
                                masterGain = master,
                                lowBoostDb = low,
                                highBoostDb = high,
                            ),
                        )
                    }
                    else -> viewModel.createUserPreset(name, icon, boost, master, low, high)
                }
                editor = null
            },
        )
    }

    pendingResetBuiltIn?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingResetBuiltIn = null },
            title = { Text(stringResource(R.string.preset_reset_builtin_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetBuiltInOverride(id)
                        pendingResetBuiltIn = null
                        if (editMode) {
                            editMode = false
                            builtInDrafts = emptyList()
                            userDrafts = emptyList()
                        }
                    },
                ) {
                    Text(stringResource(R.string.preset_reset_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingResetBuiltIn = null }) {
                    Text(stringResource(R.string.save_preset_cancel))
                }
            },
        )
    }
}

@Composable
private fun summaryForBuiltIn(p: AmplifierPreset): String {
    val boost = (p.boostQuietSounds * 100).toInt().coerceIn(0, 100)
    return stringResource(R.string.preset_summary, boost, p.masterGain, p.lowBoostDb, p.highBoostDb)
}

@Composable
private fun summaryForUser(p: CustomPreset): String {
    val boost = (p.boostQuietSounds * 100).toInt().coerceIn(0, 100)
    return stringResource(R.string.preset_summary, boost, p.masterGain, p.lowBoostDb, p.highBoostDb)
}

private fun usersSorted(list: List<CustomPreset>): List<CustomPreset> =
    list.sortedBy { it.name.lowercase() }

@Composable
private fun PresetCardReadOnly(
    title: String,
    summary: String,
    iconKey: String,
    showDefaultBadge: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                PresetIcons.vector(iconKey),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (showDefaultBadge) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        ) {
                            Text(
                                text = stringResource(R.string.preset_default_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuiltInPresetEditCard(
    state: BuiltInEditState,
    onStateChange: (BuiltInEditState) -> Unit,
    onResetClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    PresetIcons.vector(state.iconKey),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(state.nameRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.showDefaultBadge) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        ) {
                            Text(
                                stringResource(R.string.preset_default_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = onResetClick) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.preset_reset_confirm_button),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.preset_icon_label), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresetIcons.choices.forEach { (key, _) ->
                    val selected = key == state.iconKey
                    IconButton(
                        onClick = { onStateChange(state.copy(iconKey = key)) },
                        modifier = Modifier
                            .size(44.dp)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp),
                            ),
                    ) {
                        Icon(
                            PresetIcons.vector(key),
                            contentDescription = key,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PresetEditSliders(
                boostPct = state.boostPct,
                onBoostChange = { onStateChange(state.copy(boostPct = it)) },
                master = state.master,
                onMasterChange = { onStateChange(state.copy(master = it)) },
                low = state.low,
                onLowChange = { onStateChange(state.copy(low = it)) },
                high = state.high,
                onHighChange = { onStateChange(state.copy(high = it)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserPresetEditCard(
    state: UserEditState,
    onStateChange: (UserEditState) -> Unit,
) {
    val deletedStyle = state.markedDelete
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    PresetIcons.vector(state.iconKey),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onStateChange(state.copy(name = it)) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    label = { Text(stringResource(R.string.preset_name_label)) },
                    singleLine = true,
                    enabled = !deletedStyle,
                )
                if (!state.lockDelete) {
                    IconButton(
                        onClick = { onStateChange(state.copy(markedDelete = !state.markedDelete)) },
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.save_preset_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (deletedStyle) {
                Text(
                    stringResource(R.string.save_preset_delete),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onStateChange(state.copy(markedDelete = false)) }) {
                    Text(stringResource(R.string.save_preset_cancel))
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.preset_icon_label), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PresetIcons.choices.forEach { (key, _) ->
                        val selected = key == state.iconKey
                        IconButton(
                            onClick = { onStateChange(state.copy(iconKey = key)) },
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp),
                                ),
                        ) {
                            Icon(
                                PresetIcons.vector(key),
                                contentDescription = key,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                PresetEditSliders(
                    boostPct = state.boostPct,
                    onBoostChange = { onStateChange(state.copy(boostPct = it)) },
                    master = state.master,
                    onMasterChange = { onStateChange(state.copy(master = it)) },
                    low = state.low,
                    onLowChange = { onStateChange(state.copy(low = it)) },
                    high = state.high,
                    onHighChange = { onStateChange(state.copy(high = it)) },
                )
            }
        }
    }
}

@Composable
private fun PresetEditSliders(
    boostPct: Float,
    onBoostChange: (Float) -> Unit,
    master: Float,
    onMasterChange: (Float) -> Unit,
    low: Float,
    onLowChange: (Float) -> Unit,
    high: Float,
    onHighChange: (Float) -> Unit,
) {
    Text("${stringResource(R.string.boost_quiet_sounds)}: ${boostPct.toInt()}%")
    Slider(value = boostPct, onValueChange = onBoostChange, valueRange = 0f..100f)
    Text("${stringResource(R.string.output_level)}: x${"%.1f".format(master)}")
    Slider(value = master, onValueChange = onMasterChange, valueRange = 1f..5f)
    Text("${stringResource(R.string.freq_low)}: ${low.toInt()} dB")
    Slider(value = low, onValueChange = onLowChange, valueRange = -12f..12f)
    Text("${stringResource(R.string.freq_high)}: ${high.toInt()} dB")
    Slider(value = high, onValueChange = onHighChange, valueRange = 0f..18f)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetEditorDialog(
    model: PresetEditorModel,
    onDismiss: () -> Unit,
    onSave: (PresetEditorModel, String, String, Float, Float, Float, Float) -> Unit,
) {
    var name by remember(model) { mutableStateOf(model.initialName) }
    var iconKey by remember(model) { mutableStateOf(model.initialIcon) }
    var boost by remember(model) { mutableFloatStateOf(model.initialBoost * 100f) }
    var master by remember(model) { mutableFloatStateOf(model.initialMaster) }
    var low by remember(model) { mutableFloatStateOf(model.initialLow) }
    var high by remember(model) { mutableFloatStateOf(model.initialHigh) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    model.builtInId != null -> stringResource(R.string.preset_editor_title_edit)
                    model.existingUserId != null -> stringResource(R.string.preset_editor_title_edit)
                    else -> stringResource(R.string.preset_editor_title_new)
                },
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
            ) {
                if (!model.nameLocked) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.preset_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(stringResource(R.string.preset_icon_label), style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PresetIcons.choices.forEach { (key, _) ->
                        val selected = key == iconKey
                        IconButton(
                            onClick = { iconKey = key },
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp),
                                ),
                        ) {
                            Icon(
                                PresetIcons.vector(key),
                                contentDescription = key,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("${stringResource(R.string.boost_quiet_sounds)}: ${boost.toInt()}%")
                Slider(value = boost, onValueChange = { boost = it }, valueRange = 0f..100f)
                Text("${stringResource(R.string.output_level)}: x${"%.1f".format(master)}")
                Slider(value = master, onValueChange = { master = it }, valueRange = 1f..5f)
                Text("${stringResource(R.string.freq_low)}: ${low.toInt()} dB")
                Slider(value = low, onValueChange = { low = it }, valueRange = -12f..12f)
                Text("${stringResource(R.string.freq_high)}: ${high.toInt()} dB")
                Slider(value = high, onValueChange = { high = it }, valueRange = 0f..18f)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val nm = if (model.nameLocked) "" else name.trim()
                    if (!model.nameLocked && nm.isEmpty()) return@TextButton
                    onSave(model, nm, iconKey, boost / 100f, master, low, high)
                },
                enabled = model.nameLocked || name.trim().isNotEmpty(),
            ) {
                Text(stringResource(R.string.save_preset_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.save_preset_cancel))
            }
        },
    )
}
