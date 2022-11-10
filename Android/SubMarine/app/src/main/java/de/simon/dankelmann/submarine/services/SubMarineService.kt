package de.simon.dankelmann.submarine.services

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.R
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1

class SubMarineService {

    enum class ConnectionStates(val value: Int)  {
        Disconnected(0), Connecting(1), Connected(2)
    }

    enum class CallbackType(val value: Int)  {
        BluetoothConnectionStateChanged(0), IcomingData(1), CommandSent(2), SetOperationMode(3), ReplaySignal(4)
    }

    private val _logTag = "SubmarineService"
    private var _connectionState:Int = ConnectionStates.Disconnected.value
    private var _bluetoothSerial:BluetoothSerial? = null
    private var _isListening = false

    var deviceAddress:String = ""

    //var connectionStateChangedCallback: KFunction1<Int, Unit>? = null
    private var _connectionStateChangedCallbacks: MutableList<KFunction1<Int, Unit>> = mutableListOf()
    private var _incomingDataCallbacks: MutableList<KFunction1<String, Unit>> = mutableListOf()
    private var _commandSentCallbacks: MutableList<KFunction1<String, Unit>> = mutableListOf()
    private var _setOperationModeCallbacks: MutableList<KFunction1<String, Unit>> = mutableListOf()
    private var _replaySignalCallbacks: MutableList<KFunction1<String, Unit>> = mutableListOf()

    fun connect(){
        if(_connectionState != ConnectionStates.Connected.value && deviceAddress != ""){
            Log.d(_logTag, "Connecting Bluetooth Serial on " + deviceAddress)
            _bluetoothSerial = BluetoothSerial(AppContext.getContext(), ::connectionStateChangedCallback)
            Thread(Runnable {
                //_bluetoothSerial?.connect(deviceFromBundle.address, ::receivedDataCallback)
                _bluetoothSerial?.connect(deviceAddress, ::bluetoothSerialReceivedDataCallback)
            }).start()
        }

        if(_connectionState == ConnectionStates.Connected.value){
            connectionStateChangedCallback(2)
        }
    }

    fun clearCallbacks(){
        _connectionStateChangedCallbacks = mutableListOf()
        _incomingDataCallbacks = mutableListOf()
        _commandSentCallbacks = mutableListOf()
        _setOperationModeCallbacks = mutableListOf()
        _replaySignalCallbacks = mutableListOf()
    }

    fun registerCallback(function:Any, callbackType: CallbackType){
        when(callbackType){
            CallbackType.BluetoothConnectionStateChanged -> {
                _connectionStateChangedCallbacks.add(function as KFunction1<Int, Unit>)
            }
            CallbackType.IcomingData -> {
                _incomingDataCallbacks.add(function as KFunction1<String, Unit>)
            }
            CallbackType.CommandSent -> {
                _commandSentCallbacks.add(function as KFunction1<String, Unit>)
            }
            CallbackType.SetOperationMode -> {
                _setOperationModeCallbacks.add(function as KFunction1<String, Unit>)
            }
            CallbackType.ReplaySignal -> {
                _replaySignalCallbacks.add(function as KFunction1<String, Unit>)
            }
            else -> {
                Log.d(_logTag, "Callback could not be registered: " + callbackType.toString())
            }
        }
    }



    fun isConnected():Boolean{
        return _connectionState == ConnectionStates.Connected.value
    }

    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Submarine Service Connection Callback: " + connectionState)
        _connectionState = connectionState
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
                _isListening = false
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
                _isListening = false
            }
            2 -> {
                Log.d(_logTag, "Connected")
                //_bluetoothSerial?.connect(deviceAddress!!, ::bluetoothSerialReceivedDataCallback)
                _isListening = true
            }
        }

        // EXECUTE CALLBACKS
        _connectionStateChangedCallbacks.map {
            try{
                if(it != null){
                    it(connectionState)
                } else {
                    Log.d(_logTag, "Callback was null")
                }
            } catch(e:Exception){
                Log.d(_logTag, "Connection State Callback could not be called: " + e.message)
            }
        }
    }

    fun sendCommandToDevice(command:String, commandId:String, dataString: String){
        val commandString = command + commandId + dataString
        var callback = ::commandSentToDeviceCallback
        if(command == Constants.COMMAND_SET_OPERATION_MODE){
            callback = ::setOperationModeCallback
        }
        if(command == Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND){
            callback = ::replaySignalCallback
        }

        _bluetoothSerial!!.sendByteString(commandString + "\n", callback)
    }

    private fun commandSentToDeviceCallback(data: String){
        Log.d(_logTag, "Command was sent to Device successfully")
        // EXECUTE CALLBACKS
        _commandSentCallbacks.map {
            try{
                if(it != null){
                    it(data)
                } else {
                    Log.d(_logTag, "Callback was null")
                }
            } catch(e:Exception){
                Log.d(_logTag, "CommandSent Callback could not be called: " + e.message)
            }
        }
    }

    private fun bluetoothSerialReceivedDataCallback(data: String){
        if(data != ""){
            Log.d(_logTag, "Received incoming Data: " + data)

            // PARSE COMMAND AND DATA
            var incomingCommand = data.substring(0,4)
            var incomingCommandId = data.substring(4,8)

            Log.d(_logTag, "Icoming Command: " + incomingCommand)
            Log.d(_logTag, "Icoming Command Id: " + incomingCommandId)

            /*
            when (incomingCommand) {
               "0003" -> handleIncomingSignalTransfer(message)
               else -> { // Note the block
                   Log.d(_logTag, "Icoming Command not parseable")
               }
            }
            */

            // EXECUTE CALLBACKS
            _incomingDataCallbacks.map {
                try{
                    if(it != null){
                        it(data)
                    } else {
                        Log.d(_logTag, "Callback was null")
                    }
                } catch(e:Exception){
                    Log.d(_logTag, "IncomingData Callback could not be called: " + e.message)
                }
            }
        }
    }

    fun setOperationMode(operationMode:String){
        val command = Constants.COMMAND_SET_OPERATION_MODE
        val commandId = Constants.COMMAND_ID_DUMMY
        sendCommandToDevice(command, commandId, operationMode)
    }

    private fun setOperationModeCallback(data:String){
        // EXECUTE CALLBACKS
        _setOperationModeCallbacks.map {
            try{
                if(it != null){
                    it(data)
                } else {
                    Log.d(_logTag, "Callback was null")
                }
            } catch(e:Exception){
                Log.d(_logTag, "Connection State Callback could not be called: " + e.message)
            }
        }
    }

    private fun replaySignalCallback(data:String){
        // EXECUTE CALLBACKS
        _replaySignalCallbacks.map {
            try{
                if(it != null){
                    it(data)
                } else {
                    Log.d(_logTag, "Callback was null")
                }
            } catch(e:Exception){
                Log.d(_logTag, "Replay Signal Callback could not be called: " + e.message)
            }
        }
    }

    // ---------------------------------------------------------- HELPERS
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