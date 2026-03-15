package com.soundamplifier.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundamplifier.audio.AudioEngine
import com.soundamplifier.audio.AudioProcessor
import com.soundamplifier.data.AudiogramProfile
import com.soundamplifier.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AmplifierUiState(
    val isRunning: Boolean = false,
    val noiseReduction: Int = 50,
    val masterGain: Float = 1.5f,
    val lowBoost: Float = 0f,    // dB
    val highBoost: Float = 0f,   // dB
    val activeProfile: AudiogramProfile? = null
)

class AmplifierViewModel(private val repository: ProfileRepository) : ViewModel() {

    val processor = AudioProcessor()
    val engine = AudioEngine(processor)

    private val _uiState = MutableStateFlow(AmplifierUiState())
    val uiState: StateFlow<AmplifierUiState> = _uiState

    init {
        loadLatestProfilePublic()
    }

    fun loadLatestProfilePublic() {
        viewModelScope.launch {
            val profile = repository.getLatestProfile()
            profile?.let { applyProfile(it) }
        }
    }

    fun applyProfile(profile: AudiogramProfile) {
        val gains = profile.leftGainList() // Use left ear as default; could average both
        processor.setGainsFromDb(gains)
        _uiState.value = _uiState.value.copy(activeProfile = profile)
    }

    fun toggleAmplifier() {
        if (engine.isRunning) {
            engine.stop()
            _uiState.value = _uiState.value.copy(isRunning = false)
        } else {
            engine.start()
            _uiState.value = _uiState.value.copy(isRunning = true)
        }
    }

    fun setNoiseReduction(level: Int) {
        processor.setNoiseReduction(level)
        _uiState.value = _uiState.value.copy(noiseReduction = level)
    }

    fun setMasterGain(gain: Float) {
        processor.masterGain = gain
        _uiState.value = _uiState.value.copy(masterGain = gain)
    }

    fun setLowBoost(db: Float) {
        // Apply to low frequency bands (0, 1)
        processor.bandGains[0] = dbToLinear(db)
        processor.bandGains[1] = dbToLinear(db)
        _uiState.value = _uiState.value.copy(lowBoost = db)
    }

    fun setHighBoost(db: Float) {
        // Apply to high frequency bands (4, 5)
        processor.bandGains[4] = dbToLinear(db)
        processor.bandGains[5] = dbToLinear(db)
        _uiState.value = _uiState.value.copy(highBoost = db)
    }

    private fun dbToLinear(db: Float): Float =
        Math.pow(10.0, db / 20.0).toFloat()

    override fun onCleared() {
        super.onCleared()
        engine.stop()
    }
}
