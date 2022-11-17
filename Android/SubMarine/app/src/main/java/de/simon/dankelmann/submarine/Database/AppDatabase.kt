package de.simon.dankelmann.submarine.Database

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Dao.LocationDao
import de.simon.dankelmann.submarine.Dao.SignalDao
import de.simon.dankelmann.submarine.Entities.LocationEntity
import de.simon.dankelmann.submarine.Entities.SignalEntity
import java.io.*
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.*


/*
@Database(entities = [LocationEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}
*/

@Database(
    entities = [LocationEntity::class, SignalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun signalDao(): SignalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private var _logTag = "AppDatabase"
        private val _dbDirectoryPath = Environment.getExternalStorageDirectory().absolutePath + "/Submarine/Database/"
        private val _dbFileName = "submarine-database.db"

        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                var directory = File(_dbDirectoryPath)
                if(!directory.exists()){
                    directory.mkdirs()
                }

                synchronized(this) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        _dbDirectoryPath + _dbFileName
                    ).build()
                }
            }
            return INSTANCE!!
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun exportToFile():Long{
            var scrFile = File(getDatabase(AppContext.getContext()).openHelper.writableDatabase.path)

            var destinationDirectoryPath = _dbDirectoryPath + "/Backup/"
            // CREATE THE BACKUP LOCATION IF IT DOES NOT EXIST
            var destinationDirectory = File(destinationDirectoryPath)
            if(!destinationDirectory.exists()){
                destinationDirectory.mkdir()
            }

            // CREATE A BACKUP FILENAME
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            var destinationFileName = "submarine_exportedDb_"+timeStamp+".db"
            var destinationFilePath = destinationDirectoryPath + destinationFileName

            var destinationFile = File(destinationFilePath)
            // CREATE THE EMPTY FILE IF IT DOES NOT EXIST
            if(!destinationFile.exists()){
                destinationFile.createNewFile()
            }
            // COPY THE FILES CONTENT
            Files.copy(scrFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

            destinationFile = File(destinationFilePath)
            return destinationFile.length()
        }
    }
}