package de.simon.dankelmann.submarine.AppContext

import android.app.Application

class AppContext: Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AppContext
            private set
    }
}