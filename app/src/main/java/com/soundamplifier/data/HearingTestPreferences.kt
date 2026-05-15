package com.soundamplifier.data

import android.content.Context

/** Hearing-test prompt completion, keyed by [AccountLocalIds.localKey] so each account is independent. */
object HearingTestPreferences {
    private const val PREFS_FILE = "smarthear_prefs"
    private const val KEY_COMPLETED_PREFIX = "hearing_test_completed_"
    private const val KEY_DISMISSED_PREFIX = "hearing_test_dismissed_"
    private const val KEY_COMPLETED_LEGACY = "hearing_test_completed"
    private const val KEY_DISMISSED_LEGACY = "hearing_test_dismissed"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private fun completedKey(accountId: String) = "$KEY_COMPLETED_PREFIX$accountId"

    private fun dismissedKey(accountId: String) = "$KEY_DISMISSED_PREFIX$accountId"

    fun isCompleted(context: Context, accountId: String): Boolean {
        val p = prefs(context)
        val k = completedKey(accountId)
        if (p.getBoolean(k, false)) return true
        if (accountId == AccountLocalIds.GUEST && !p.contains(k) && p.contains(KEY_COMPLETED_LEGACY)) {
            val legacy = p.getBoolean(KEY_COMPLETED_LEGACY, false)
            if (legacy) {
                p.edit().putBoolean(k, true).remove(KEY_COMPLETED_LEGACY).apply()
            }
            return legacy
        }
        return false
    }

    fun setCompleted(context: Context, accountId: String) {
        prefs(context).edit()
            .putBoolean(completedKey(accountId), true)
            .remove(KEY_COMPLETED_LEGACY)
            .apply()
    }

    fun isDismissedForever(context: Context, accountId: String): Boolean {
        val p = prefs(context)
        val k = dismissedKey(accountId)
        if (p.getBoolean(k, false)) return true
        if (accountId == AccountLocalIds.GUEST && !p.contains(k) && p.contains(KEY_DISMISSED_LEGACY)) {
            val legacy = p.getBoolean(KEY_DISMISSED_LEGACY, false)
            if (legacy) {
                p.edit().putBoolean(k, true).remove(KEY_DISMISSED_LEGACY).apply()
            }
            return legacy
        }
        return false
    }

    fun setDismissedForever(context: Context, accountId: String) {
        prefs(context).edit()
            .putBoolean(dismissedKey(accountId), true)
            .remove(KEY_DISMISSED_LEGACY)
            .apply()
    }
}
