package com.soundamplifier

import android.app.Application
import com.soundamplifier.ui.LocaleManager

class SunoXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LocaleManager.syncAppCompatLocales(this)
    }
}
