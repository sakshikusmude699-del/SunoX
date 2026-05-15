package com.soundamplifier.data

import android.content.Context

/**
 * After a successful sign-in, the home "Open Amplifier" entry can be gated until Firestore has an
 * audiogram. [markPendingAfterSignIn] enables that flow; [clearPending] ends it when the user
 * leaves Home or when an audiogram is detected.
 */
object FirstLoginHomePreferences {
    private const val PREFS_FILE = "smarthear_prefs"
    private const val KEY_PENDING_PREFIX = "first_login_home_open_amp_pending_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private fun key(uid: String) = "$KEY_PENDING_PREFIX$uid"

    fun markPendingAfterSignIn(context: Context, uid: String) {
        prefs(context).edit().putBoolean(key(uid), true).apply()
    }

    fun isPending(context: Context, uid: String): Boolean =
        prefs(context).getBoolean(key(uid), false)

    fun clearPending(context: Context, uid: String) {
        prefs(context).edit().remove(key(uid)).apply()
    }
}
