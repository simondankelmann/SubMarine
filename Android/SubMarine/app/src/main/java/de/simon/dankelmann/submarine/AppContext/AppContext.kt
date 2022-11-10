package de.simon.dankelmann.submarine.AppContext

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import de.simon.dankelmann.submarine.services.SubMarineService

class AppContext: Application() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        instance = this
        //submarineService = SubMarineService()
    }

    companion object {
        lateinit var instance: AppContext
            private set
        var submarineService: SubMarineService = SubMarineService()
    }
}