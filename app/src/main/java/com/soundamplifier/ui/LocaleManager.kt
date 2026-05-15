package com.soundamplifier.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleManager {
    const val PREFS_NAME = "smarthear_prefs"
    const val KEY_LANGUAGE = "app_language"
    const val LANG_EN = "en"
    const val LANG_HI = "hi"
    const val LANG_MR = "mr"
    const val LANG_TA = "ta"
    const val LANG_TE = "te"
    const val LANG_GU = "gu"

    fun getSavedLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANG_EN) ?: LANG_EN
    }

    /**
     * Call from [Application.onCreate] so the first [android.app.Activity] sees the correct locale
     * for string resources (including inside Compose).
     */
    fun syncAppCompatLocales(application: Application) {
        AppCompatDelegate.setApplicationLocales(localeListCompatForCode(getSavedLanguageCode(application)))
    }

    fun setLanguage(activity: Activity, languageCode: String) {
        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
        AppCompatDelegate.setApplicationLocales(localeListCompatForCode(languageCode))
    }

    private fun localeListCompatForCode(languageCode: String): LocaleListCompat {
        val tags = when (languageCode) {
            LANG_HI -> "hi-IN"
            LANG_MR -> "mr-IN"
            LANG_TA -> "ta-IN"
            LANG_TE -> "te-IN"
            LANG_GU -> "gu-IN"
            else -> "en"
        }
        return LocaleListCompat.forLanguageTags(tags)
    }

    fun applyPersistedLocale(context: Context): Context {
        return wrap(context, getSavedLanguageCode(context))
    }

    fun wrap(context: Context, languageCode: String): Context {
        val locale = localeForCode(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return context.createConfigurationContext(config)
    }

    private fun localeForCode(languageCode: String): Locale = when (languageCode) {
        LANG_HI -> Locale("hi", "IN")
        LANG_MR -> Locale("mr", "IN")
        LANG_TA -> Locale("ta", "IN")
        LANG_TE -> Locale("te", "IN")
        LANG_GU -> Locale("gu", "IN")
        else -> Locale.ENGLISH
    }
}
