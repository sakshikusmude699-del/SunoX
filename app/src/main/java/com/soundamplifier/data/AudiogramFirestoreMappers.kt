package com.soundamplifier.data

import kotlin.math.roundToInt

/** Maps hearing thresholds to Firestore-style frequency key → dB HL (Int). */
fun List<Float>.toFirestoreEarMap(): Map<String, Int> =
    AUDIOGRAM_FREQUENCIES.mapIndexed { i, hz ->
        hz.toString() to this[i].roundToInt().coerceIn(0, 120)
    }.toMap()

fun AudiogramProfile.toFirestoreEarMaps(): Pair<Map<String, Int>, Map<String, Int>> =
    leftThresholdList().toFirestoreEarMap() to rightThresholdList().toFirestoreEarMap()

fun normalizeFirestoreEarField(raw: Any?): Map<String, Int>? {
    if (raw !is Map<*, *>) return null
    val out = mutableMapOf<String, Int>()
    for ((k, v) in raw) {
        val key = k?.toString() ?: continue
        val intVal = when (v) {
            is Number -> v.toInt()
            else -> continue
        }
        out[key] = intVal.coerceIn(0, 120)
    }
    return out
}

fun Map<String, Any>.toAudiogramProfileFromFirestore(): AudiogramProfile? {
    val left = normalizeFirestoreEarField(this["leftEar"]) ?: return null
    val right = normalizeFirestoreEarField(this["rightEar"]) ?: return null
    val leftValues = AUDIOGRAM_FREQUENCIES.map { freq ->
        left[freq.toString()]?.toFloat() ?: return null
    }
    val rightValues = AUDIOGRAM_FREQUENCIES.map { freq ->
        right[freq.toString()]?.toFloat() ?: return null
    }
    val leftGains = thresholdsToGains(leftValues)
    val rightGains = thresholdsToGains(rightValues)
    return AudiogramProfile(
        leftEarThresholds = leftValues.joinToString(","),
        rightEarThresholds = rightValues.joinToString(","),
        leftEarGains = leftGains.joinToString(","),
        rightEarGains = rightGains.joinToString(","),
    )
}

fun firestoreAudiogramMapsMatchDocument(
    doc: Map<String, Any>,
    leftEar: Map<String, Int>,
    rightEar: Map<String, Int>,
): Boolean {
    val dLeft = normalizeFirestoreEarField(doc["leftEar"]) ?: return false
    val dRight = normalizeFirestoreEarField(doc["rightEar"]) ?: return false
    return AUDIOGRAM_FREQUENCIES.all { hz ->
        val key = hz.toString()
        dLeft[key] == leftEar[key] && dRight[key] == rightEar[key]
    }
}
