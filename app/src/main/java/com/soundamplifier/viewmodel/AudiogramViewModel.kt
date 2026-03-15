package com.soundamplifier.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundamplifier.audio.AudiogramTest
import com.soundamplifier.data.AudiogramProfile
import com.soundamplifier.data.ProfileRepository
import com.soundamplifier.data.thresholdsToGains
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class TestEar { LEFT, RIGHT }
enum class TestState { IDLE, TESTING, DONE }

data class AudiogramUiState(
    val testState: TestState = TestState.IDLE,
    val currentEar: TestEar = TestEar.LEFT,
    val currentFrequencyIndex: Int = 0,
    val currentLevelIndex: Int = 0,
    val leftThresholds: MutableList<Float> = MutableList(6) { 0f },
    val rightThresholds: MutableList<Float> = MutableList(6) { 0f },
    val message: String = "Press Start to begin the hearing test"
)

class AudiogramViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val audiogramTest = AudiogramTest()
    private val _uiState = MutableStateFlow(AudiogramUiState())
    val uiState: StateFlow<AudiogramUiState> = _uiState

    private var toneJob: Job? = null

    fun startTest() {
        _uiState.value = AudiogramUiState(
            testState = TestState.TESTING,
            message = "Testing LEFT ear — put on headphones in a quiet place"
        )
        playNextTone()
    }

    /** Called when user taps "I hear it" */
    fun userHeard() {
        val state = _uiState.value
        if (state.testState != TestState.TESTING) return

        toneJob?.cancel()
        audiogramTest.stopTone()

        // Record threshold at current level
        val thresholdDb = AudiogramTest.TEST_LEVELS_DB[state.currentLevelIndex]
        recordThreshold(state, thresholdDb)
    }

    /** Called when user taps "Can't hear" — move to louder level */
    fun userCannotHear() {
        val state = _uiState.value
        if (state.testState != TestState.TESTING) return

        toneJob?.cancel()
        audiogramTest.stopTone()

        val nextLevel = state.currentLevelIndex + 1
        if (nextLevel >= AudiogramTest.TEST_LEVELS_DB.size) {
            // Max level reached, record as max threshold
            recordThreshold(state, AudiogramTest.TEST_LEVELS_DB.last())
        } else {
            _uiState.value = state.copy(currentLevelIndex = nextLevel)
            playNextTone()
        }
    }

    private fun recordThreshold(state: AudiogramUiState, thresholdDb: Float) {
        // Convert dBFS to approximate dB HL (hearing level) — simplified offset
        val thresholdHL = (-thresholdDb).coerceIn(0f, 120f)

        if (state.currentEar == TestEar.LEFT) {
            state.leftThresholds[state.currentFrequencyIndex] = thresholdHL
        } else {
            state.rightThresholds[state.currentFrequencyIndex] = thresholdHL
        }

        advanceTest(state)
    }

    private fun advanceTest(state: AudiogramUiState) {
        val nextFreqIndex = state.currentFrequencyIndex + 1

        if (nextFreqIndex < AudiogramTest.FREQUENCIES.size) {
            _uiState.value = state.copy(
                currentFrequencyIndex = nextFreqIndex,
                currentLevelIndex = 0,
                message = "Testing ${state.currentEar.name} ear — ${AudiogramTest.FREQUENCIES[nextFreqIndex]} Hz"
            )
            playNextTone()
        } else if (state.currentEar == TestEar.LEFT) {
            // Switch to right ear
            _uiState.value = state.copy(
                currentEar = TestEar.RIGHT,
                currentFrequencyIndex = 0,
                currentLevelIndex = 0,
                message = "Now testing RIGHT ear"
            )
            playNextTone()
        } else {
            // Test complete
            _uiState.value = state.copy(
                testState = TestState.DONE,
                message = "Test complete! Your profile has been saved."
            )
            saveProfile(state)
        }
    }

    private fun playNextTone() {
        val state = _uiState.value
        val freq = AudiogramTest.FREQUENCIES[state.currentFrequencyIndex]
        val level = AudiogramTest.TEST_LEVELS_DB[state.currentLevelIndex]

        toneJob = viewModelScope.launch {
            audiogramTest.playTone(freq, level)
        }
    }

    private fun saveProfile(state: AudiogramUiState) {
        val leftGains = thresholdsToGains(state.leftThresholds)
        val rightGains = thresholdsToGains(state.rightThresholds)

        val profile = AudiogramProfile(
            leftEarThresholds = state.leftThresholds.joinToString(","),
            rightEarThresholds = state.rightThresholds.joinToString(","),
            leftEarGains = leftGains.joinToString(","),
            rightEarGains = rightGains.joinToString(",")
        )

        viewModelScope.launch { repository.saveProfile(profile) }
    }

    override fun onCleared() {
        super.onCleared()
        audiogramTest.stopTone()
    }
}
