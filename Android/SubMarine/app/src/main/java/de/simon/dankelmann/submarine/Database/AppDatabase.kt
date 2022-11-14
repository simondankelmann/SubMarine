package de.simon.dankelmann.submarine.Database

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.simon.dankelmann.submarine.Dao.LocationDao
import de.simon.dankelmann.submarine.Dao.SignalDao
import de.simon.dankelmann.submarine.Entities.LocationEntity
import de.simon.dankelmann.submarine.Entities.SignalEntity
import java.io.*
import java.nio.channels.FileChannel
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

        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "submarine-database.db"
                    ).build()                }
            }
            return INSTANCE!!
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun exportToSdCard(){
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            //var destinationPath = Environment.getExternalStorageDirectory().absolutePath + "/exportedDb_"+timeStamp+".db"
            var destinationPath = Environment.getDataDirectory().absolutePath + "/test.db"
            var scrFile:File = File(INSTANCE!!.openHelper.writableDatabase.path)
            var destinationFile = File(destinationPath)

            Log.d(_logTag,"SRC: " + scrFile)
            if(!scrFile.exists()){
                Log.d(_logTag,"SRC NON EXISTENT ")
            }



            Log.d(_logTag,"DEST: " + destinationPath)
            if(!destinationFile.exists()){
                Log.d(_logTag,"DEST NON EXISTENT ")
                destinationFile.createNewFile()
            }

            Log.d(_logTag,"TRYING TO COPY")
            scrFile.copyTo(destinationFile, true)
        }

        /*
        @Throws(IOException::class)
        private fun exportFile(src: File, dst: File): File? {

            //if folder does not exist
            if (!dst.exists()) {
                if (!dst.mkdir()) {
                    return null
                }
            }
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val expFile = File(dst.path + File.separator + "IMG_" + timeStamp + ".jpg")
            var inChannel: FileChannel? = null
            var outChannel: FileChannel? = null
            try {
                inChannel = FileInputStream(src).channel
                outChannel = FileOutputStream(expFile).channel
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            try {
                inChannel!!.transferTo(0, inChannel.size(), outChannel)
            } finally {
                inChannel?.close()
                outChannel?.close()
            }
            return expFile
        }*/
    }
}