package com.soundamplifier.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.math.abs

@Entity(
    tableName = "custom_presets",
    indices = [Index(value = ["accountId", "createdAt"])],
)
data class CustomPreset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** [AccountLocalIds.localKey] — Firebase uid or [AccountLocalIds.GUEST]. */
    val accountId: String = AccountLocalIds.GUEST,
    val name: String,
    val boostQuietSounds: Float,   // 0.0 - 1.0
    val masterGain: Float,          // 1.0 - 5.0
    val lowBoostDb: Float,
    val highBoostDb: Float,
    val createdAt: Long = System.currentTimeMillis(),
    /** Icon key for UI (see PresetIcons). */
    val iconKey: String = "tune",
    /** If set, this row overrides the built-in preset with the same id (e.g. `conversation`). */
    val builtInPresetId: String? = null,
) {
    private companion object {
        const val EPS = 1e-4f
    }

    fun sameSignatureAs(other: CustomPreset): Boolean =
        name == other.name &&
            abs(boostQuietSounds - other.boostQuietSounds) < EPS &&
            abs(masterGain - other.masterGain) < EPS &&
            abs(lowBoostDb - other.lowBoostDb) < EPS &&
            abs(highBoostDb - other.highBoostDb) < EPS
}
