package de.simon.dankelmann.submarine.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity
data class SignalEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int,

    // GENERAL
    @ColumnInfo(name = "name") var name: String?,
    @ColumnInfo(name = "tag") var tag: String?,
    @ColumnInfo(name = "locationid") var locationId: Int?,
    @ColumnInfo(name = "recordingdate") var recordingDate: Int?,
    @ColumnInfo(name = "type") var type: String?,

    // ADAPTER SETTINGS
    @ColumnInfo(name = "frequency") var frequency: Float?,
    @ColumnInfo(name = "modulation") var modulation: Int?,
    @ColumnInfo(name = "drate") var dRate: Int?,
    @ColumnInfo(name = "rxbw") var rxBw: Float?,
    @ColumnInfo(name = "pktformat") var pktFormat: Int?,
    @ColumnInfo(name = "signalData") var signalData: String?,
    @ColumnInfo(name = "signalDataLength") var signalDataLength: Int?,
    @ColumnInfo(name = "lqi") var lqi: Float?,
    @ColumnInfo(name = "rssi") var rssi: Float?,

    // ADDITIONAL
    @ColumnInfo(name = "proofofwork") var proofOfWork: Boolean = false,
    @ColumnInfo(name = "published") var published: Boolean = false
)