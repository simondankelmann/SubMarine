package de.simon.dankelmann.submarine.Models

import de.simon.dankelmann.submarine.Entities.SignalEntity

class CC1101Configuration {

    var mhz:Float = 433.92f
    var tx:Boolean = false
    var modulation:Int = 2
    var dRate:Int = 512
    var rxBw:Float = 256.0f
    var pktFormat:Int = 3
    var lqi:Float = 0.0f
    var rssi:Float = 0.0f

    fun loadFromString(cc1101ConfigString:String){
        this.mhz = cc1101ConfigString.substring(0,6).toFloat()
        this.modulation = cc1101ConfigString[7].toString().toInt()
        this.dRate = cc1101ConfigString.substring(8,11).toInt()
        this.rxBw = cc1101ConfigString.substring(11,17).toFloat()
        this.pktFormat = cc1101ConfigString[17].toString().toInt()
        this.lqi = cc1101ConfigString.substring(18,24).toFloat()
        this.rssi = cc1101ConfigString.substring(24,30).toFloat()
    }

    fun loadFromSignalEntity(signalEntity: SignalEntity){
        this.mhz = signalEntity.frequency!!
        this.modulation = signalEntity.modulation!!
        this.dRate = signalEntity.modulation!!
        this.rxBw = signalEntity.rxBw!!
        this.pktFormat = signalEntity.pktFormat!!
        this.lqi = signalEntity.lqi!!
        this.rssi = signalEntity.rssi!!
    }

    fun getModulationName(modulation:Int):String{
        when(modulation){
            0 -> return "2-FSK"
            1 -> return "GFSK"
            2 -> return "ASK/OOK"
            3 -> return "4-FSK"
            4 -> return "MSK"
        }
        return "Unknown"
    }

    fun getConfigurationString():String{
        var mhz = this.mhz.toString()
        while(mhz.length < 6){
            mhz = "0$mhz"
        }

        var tx = "0"
        if(this.tx == true){
            tx = "1"
        }
        var modulation = this.modulation.toString()

        var dRate = this.dRate.toString()
        while(dRate.length < 3){
            dRate = "0$dRate"
        }

        var rxBw = this.rxBw.toString()
        while(rxBw.length < 6){
            rxBw = "0$rxBw"
        }

        var pktFormat = this.pktFormat.toString()

        var lqi = this.lqi.toString()
        while(lqi.length < 6){
            lqi = "0$lqi"
        }

        var rssi = this.rssi.toString()
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