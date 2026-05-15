package com.soundamplifier.audio

import androidx.annotation.StringRes
import com.soundamplifier.R

data class AmplifierPreset(
    val id: String,
    @StringRes val nameRes: Int,
    val icon: String,
    val boostQuietSounds: Float,
    val masterGain: Float,
    val lowBoostDb: Float,
    val highBoostDb: Float
)

object Presets {
    val CONVERSATION = AmplifierPreset(
        id = "conversation",
        nameRes = R.string.preset_conversation,
        icon = "chat",
        boostQuietSounds = 0.7f,
        masterGain = 2.0f,
        lowBoostDb = -3f,
        highBoostDb = 6f
    )
    val MUSIC = AmplifierPreset(
        id = "music",
        nameRes = R.string.preset_music,
        icon = "music_note",
        boostQuietSounds = 0.3f,
        masterGain = 1.5f,
        lowBoostDb = 4f,
        highBoostDb = 2f
    )
    val OUTDOORS = AmplifierPreset(
        id = "outdoors",
        nameRes = R.string.preset_outdoors,
        icon = "park",
        boostQuietSounds = 0.5f,
        masterGain = 2.5f,
        lowBoostDb = -6f,
        highBoostDb = 4f
    )
    val CLASSROOM = AmplifierPreset(
        id = "classroom",
        nameRes = R.string.preset_classroom,
        icon = "school",
        boostQuietSounds = 0.85f,
        masterGain = 3.0f,
        lowBoostDb = -2f,
        highBoostDb = 8f
    )
    val TV = AmplifierPreset(
        id = "tv",
        nameRes = R.string.preset_tv,
        icon = "tv",
        boostQuietSounds = 0.6f,
        masterGain = 2.0f,
        lowBoostDb = 0f,
        highBoostDb = 5f
    )

    val ALL = listOf(CONVERSATION, MUSIC, OUTDOORS, CLASSROOM, TV)
}
