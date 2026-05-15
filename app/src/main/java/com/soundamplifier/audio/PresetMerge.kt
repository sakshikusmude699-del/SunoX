package com.soundamplifier.audio

import com.soundamplifier.data.CustomPreset

fun CustomPreset.toAmplifierOverride(base: AmplifierPreset): AmplifierPreset =
    base.copy(
        boostQuietSounds = boostQuietSounds,
        masterGain = masterGain,
        lowBoostDb = lowBoostDb,
        highBoostDb = highBoostDb,
        icon = iconKey,
    )

/** Applies Room/Firestore overrides to the canonical built-in list. */
fun mergeBuiltInsWithOverrides(stored: List<CustomPreset>): List<AmplifierPreset> {
    val byBuiltIn = stored.mapNotNull { p -> p.builtInPresetId?.let { it to p } }.toMap()
    return Presets.ALL.map { base -> byBuiltIn[base.id]?.toAmplifierOverride(base) ?: base }
}
