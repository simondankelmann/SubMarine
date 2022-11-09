package de.simon.dankelmann.submarine.services

import android.content.Context
import androidx.room.Room
import de.simon.dankelmann.submarine.Database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class DatabaseService (){
    companion object {
        fun getDatabase(appContext:Context): AppDatabase {
            //return Room.databaseBuilder(appContext, AppDatabase::class.java, "submarine-database").build()
            return AppDatabase.getDatabase(appContext)
        }
    }
}