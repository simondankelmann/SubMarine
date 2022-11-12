package de.simon.dankelmann.submarine.Services

import android.content.Context
import de.simon.dankelmann.submarine.Database.AppDatabase

class DatabaseService (){
    companion object {
        fun getDatabase(appContext:Context): AppDatabase {
            //return Room.databaseBuilder(appContext, AppDatabase::class.java, "submarine-database").build()
            return AppDatabase.getDatabase(appContext)
        }
    }
}