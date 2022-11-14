package de.simon.dankelmann.submarine.Services

import android.bluetooth.BluetoothDevice
import android.util.Log
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Interfaces.SubmarineResultListenerInterface
import de.simon.dankelmann.submarine.Models.SubmarineCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
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
    private var _bluetoothDevice:BluetoothDevice? = null
    //var deviceAddress:String = ""

    //var connectionStateChangedCallback: KFunction1<Int, Unit>? = null
    private var _connectionStateChangedCallbacks: MutableList<KFunction1<Int, Unit>> = mutableListOf()
    private var _incomingDataCallbacks: MutableList<KFunction1<String, Unit>> = mutableListOf()
    private var _commandSentCallbacks: MutableList<KFunction1<String, Unit>> = mutableListOf()
    private var _setOperationModeCallbacks: MutableList<KFunction1<String, Unit>> = mutableListOf()
    private var _replaySignalCallbacks: MutableList<KFunction1<String, Unit>> = mutableListOf()

    // OUTSOURCING CALLBACKS TO INTERFACE
    private var _resultListeners:MutableList<SubmarineResultListenerInterface> = mutableListOf()

    fun connect(){
        if(!isConnected() && !isConnecting()){
            if(_bluetoothDevice != null && _bluetoothDevice!!.address != null && _bluetoothDevice!!.address != ""){
                var macAddress = _bluetoothDevice!!.address
                Log.d(_logTag, "Connecting Bluetooth Serial on " + macAddress)
                _bluetoothSerial = BluetoothSerial(AppContext.getContext(), this)
                CoroutineScope(Dispatchers.IO).launch {
                    _bluetoothSerial?.connect(macAddress, 1)
                }
                /*
                Thread(Runnable {
                    //_bluetoothSerial?.connect(deviceFromBundle.address, ::receivedDataCallback)
                    //_bluetoothSerial?.connect(macAddress, ::bluetoothSerialReceivedDataCallback)

                }).start()*/
            } else {
                Log.d(_logTag, "Mac Address is not valid")
            }
        } else {
            Log.d(_logTag, "Already connected")
            connectionStateChanged(ConnectionStates.Connected.value)
        }

        /*if(_connectionState == ConnectionStates.Connected.value){
            connectionStateChangedCallback(2)
        }*/
    }

    fun setBluetoothDevice(bluetoothDevice: BluetoothDevice){
        _bluetoothDevice = bluetoothDevice
    }

    fun getBluetoothDevice():BluetoothDevice?{
        return _bluetoothDevice
    }

    fun addResultListener(resultListener: SubmarineResultListenerInterface){
        //if(_resultListeners.contains(resultListener) == false){

        //}
        _resultListeners.add(resultListener)
    }

    fun removeResultListener(resultListener: SubmarineResultListenerInterface){
        //if(_resultListeners.contains(resultListener)){
            _resultListeners.remove(resultListener)
        //}
    }

    fun getResultListeners():MutableList<SubmarineResultListenerInterface>{
       return _resultListeners
    }

    fun isConnected():Boolean{
        return getConnectionState() == ConnectionStates.Connected.value
    }

    fun isConnecting():Boolean{
        return getConnectionState() == ConnectionStates.Connecting.value
    }

    fun getConnectionState():Int{
        return _connectionState
    }

    fun connectionStateChanged(connectionState:Int){
        _connectionState = connectionState

        // EXECUTE CALLBACKS
        _resultListeners.map {
            try {
                it.onConnectionStateChanged(connectionState)
            } catch(ex:java.lang.Exception){
                Log.d(_logTag, "Callback could not be Executed: " + ex.message)
            }
        }

        Log.d(_logTag, "Connection State was changed: " + connectionState)
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

        // TODO: REMOVE WHEN MOVED INTERFACE
        /*
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
        }*/
    }

    fun sendCommandToDevice(command:SubmarineCommand){
        /*
        val commandString = command + commandId + dataString
        var callback = ::commandSentToDeviceCallback
        if(command == Constants.COMMAND_SET_OPERATION_MODE){
            callback = ::setOperationModeCallback
        }
        if(command == Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND){
            callback = ::replaySignalCallback
        }*/

        //_bluetoothSerial!!.sendByteString(commandString + "\n", callback)
        _bluetoothSerial!!.sendCommand(command)
    }




    fun setOperationMode(operationMode:String, dataString: String? = null){
        var cmdData = operationMode
        if(dataString != null){
            cmdData += dataString
        }
        val command = SubmarineCommand(Constants.COMMAND_SET_OPERATION_MODE,Constants.COMMAND_ID_DUMMY, cmdData)
        sendCommandToDevice(command)
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


    companion object{

    }

}