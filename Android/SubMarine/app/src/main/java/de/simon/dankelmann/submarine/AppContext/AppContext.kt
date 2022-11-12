package de.simon.dankelmann.submarine.AppContext

import android.content.Context
import de.simon.dankelmann.submarine.Services.SubMarineService

/*
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
*/

abstract class AppContext {
    companion object {

        private lateinit var context: Context
        var submarineService: SubMarineService = SubMarineService()


        fun setContext(con: Context) {
            context=con
        }

        fun getContext():Context {
            return context
        }

    }
}