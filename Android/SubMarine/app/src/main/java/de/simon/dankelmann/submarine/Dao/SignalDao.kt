package de.simon.dankelmann.submarine.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.submarine.Entities.LocationEntity
import de.simon.dankelmann.submarine.Entities.SignalEntity

@Dao
interface SignalDao {
    @Query("SELECT * FROM signalentity")
    fun getAll(): List<SignalEntity>

    @Query("SELECT * FROM signalentity WHERE uid IN (:singalIds)")
    fun loadAllByIds(singalIds: IntArray): List<SignalEntity>

    @Query("SELECT * FROM signalentity WHERE uid = :singalId LIMIT 1")
    fun getById(singalId: Int): SignalEntity

    @Query("SELECT * FROM signalentity WHERE name LIKE :searchString")
    fun search(searchString: String): List<SignalEntity>

    /*
    @Query("SELECT * FROM locationentity WHERE uid = locationId LIMIT 1")
    fun findById(locationId: Int): LocationEntity
    */

    @Query("UPDATE signalentity SET proofofwork = :pow WHERE uid = :singalId")
    fun updateProofOfWork(singalId: Int, pow:Boolean)

    @Query("UPDATE signalentity SET name = :name , signalData = :signalData, signalDataLength = :signalDataLength WHERE uid = :singalId")
    fun updateValues(singalId: Int, name:String, signalData:String, signalDataLength: Int)

    @Insert
    fun insertAll(vararg signalEntities: SignalEntity)

    @Insert
    suspend fun insertItem(signal: SignalEntity): Long

    @Delete
    fun delete(signal: SignalEntity)
}