package com.soundamplifier.ui.preset

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector

object PresetIcons {
    val choices: List<Pair<String, ImageVector>> = listOf(
        "chat" to Icons.Rounded.Chat,
        "music_note" to Icons.Rounded.MusicNote,
        "park" to Icons.Rounded.Park,
        "headphones" to Icons.Rounded.Headphones,
        "phone_android" to Icons.Rounded.PhoneAndroid,
        "home" to Icons.Rounded.Home,
        "mic" to Icons.Rounded.Mic,
        "favorite" to Icons.Rounded.Favorite,
        "directions_car" to Icons.Rounded.DirectionsCar,
        "apartment" to Icons.Rounded.Apartment,
        "school" to Icons.Rounded.School,
        "tv" to Icons.Rounded.Tv,
        "hearing" to Icons.Rounded.Hearing,
        "tune" to Icons.Rounded.Tune,
    )

    private val byKey = choices.toMap()

    fun vector(key: String): ImageVector = byKey[key] ?: Icons.Rounded.Tune
}
