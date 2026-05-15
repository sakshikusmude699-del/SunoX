package com.soundamplifier.audio

data class MicSourceOption(
    val type: MicSourceType,
    val label: String,
    val deviceId: Int?,          // null => system default
    val isAvailable: Boolean
)

