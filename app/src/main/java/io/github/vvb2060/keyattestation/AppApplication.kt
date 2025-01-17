package io.github.vvb2060.keyattestation

import android.app.Application
import rikka.html.text.HtmlCompat
import rikka.material.app.DayNightDelegate

class AppApplication : Application() {
    companion object {
        const val TAG = "KeyAttestation"

        @JvmStatic
        lateinit var App: Application private set
    }

    init {
        App = this
        DayNightDelegate.setApplicationContext(this)
        DayNightDelegate.setDefaultNightMode(DayNightDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        HtmlCompat.setContext(this)
    }
}
