package com.soundamplifier.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * Local Room/Firebase sync scope for hearing data. Signed-in users use [FirebaseUser.getUid].
 * [GUEST] is retained for legacy Room rows and auth handoff; app entry requires sign-in.
 */
object AccountLocalIds {
    const val GUEST = "__guest__"

    /** Current local data partition (Firebase uid or [GUEST] when signed out — avoid using when signed out). */
    @Suppress("UNUSED_PARAMETER")
    fun localKey(context: Context): String {
        FirebaseAuth.getInstance().currentUser?.uid?.let { return it }
        return GUEST
    }
}
