package de.simon.dankelmann.submarine.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.submarine.Entities.LocationEntity
import de.simon.dankelmann.submarine.Entities.SignalEntity

@Dao
interface LocationDao {
    @Query("SELECT * FROM locationentity")
    fun getAll(): List<LocationEntity>

    @Query("SELECT * FROM locationentity WHERE uid IN (:locationIds)")
    fun loadAllByIds(locationIds: IntArray): List<LocationEntity>

    @Query("SELECT * FROM locationentity WHERE uid = :locationId LIMIT 1")
    fun getById(locationId: Int): LocationEntity


    /*
    @Query("SELECT * FROM locationentity WHERE uid = locationId LIMIT 1")
    fun findById(locationId: Int): LocationEntity*/

    @Insert
    fun insertAll(vararg locationEntities: LocationEntity)

    @Insert
    suspend fun insertItem(location: LocationEntity): Long

    @Delete
    fun delete(location: LocationEntity)
}