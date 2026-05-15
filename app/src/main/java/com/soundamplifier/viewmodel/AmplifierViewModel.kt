package com.soundamplifier.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.soundamplifier.R
import com.soundamplifier.audio.AmplifierPreset
import com.soundamplifier.audio.MicSourceManager
import com.soundamplifier.audio.Presets
import com.soundamplifier.audio.mergeBuiltInsWithOverrides
import com.soundamplifier.audio.MicSourceOption
import com.soundamplifier.audio.NativeAudioEngine
import com.soundamplifier.data.AccountLocalIds
import com.soundamplifier.data.AudiogramProfile
import com.soundamplifier.data.CustomPreset
import com.soundamplifier.data.CustomPresetDao
import com.soundamplifier.data.FirestoreUserRepository
import com.soundamplifier.data.ProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class AmplifierUiState(
    val isRunning: Boolean = false,
    val inputLevel: Float = 0f,
    val micOptions: List<MicSourceOption> = emptyList(),
    val boostQuietSounds: Int = 75,  // 0-100%, scales compression ratio and makeup gain
    val masterGain: Float = 1.5f,
    val lowBoost: Float = 0f,    // dB
    val highBoost: Float = 0f,   // dB
    val activeProfile: AudiogramProfile? = null,
    val activePresetId: String? = null,
    val hasUserChangedSettings: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class AmplifierViewModel(
    private val repository: ProfileRepository,
    private val customPresetDao: CustomPresetDao,
    appContext: Context,
) : ViewModel() {

    private val appContext = appContext.applicationContext
    private val engine = NativeAudioEngine()
    private val micManager = MicSourceManager(appContext, engine)
    private val firestoreUserRepository = FirestoreUserRepository()
    private val presetToasts = MutableSharedFlow<String>(extraBufferCapacity = 1)

    private val accountLocalId = MutableStateFlow(AccountLocalIds.localKey(appContext))

    val toasts: SharedFlow<String> = merge(micManager.toasts, presetToasts)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val customPresets: StateFlow<List<CustomPreset>> = accountLocalId
        .flatMapLatest { id -> customPresetDao.getAllPresetsFlowForAccount(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val builtInPresetsDisplay: StateFlow<List<AmplifierPreset>> = customPresets
        .map { mergeBuiltInsWithOverrides(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Presets.ALL)

    val userOnlyCustomPresets: StateFlow<List<CustomPreset>> = customPresets
        .map { list -> list.filter { it.builtInPresetId == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(AmplifierUiState())
    val uiState: StateFlow<AmplifierUiState> = combine(
        _uiState,
        engine.inputLevel,
        micManager.options
    ) { state, level, options ->
        state.copy(
            inputLevel = level,
            micOptions = options
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AmplifierUiState())

    fun applyPreset(preset: AmplifierPreset) {
        val boostPercent = (preset.boostQuietSounds * 100).toInt().coerceIn(0, 100)
        engine.setBoostQuietSounds(preset.boostQuietSounds)
        engine.setMasterGain(preset.masterGain)
        engine.setLowBoost(preset.lowBoostDb)
        engine.setHighBoost(preset.highBoostDb)
        _uiState.value = _uiState.value.copy(
            boostQuietSounds = boostPercent,
            masterGain = preset.masterGain,
            lowBoost = preset.lowBoostDb,
            highBoost = preset.highBoostDb,
            activePresetId = preset.id,
            hasUserChangedSettings = false
        )
    }

    fun applyCustomPreset(preset: CustomPreset) {
        val boostPercent = (preset.boostQuietSounds * 100).toInt().coerceIn(0, 100)
        engine.setBoostQuietSounds(preset.boostQuietSounds)
        engine.setMasterGain(preset.masterGain)
        engine.setLowBoost(preset.lowBoostDb)
        engine.setHighBoost(preset.highBoostDb)
        _uiState.value = _uiState.value.copy(
            boostQuietSounds = boostPercent,
            masterGain = preset.masterGain,
            lowBoost = preset.lowBoostDb,
            highBoost = preset.highBoostDb,
            activePresetId = "custom_${preset.id}",
            hasUserChangedSettings = false
        )
    }

    /**
     * Derives preset params from bilateral thresholds (dB HL per frequency, same order as [AUDIOGRAM_FREQUENCIES]).
     */
    fun buildMyHearingProfilePreset(left: List<Float>, right: List<Float>): CustomPreset {
        require(left.size >= 6 && right.size >= 6)
        val pairAvg = (0 until 6).map { i -> (left[i] + right[i]) / 2f }
        val avgHl = pairAvg.average().toFloat()
        val lowAvg = (0..2).map { pairAvg[it] }.average().toFloat()
        val highAvg = (3..5).map { pairAvg[it] }.average().toFloat()
        val slope = highAvg - lowAvg
        var highBoostExtra = 0f
        if (slope > 12f) highBoostExtra += 2f
        if (slope > 22f) highBoostExtra += 2f
        if (slope > 32f) highBoostExtra += 2f

        val boost: Float
        val master: Float
        val lowB: Float
        val highB: Float
        when {
            avgHl < 25f -> {
                boost = 0.3f; master = 1.5f; lowB = 0f; highB = 2f
            }
            avgHl < 40f -> {
                boost = 0.5f; master = 2f; lowB = 0f; highB = 4f
            }
            avgHl < 55f -> {
                boost = 0.7f; master = 2.5f; lowB = 2f; highB = 6f
            }
            avgHl < 70f -> {
                boost = 0.85f; master = 3f; lowB = 3f; highB = 8f
            }
            else -> {
                boost = 1f; master = 4f; lowB = 4f; highB = 10f
            }
        }
        val highFinal = (highB + highBoostExtra).coerceIn(0f, 18f)
        val name = appContext.getString(R.string.hearing_test_my_profile)
        return CustomPreset(
            accountId = accountLocalId.value,
            name = name,
            boostQuietSounds = boost.coerceIn(0f, 1f),
            masterGain = master.coerceIn(1f, 5f),
            lowBoostDb = lowB,
            highBoostDb = highFinal,
            iconKey = "hearing",
        )
    }

    /**
     * Replace any Room preset with the hearing-profile name, insert new row, apply engine + UI, toast, optional Firestore.
     * Call from a coroutine (e.g. MainActivity) after [persistAudiogram].
     */
    suspend fun replaceMyHearingProfileAndApplyNow(preset: CustomPreset) {
        val name = appContext.getString(R.string.hearing_test_my_profile)
        val scope = accountLocalId.value
        val saved = withContext(Dispatchers.IO) {
            customPresetDao.deleteByNameForAccount(scope, name)
            val toInsert = preset.copy(name = name, id = 0, accountId = scope)
            val newId = customPresetDao.insert(toInsert).toInt()
            toInsert.copy(id = newId)
        }
        applyCustomPreset(saved)
        presetToasts.emit(appContext.getString(R.string.hearing_test_profile_applied))
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                firestoreUserRepository.saveCustomPreset(user.uid, saved)
            } catch (_: Exception) {
            }
        }
    }

    fun saveCurrentAsPreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val s = _uiState.value
            val boost = (s.boostQuietSounds / 100f).coerceIn(0f, 1f)
            val scope = accountLocalId.value
            val entity = CustomPreset(
                accountId = scope,
                name = trimmed,
                boostQuietSounds = boost,
                masterGain = s.masterGain,
                lowBoostDb = s.lowBoost,
                highBoostDb = s.highBoost,
            )
            val newId = customPresetDao.insert(entity).toInt()
            val saved = entity.copy(id = newId)
            engine.setBoostQuietSounds(boost)
            engine.setMasterGain(saved.masterGain)
            engine.setLowBoost(saved.lowBoostDb)
            engine.setHighBoost(saved.highBoostDb)
            _uiState.value = s.copy(
                boostQuietSounds = (boost * 100).toInt().coerceIn(0, 100),
                masterGain = saved.masterGain,
                lowBoost = saved.lowBoostDb,
                highBoost = saved.highBoostDb,
                activePresetId = "custom_$newId",
                hasUserChangedSettings = false,
            )
            presetToasts.emit(appContext.getString(R.string.save_preset_saved))
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                try {
                    firestoreUserRepository.saveCustomPreset(user.uid, saved)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun deleteCustomPreset(preset: CustomPreset) {
        if (preset.builtInPresetId != null) return
        viewModelScope.launch {
            customPresetDao.delete(preset)
            val active = _uiState.value.activePresetId
            if (active == "custom_${preset.id}") {
                _uiState.value = _uiState.value.copy(activePresetId = null)
            }
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                try {
                    firestoreUserRepository.deleteCustomPreset(user.uid, preset.id)
                } catch (_: Exception) {
                }
            }
        }
    }

    private suspend fun syncPresetToCloud(preset: CustomPreset) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        try {
            firestoreUserRepository.saveCustomPreset(user.uid, preset)
        } catch (_: Exception) {
        }
    }

    fun createUserPreset(name: String, iconKey: String, boost: Float, master: Float, low: Float, high: Float) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val scope = accountLocalId.value
            val entity = CustomPreset(
                accountId = scope,
                name = trimmed,
                boostQuietSounds = boost.coerceIn(0f, 1f),
                masterGain = master.coerceIn(1f, 5f),
                lowBoostDb = low,
                highBoostDb = high,
                iconKey = iconKey,
                builtInPresetId = null,
            )
            val newId = customPresetDao.insert(entity).toInt()
            val saved = entity.copy(id = newId)
            syncPresetToCloud(saved)
            presetToasts.emit(appContext.getString(R.string.preset_saved))
        }
    }

    fun updateUserPreset(preset: CustomPreset) {
        if (preset.builtInPresetId != null) return
        viewModelScope.launch {
            customPresetDao.update(preset)
            syncPresetToCloud(preset)
            presetToasts.emit(appContext.getString(R.string.preset_saved))
        }
    }

    fun upsertBuiltInOverride(
        builtInId: String,
        iconKey: String,
        boost: Float,
        master: Float,
        low: Float,
        high: Float,
    ) {
        val base = Presets.ALL.find { it.id == builtInId } ?: return
        viewModelScope.launch {
            val scope = accountLocalId.value
            val name = appContext.getString(base.nameRes)
            val row = CustomPreset(
                accountId = scope,
                name = name,
                boostQuietSounds = boost.coerceIn(0f, 1f),
                masterGain = master.coerceIn(1f, 5f),
                lowBoostDb = low,
                highBoostDb = high,
                iconKey = iconKey,
                builtInPresetId = builtInId,
            )
            val existing = customPresetDao.getBuiltInOverride(scope, builtInId)
            val saved = if (existing != null) {
                val updated = row.copy(id = existing.id, createdAt = existing.createdAt)
                customPresetDao.update(updated)
                updated
            } else {
                val id = customPresetDao.insert(row.copy(id = 0)).toInt()
                row.copy(id = id)
            }
            syncPresetToCloud(saved)
            presetToasts.emit(appContext.getString(R.string.preset_saved))
        }
    }

    fun resetBuiltInOverride(builtInId: String) {
        viewModelScope.launch {
            val scope = accountLocalId.value
            val existing = customPresetDao.getBuiltInOverride(scope, builtInId) ?: return@launch
            customPresetDao.deleteBuiltInOverride(scope, builtInId)
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                try {
                    firestoreUserRepository.deleteCustomPreset(user.uid, existing.id)
                } catch (_: Exception) {
                }
            }
            val active = _uiState.value.activePresetId
            if (active == builtInId) {
                applyPreset(Presets.ALL.find { it.id == builtInId }!!)
            }
            presetToasts.emit(appContext.getString(R.string.preset_reset_default))
        }
    }

    private fun clearPresetOnManualChange() {
        _uiState.value = _uiState.value.copy(
            activePresetId = null,
            hasUserChangedSettings = true
        )
    }

    init {
        micManager.start()
        loadLatestProfilePublic()
    }

    /** Call when the signed-in user or guest flag changes so Room reads the correct partition. */
    fun refreshAccountScope(context: Context) {
        val key = AccountLocalIds.localKey(context.applicationContext)
        val previous = accountLocalId.value
        accountLocalId.value = key
        if (previous != key) {
            engine.stop()
            _uiState.value = AmplifierUiState()
        }
        loadLatestProfilePublic()
    }

    fun loadLatestProfilePublic() {
        viewModelScope.launch {
            val profile = repository.getLatestForAccount(accountLocalId.value)
            if (profile != null) {
                applyProfile(profile)
            } else {
                _uiState.value = _uiState.value.copy(activeProfile = null)
                engine.setPrescribedGains(FloatArray(6) { 10f })
            }
        }
    }

    fun refreshMicDeviceLabels() {
        micManager.refreshDeviceList()
    }

    fun applyProfile(profile: AudiogramProfile) {
        val prescribedGains = computePrescribedGains(profile.leftThresholdList())
        engine.setPrescribedGains(prescribedGains)
        _uiState.value = _uiState.value.copy(activeProfile = profile)
    }

    /** NAL-NL2 / DSL inspired: prescribedGain = hearingLoss * 0.46 + 0.08 * avgLoss - 10, capped 0-40 dB */
    private fun computePrescribedGains(thresholds: List<Float>): FloatArray {
        val avg = thresholds.average().toFloat()
        return FloatArray(6) { i ->
            val hl = thresholds.getOrElse(i) { 0f }
            (hl * 0.46f + 0.08f * avg - 10f).coerceIn(0f, 40f)
        }
    }

    fun toggleAmplifier() {
        if (engine.isRunning) {
            engine.stop()
            _uiState.value = _uiState.value.copy(isRunning = false)
        } else {
            // Sync parameters before starting
            val state = _uiState.value
            engine.setBoostQuietSounds(state.boostQuietSounds / 100f)
            engine.setMasterGain(state.masterGain)
            engine.setLowBoost(state.lowBoost)
            engine.setHighBoost(state.highBoost)
            // Apply prescribed gains from profile if available
            state.activeProfile?.let { profile ->
                val prescribed = computePrescribedGains(profile.leftThresholdList())
                engine.setPrescribedGains(prescribed)
            } ?: run {
                engine.setPrescribedGains(FloatArray(6) { 10f })  // flat +10 dB default
            }
            engine.setCompressionParams(5f, 50f, 3f, -40f, 10f)
            engine.setExpansionParams(-60f, 2f)
            if (engine.start()) {
                _uiState.value = _uiState.value.copy(isRunning = true)
            }
        }
    }

    fun setBoostQuietSounds(percent: Int) {
        clearPresetOnManualChange()
        engine.setBoostQuietSounds(percent / 100f)
        _uiState.value = _uiState.value.copy(boostQuietSounds = percent)
    }

    fun setMasterGain(gain: Float) {
        clearPresetOnManualChange()
        engine.setMasterGain(gain)
        _uiState.value = _uiState.value.copy(masterGain = gain)
    }

    fun setLowBoost(db: Float) {
        clearPresetOnManualChange()
        engine.setLowBoost(db)
        _uiState.value = _uiState.value.copy(lowBoost = db)
    }

    fun setHighBoost(db: Float) {
        clearPresetOnManualChange()
        engine.setHighBoost(db)
        _uiState.value = _uiState.value.copy(highBoost = db)
    }

    override fun onCleared() {
        super.onCleared()
        micManager.stop()
        engine.release()
    }
}
