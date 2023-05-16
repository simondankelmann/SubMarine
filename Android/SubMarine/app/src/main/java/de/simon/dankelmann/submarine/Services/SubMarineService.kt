package de.simon.dankelmann.submarine.Services

import android.Manifest
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Interfaces.SubmarineResultListenerInterface
import de.simon.dankelmann.submarine.Models.CC1101Configuration
import de.simon.dankelmann.submarine.Models.SubmarineCommand
import de.simon.dankelmann.submarine.PermissionCheck.PermissionCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.sign
import kotlin.reflect.KFunction1

class SubMarineService (activity: Activity) {
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
    private val _activity:Activity = activity
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
                _bluetoothSerial = BluetoothSerial(AppContext.getContext(), this, _activity)
                CoroutineScope(Dispatchers.IO).launch {
                    _bluetoothSerial?.connect(macAddress, 1)
                }
            } else {
                Log.d(_logTag, "Mac Address is not valid")
            }
        } else {
            Log.d(_logTag, "Already connected")
            connectionStateChanged(ConnectionStates.Connected.value)
        }
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
        if(_bluetoothSerial != null){
            return _bluetoothSerial!!.getConnectionState()
        }
        return ConnectionStates.Disconnected.value
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

    fun sendCommandToDevice(command:SubmarineCommand){
        _bluetoothSerial!!.sendCommand(command)
    }

    fun transmitSignal(signalEntity: SignalEntity, repeatitions: Int = 1, repeatitionDelay:Int = 32000){
        var reps = repeatitions
        var repDelay = repeatitionDelay
        Log.d(_logTag, "Sending Transmissioncommand with : " +repeatitions +" Repeatitions and a Delay of: " + repeatitionDelay)

        if(reps < 0){
            reps = reps * -1
        }
        if(repDelay < 0){
            repDelay = repDelay * -1
        }


        val stringLengthRepeations = 4
        val stringLengthRepeationDelay = 6

        var repeatitionsString = reps.toString()
        while(repeatitionsString.length < stringLengthRepeations){
            repeatitionsString = "0" + repeatitionsString
        }
        Log.d(_logTag, "Repeations: " + repeatitionsString)

        var repeatitionsDelayString = repDelay.toString()
        while(repeatitionsDelayString.length < stringLengthRepeationDelay){
            repeatitionsDelayString = "0" + repeatitionsDelayString
        }
        Log.d(_logTag, "RepeationsDelay: " + repeatitionsDelayString)

        var commandString = getConfigurationStringFromSignalEntity(signalEntity) + repeatitionsString + repeatitionsDelayString + signalEntity.signalData

        var sumbarineCommand = SubmarineCommand(Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND, Constants.COMMAND_ID_DUMMY, commandString)
        sendCommandToDevice(sumbarineCommand)

        //_submarineService.sendCommandToDevice(SubmarineCommand(Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND,Constants.COMMAND_ID_DUMMY,_submarineService.getConfigurationStringFromSignalEntity(signalEntity) + signalEntity.signalData))
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

    fun setConfigurationToSignalEntity(signalEntity: SignalEntity, configuration: CC1101Configuration){
        signalEntity.frequency = configuration.mhz
        signalEntity.modulation = configuration.modulation
        signalEntity.dRate = configuration.dRate
        signalEntity.rxBw = configuration.rxBw
        signalEntity.pktFormat = configuration.pktFormat
    }



}