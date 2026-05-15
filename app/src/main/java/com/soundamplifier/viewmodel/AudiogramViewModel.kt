package com.soundamplifier.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.soundamplifier.audio.AudiogramEar
import com.soundamplifier.audio.AudiogramTest
import com.soundamplifier.data.AccountLocalIds
import com.soundamplifier.data.AudiogramProfile
import com.soundamplifier.data.FirestoreUserRepository
import com.soundamplifier.data.ProfileRepository
import com.soundamplifier.data.thresholdsToGains
import com.soundamplifier.data.toFirestoreEarMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TestPhase {
    INTRO,
    TESTING_LEFT,
    TRANSITION,
    TESTING_RIGHT,
    COMPLETE,
}

enum class Ear {
    LEFT,
    RIGHT,
}

enum class StepDirection {
    UP,
    DOWN,
}

data class HearingTestUiState(
    val phase: TestPhase = TestPhase.INTRO,
    val currentEar: Ear = Ear.LEFT,
    val currentFrequencyIndex: Int = 0,
    val currentDbLevel: Int = 40,
    val leftThresholds: List<Int> = List(6) { -1 },
    val rightThresholds: List<Int> = List(6) { -1 },
    val isPlayingTone: Boolean = false,
    val responseEnabled: Boolean = false,
    val lastDirection: StepDirection? = null,
    val reversalCount: Int = 0,
    /** Steps (user taps) taken at the current frequency — drives per-frequency dots / sub-progress. */
    val volumeStepCount: Int = 0,
    /** Incremented on every response tap so Compose can run tap feedback animation. */
    val interactionEpoch: Int = 0,
    /** Shown over testing until the user dismisses or it auto-hides. */
    val showInstructionOverlay: Boolean = false,
)

class AudiogramViewModel(
    private val repository: ProfileRepository,
    appContext: Context,
) : ViewModel() {

    private val appContext = appContext.applicationContext

    private val firestoreUserRepository = FirestoreUserRepository()
    private val audiogramTest = AudiogramTest()

    private val _uiState = MutableStateFlow(HearingTestUiState())
    val uiState: StateFlow<HearingTestUiState> = _uiState

    private var toneJob: Job? = null
    private val heardCountAtLevel = mutableMapOf<Int, Int>()
    private var volumeStepCount: Int = 0

    private val maxDbHl = 80
    private val toneDelayAfterUserResponseMs = 420L

    companion object {
        /** Matches bracketing cap in logic; used only for progress interpolation in UI. */
        const val MAX_VOLUME_STEPS_PER_FREQ_FOR_UI = 28
    }

    private val maxStepsPerFrequency = 28

    fun prepareRetake() {
        toneJob?.cancel()
        audiogramTest.stopTone()
        heardCountAtLevel.clear()
        volumeStepCount = 0
        _uiState.value = HearingTestUiState()
    }

    fun startTest() {
        toneJob?.cancel()
        audiogramTest.stopTone()
        heardCountAtLevel.clear()
        volumeStepCount = 0
        _uiState.value = HearingTestUiState(
            phase = TestPhase.TESTING_LEFT,
            currentEar = Ear.LEFT,
            currentFrequencyIndex = 0,
            currentDbLevel = 40,
            leftThresholds = List(6) { -1 },
            rightThresholds = List(6) { -1 },
            lastDirection = null,
            reversalCount = 0,
            isPlayingTone = false,
            responseEnabled = false,
            volumeStepCount = 0,
            interactionEpoch = 0,
            showInstructionOverlay = true,
        )
        presentCurrentTone(delayBeforePlayMs = 0L)
    }

    fun dismissInstruction() {
        _uiState.update { it.copy(showInstructionOverlay = false) }
    }

    fun continueAfterLeftEar() {
        if (_uiState.value.phase != TestPhase.TRANSITION) return
        heardCountAtLevel.clear()
        volumeStepCount = 0
        _uiState.value = _uiState.value.copy(
            phase = TestPhase.TESTING_RIGHT,
            currentEar = Ear.RIGHT,
            currentFrequencyIndex = 0,
            currentDbLevel = 40,
            lastDirection = null,
            reversalCount = 0,
            responseEnabled = false,
            isPlayingTone = false,
            volumeStepCount = 0,
        )
        presentCurrentTone(delayBeforePlayMs = 0L)
    }

    fun userHeard() {
        val state = _uiState.value
        if (!state.responseEnabled ||
            (state.phase != TestPhase.TESTING_LEFT && state.phase != TestPhase.TESTING_RIGHT)
        ) return
        toneJob?.cancel()
        audiogramTest.stopTone()

        val level = state.currentDbLevel
        val count = (heardCountAtLevel[level] ?: 0) + 1
        heardCountAtLevel[level] = count
        val nextEpoch = state.interactionEpoch + 1

        if (count >= 2) {
            _uiState.value = state.copy(interactionEpoch = nextEpoch)
            recordThreshold(level)
            return
        }

        volumeStepCount++
        if (volumeStepCount >= maxStepsPerFrequency) {
            _uiState.value = state.copy(interactionEpoch = nextEpoch)
            recordThreshold(level.coerceIn(0, maxDbHl))
            return
        }

        val newDir = StepDirection.DOWN
        val newRev = if (state.lastDirection == StepDirection.UP) state.reversalCount + 1 else state.reversalCount
        val nextDb = (level - 10).coerceAtLeast(0)

        _uiState.value = state.copy(
            currentDbLevel = nextDb,
            lastDirection = newDir,
            reversalCount = newRev,
            responseEnabled = false,
            isPlayingTone = false,
            volumeStepCount = volumeStepCount,
            interactionEpoch = nextEpoch,
        )
        presentCurrentTone(delayBeforePlayMs = toneDelayAfterUserResponseMs)
    }

    fun userCannotHear() {
        val state = _uiState.value
        if (!state.responseEnabled ||
            (state.phase != TestPhase.TESTING_LEFT && state.phase != TestPhase.TESTING_RIGHT)
        ) return
        toneJob?.cancel()
        audiogramTest.stopTone()

        val nextEpoch = state.interactionEpoch + 1
        volumeStepCount++
        if (volumeStepCount >= maxStepsPerFrequency) {
            _uiState.value = state.copy(interactionEpoch = nextEpoch)
            recordThreshold(state.currentDbLevel.coerceIn(0, maxDbHl))
            return
        }

        val newDir = StepDirection.UP
        val newRev = if (state.lastDirection == StepDirection.DOWN) state.reversalCount + 1 else state.reversalCount
        val nextDb = (state.currentDbLevel + 5).coerceAtMost(maxDbHl)

        _uiState.value = state.copy(
            currentDbLevel = nextDb,
            lastDirection = newDir,
            reversalCount = newRev,
            responseEnabled = false,
            isPlayingTone = false,
            volumeStepCount = volumeStepCount,
            interactionEpoch = nextEpoch,
        )
        presentCurrentTone(delayBeforePlayMs = toneDelayAfterUserResponseMs)
    }

    private fun recordThreshold(thresholdDb: Int) {
        val state = _uiState.value
        val epoch = state.interactionEpoch
        val idx = state.currentFrequencyIndex
        val th = thresholdDb.coerceIn(0, maxDbHl)

        val left = List(6) { i -> state.leftThresholds[i] }
        val right = List(6) { i -> state.rightThresholds[i] }
        val nextLeft = if (state.currentEar == Ear.LEFT) {
            left.toMutableList().apply { set(idx, th) }
        } else left
        val nextRight = if (state.currentEar == Ear.RIGHT) {
            right.toMutableList().apply { set(idx, th) }
        } else right

        val nextFreq = idx + 1
        val freqs = audiogramTest.frequencies
        if (nextFreq < freqs.size) {
            heardCountAtLevel.clear()
            volumeStepCount = 0
            _uiState.value = state.copy(
                leftThresholds = nextLeft,
                rightThresholds = nextRight,
                currentFrequencyIndex = nextFreq,
                currentDbLevel = 40,
                lastDirection = null,
                reversalCount = 0,
                responseEnabled = false,
                isPlayingTone = false,
                volumeStepCount = 0,
                interactionEpoch = epoch,
            )
            presentCurrentTone(delayBeforePlayMs = toneDelayAfterUserResponseMs)
            return
        }

        if (state.currentEar == Ear.LEFT) {
            heardCountAtLevel.clear()
            volumeStepCount = 0
            _uiState.value = state.copy(
                leftThresholds = nextLeft,
                rightThresholds = nextRight,
                phase = TestPhase.TRANSITION,
                currentFrequencyIndex = 0,
                currentDbLevel = 40,
                responseEnabled = false,
                isPlayingTone = false,
                volumeStepCount = 0,
                interactionEpoch = epoch,
            )
            return
        }

        _uiState.value = state.copy(
            leftThresholds = nextLeft,
            rightThresholds = nextRight,
            phase = TestPhase.COMPLETE,
            responseEnabled = false,
            isPlayingTone = false,
            interactionEpoch = epoch,
        )
    }

    private fun presentCurrentTone(delayBeforePlayMs: Long) {
        val state = _uiState.value
        if (state.phase != TestPhase.TESTING_LEFT && state.phase != TestPhase.TESTING_RIGHT) return

        toneJob?.cancel()
        toneJob = viewModelScope.launch {
            if (delayBeforePlayMs > 0) {
                delay(delayBeforePlayMs)
            }
            _uiState.value = _uiState.value.copy(isPlayingTone = true, responseEnabled = false)
            val s = _uiState.value
            val ear = if (s.phase == TestPhase.TESTING_LEFT) AudiogramEar.LEFT else AudiogramEar.RIGHT
            val hz = audiogramTest.frequencies[s.currentFrequencyIndex]
            try {
                audiogramTest.playToneBurst(hz, s.currentDbLevel, ear)
            } catch (e: Exception) {
                Log.e("AudiogramViewModel", "Tone burst failed", e)
            }
            _uiState.value = _uiState.value.copy(isPlayingTone = false, responseEnabled = true)
        }
    }

    /** Persist audiogram only (Room + Firestore). Call from MainActivity after COMPLETE. */
    suspend fun persistAudiogram(left: List<Float>, right: List<Float>) {
        val leftGains = thresholdsToGains(left)
        val rightGains = thresholdsToGains(right)
        val accountId = AccountLocalIds.localKey(appContext)
        val profile = AudiogramProfile(
            accountId = accountId,
            leftEarThresholds = left.joinToString(",") { it.toString() },
            rightEarThresholds = right.joinToString(",") { it.toString() },
            leftEarGains = leftGains.joinToString(",") { it.toString() },
            rightEarGains = rightGains.joinToString(",") { it.toString() },
        )
        try {
            repository.saveProfile(profile)
        } catch (e: Exception) {
            Log.e("AudiogramViewModel", "Local audiogram save failed", e)
            throw e
        }
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        try {
            firestoreUserRepository.saveAudiogram(
                firebaseUser.uid,
                left.toFirestoreEarMap(),
                right.toFirestoreEarMap(),
            )
        } catch (e: Exception) {
            Log.e("AudiogramViewModel", "Firestore audiogram sync failed", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        toneJob?.cancel()
        audiogramTest.stopTone()
    }
}
