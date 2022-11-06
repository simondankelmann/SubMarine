package de.simon.dankelmann.submarine.appcontext

import android.app.Application
import android.content.Context

class AppContext : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext.appContext = applicationContext
    }

    companion object {
        lateinit  var appContext: Context
    }
}