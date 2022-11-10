package de.simon.dankelmann.submarine.services

import android.os.Build
import androidx.annotation.RequiresApi
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Entities.SignalEntity
import java.time.LocalDateTime
import java.time.ZoneOffset

class SubMarineService {
    @RequiresApi(Build.VERSION_CODES.O)
    fun parseSignalEntityFromDataString(data:String, locationId:Int): SignalEntity {
        var configEndIndex = Constants.BLUETOOTH_COMMAND_HEADER_LENGTH + Constants.CC1101_ADAPTER_CONFIGURATION_LENGTH
        var cc1101ConfigString = data.substring(Constants.BLUETOOTH_COMMAND_HEADER_LENGTH, configEndIndex)
        var signalData = data.substring(configEndIndex)

        // CLEAR EMPTY FIRST SAMPLES:
        var samples = signalData.split(",").toMutableList()
        while(samples.last().toInt() <= 0){
            samples.removeLast()
        }

        // CLEAR EMPTY LAST SAMPLES:
        while(samples.first().toInt() <= 0){
            samples.removeFirst()
        }

        signalData = samples.joinToString(",")
        var samplesCount = signalData.split(',').size

        var signalName = ""
        var signalTag = ""
        var timestamp = LocalDateTime.now().atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        var frequency = cc1101ConfigString.substring(0,6).toFloat()
        var modulation = cc1101ConfigString[7].toString().toInt()
        var dRate = cc1101ConfigString.substring(8,11).toInt()
        var rxBw = cc1101ConfigString.substring(11,17).toFloat()
        var pktFormat = cc1101ConfigString[17].toString().toInt()
        var lqi = cc1101ConfigString.substring(18,24).toFloat()
        var rssi = cc1101ConfigString.substring(24,30).toFloat()

        if(signalName == ""){
            signalName = frequency.toInt().toString() + "_" + LocalDateTime.now().year + "-" + LocalDateTime.now().month + "-" + + LocalDateTime.now().dayOfMonth + "-"+ LocalDateTime.now().hour + ":" + LocalDateTime.now().minute + ":" + LocalDateTime.now().second
        }

        var signalEntity: SignalEntity = SignalEntity(0, signalName, signalTag, locationId, timestamp?.toInt(), "RAW", frequency, modulation,dRate,rxBw,pktFormat,signalData,samplesCount,lqi,rssi,false,false)
        return signalEntity
    }

    fun getConfigurationStringFromSignalEntity(signalEntity: SignalEntity):String{

        var mhz = signalEntity.frequency.toString()
        while(mhz.length < 6){
            mhz = "0$mhz"
        }

        var tx = "1"
        var modulation = signalEntity.modulation.toString()

        var dRate = signalEntity.dRate.toString()
        while(dRate.length < 3){
            dRate = "0$dRate"
        }

        var rxBw = signalEntity.rxBw.toString()
        while(rxBw.length < 6){
            rxBw = "0$rxBw"
        }

        var pktFormat = signalEntity.pktFormat.toString()

        var lqi = signalEntity.lqi.toString()
        while(lqi.length < 6){
            lqi = "0$lqi"
        }

        var rssi = signalEntity.rssi.toString()
        while(rssi.length < 6){
            rssi = "0$rssi"
        }

        /*
        Adapter Configuration Structure:
        Bytes:
        0-5 => MHZ
        6 => TX
        7 => MODULATION
        8-10 => DRATE
        11-16 => RX_BW
        17 => PKT_FORMAT
        18-23 => AVG_LQI
        24-29 => AVG_RSSI
        */

        return mhz + tx + modulation + dRate + rxBw + pktFormat + lqi + rssi
    }

}