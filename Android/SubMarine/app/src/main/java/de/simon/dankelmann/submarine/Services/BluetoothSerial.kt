package de.simon.dankelmann.submarine.Services

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.AppContext.AppContext.Companion.submarineService
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Models.SubmarineCommand
//import com.google.android.things.bluetooth.BluetoothConnectionManager
import de.simon.dankelmann.submarine.PermissionCheck.PermissionCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

@RequiresApi(Build.VERSION_CODES.M)
class BluetoothSerial (context: Context, submarineService:SubMarineService, activity: Activity){
    // PUBLIC
    /*
    val connectionState_Disconnected = 0
    val connectionState_Connecting = 1
    val connectionState_Connected = 2
    */


    //PRIVATE VAR
    private val _logTag = "BluetoothSerial"
    private var _context:Context = context
    private var _bluetoothManager: BluetoothManager
    private var _bluetoothAdapter: BluetoothAdapter? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    private var _bluetoothSocket: BluetoothSocket? = null
    private var _bluetoothSocketInputStream: InputStream? = null
    private var _bluetoothSocketOutputStream: OutputStream? = null
    private var _bluetoothSerialUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
    private var _inputReaderThread:Thread? = null
    private var _maxReconnectionAttempts = 3
    private var _maxReconnectionAttemptsSocket = 3
    private var _connectionTimeoutSocket:Long = 1000
    private var _connectionState = SubMarineService.ConnectionStates.Disconnected.value
    private val _activity = activity
    //private var _callback: KFunction1<String, Unit>? = null
    //private var _connectionChangedCallback: KFunction1<Int, Unit>? = null
    // PUBLIC VAR
    //var isConnected = false
    var connectionAttempt = 0
    private var _macAddress = ""


    init{
        _bluetoothManager = _context.getSystemService(BluetoothManager::class.java)
        _bluetoothAdapter = _bluetoothManager.adapter
        registerBroadcastReceivers()
    }

    fun registerBroadcastReceivers(){
        var mReceiver = BluetoothBroadcastReceiver()
        val intentFilterConnected = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        val intentFilterDisonnected = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        AppContext.getContext().registerReceiver(mReceiver, intentFilterConnected)
        AppContext.getContext().registerReceiver(mReceiver, intentFilterDisonnected)
    }

    fun connect(macAddress: String, attempt: Int){
        try{
            _macAddress = macAddress
            if(_bluetoothAdapter != null ){
                _bluetoothDevice = _bluetoothAdapter?.getRemoteDevice(macAddress)
                if(_bluetoothDevice != null){
                    if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, _activity)){
                        Log.d(_logTag, "Connecting to Socket")
                        connectSocket()
                    }
                }
            }
        } catch (ex:java.lang.Exception){
            Log.d(_logTag, "Could not connect on Attempt  " + attempt + ":"+ ex.message)
        }
    }

    fun getConnectionState():Int{
        return _connectionState
    }

    private fun resetConnection() {

        //_connectionChangedCallback!!(connectionState_Disconnected)
        // EXECUTE CALLBACKS
        /*
        connectionStateChanged(SubMarineService.ConnectionStates.Disconnected.value)

        if (_bluetoothSocketInputStream != null) {
            try {
                _bluetoothSocketInputStream!!.close()
            } catch (e: Exception) {
            }
            _bluetoothSocketInputStream = null
        }
        if (_bluetoothSocketOutputStream != null) {
            try {
                _bluetoothSocketOutputStream!!.close()
            } catch (e: Exception) {
            }
            _bluetoothSocketOutputStream = null
        }
        if (_bluetoothSocket != null) {
            try {
                _bluetoothSocket!!.close()
            } catch (e: Exception) {
            }
            _bluetoothSocket = null
        }
        Log.d(_logTag, "Connection was resetted")*/
    }

    fun connectionStateChanged(connectionState:Int){
        _connectionState = connectionState
        if(connectionState == SubMarineService.ConnectionStates.Connected.value){
            // DOME SOMETHING
        } else if(connectionState == SubMarineService.ConnectionStates.Connecting.value) {
            // DOME SOMETHING
        } else if(connectionState == SubMarineService.ConnectionStates.Disconnected.value) {
            // DOME SOMETHING
        }

        // PASS THE CONNECTIONSTATE TO SUBMARINE SERVICE
        submarineService.connectionStateChanged(connectionState)
        Log.d(_logTag, "Connection State was changed: " + connectionState)
    }

    fun connectSocket(){
        resetConnection()

        // EXECUTE CALLBACKS
        connectionStateChanged(SubMarineService.ConnectionStates.Connecting.value)

        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT,_activity)){
            _bluetoothSocket = _bluetoothDevice?.createInsecureRfcommSocketToServiceRecord(_bluetoothSerialUuid)
            if(_bluetoothSocket != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        var attempt = 1
                        while (_bluetoothSocket?.isConnected == false) {
                            try {
                                _bluetoothSocket?.connect()
                            } catch (e: Exception) {
                                Thread.sleep(_connectionTimeoutSocket)
                                attempt++
                                Log.d(_logTag, "Could not connect to Socket: " + e.toString())
                                if (attempt >= _maxReconnectionAttemptsSocket) {
                                    Log.d(_logTag, "Exceeded max Connection attempts for Socket")
                                    break
                                }
                            }
                        }

                        if (_bluetoothSocket?.isConnected == true) {
                            Log.d(_logTag, "Connection to Socket established after attempt: $attempt")
                            _bluetoothSocketOutputStream = _bluetoothSocket!!.getOutputStream()
                            _bluetoothSocketInputStream = _bluetoothSocket!!.getInputStream()
                            beginListeningOnInputStream()
                            //Thread.sleep(_connectionTimeoutSocket)
                            connectionStateChanged(SubMarineService.ConnectionStates.Connected.value)
                        } else {
                            connectionStateChanged(SubMarineService.ConnectionStates.Disconnected.value)
                        }
                }
            }
        }
    }

    fun disconnect(){
        stopListeningOnInputStream()
        // EXECUTE CALLBACKS
        connectionStateChanged(SubMarineService.ConnectionStates.Disconnected.value)
    }

    fun sendCommand(command: SubmarineCommand){
        Log.d(_logTag, "Sending Command: " + command._command)
        sendByteString(command.getCommandString() , command)
    }

    fun sendByteString(message:String, command:SubmarineCommand? = null){
        Log.d(_logTag, "ByteSize:" + message.toByteArray().size)
        Log.d(_logTag, "StrLen:" + message.length)
        Log.d(_logTag, "DATA: " + message)

        var max = 100
        var delay = 100

        GlobalScope.launch {
            try {
                var bytes = message.toByteArray()
                var fRepeatitions:Float = bytes.size.toFloat() / max
                var repeatitions = fRepeatitions.toInt()

                var res = fRepeatitions.rem(1)
                if (res.equals(0.0F)){
                } else {
                    repeatitions++;
                }

                var alreadySent = 0
                val begin = System.currentTimeMillis()
                for (i in 0..(repeatitions - 1)){
                    var offset = max * i
                    var length = max

                    if(i == (repeatitions - 1)){
                        length = bytes.size - alreadySent
                    }

                    _bluetoothSocketOutputStream!!.write(message.toByteArray(), offset, length)

                    try{
                        if(i < (repeatitions - 1)){
                            Thread.sleep(delay.toLong())  // wait for 1 second
                        }
                    } catch (ex:java.lang.Exception){
                        Log.d(_logTag, "Exception caught: " + ex.message)
                    }

                    if(i == (repeatitions - 1)){
                        val end = System.currentTimeMillis()
                        val duration = (end - begin).toInt()

                        // EXECUTE CALLBACKS
                        if(command == null){
                            // DEFAULT CALLBACKS
                            Log.d(_logTag, "Executing onOutgoingData Callbacks")
                            submarineService.getResultListeners().map {
                                try {
                                    it.onOutgoingData(duration, command)
                                } catch(ex:java.lang.Exception){
                                    Log.d(_logTag, "Callback could not be Executed: " + ex.message)
                                }
                            }
                        } else {
                            // COMMAND SPECIFIC CALLBACKS
                            submarineService.getResultListeners().map {
                                try {
                                    when(command._command){
                                        Constants.COMMAND_SET_OPERATION_MODE -> {
                                            it.onOperationModeSet(duration, command)
                                        }

                                        Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND -> {
                                            it.onSignalReplayed(duration, command)
                                        }
                                        else -> {
                                            it.onCommandSent(duration, command)
                                        }
                                    }

                                } catch(ex:java.lang.Exception){
                                    Log.d(_logTag, "Callback could not be Executed: " + ex.message)
                                }
                            }
                        }
                    }

                    alreadySent += length
                    /*
                    Log.d(_logTag, "OFFSET: " + offset)
                    Log.d(_logTag, "LENGTH: " + length)
                    Log.d(_logTag, "SENT: " + alreadySent)*/
                }

                _bluetoothSocketOutputStream!!.flush()

            } catch (ex: java.lang.Exception) {
                Log.e(_logTag, ex.message.toString())
            }
        }
    }

    private fun beginListeningOnInputStream(){
        _inputReaderThread = Thread(Runnable {
            val delimiter: Byte = 10 //This is the ASCII code for a newline character
            var receivedBytes: MutableList<Byte>  = mutableListOf()
            while (!Thread.currentThread().isInterrupted && _connectionState == SubMarineService.ConnectionStates.Connected.value && _bluetoothSocket!!.isConnected){
                try{
                    val bytesAvailable: Int = _bluetoothSocketInputStream!!.available()
                    //Log.d(_logTag, "BYTES INCOMING: " + bytesAvailable)
                    if (bytesAvailable > 0) {
                        val packetBytes = ByteArray(bytesAvailable)
                        _bluetoothSocketInputStream?.read(packetBytes)
                        for (i in 0 until bytesAvailable) {
                            val b = packetBytes[i]
                            if (b == delimiter) {
                                val data = String(receivedBytes.toByteArray(), Charsets.UTF_8)
                                Log.d(_logTag, "RECEIVED: $data")
                                // PASS RECEIVED DATA TO CALLBACK
                                submarineService.getResultListeners().map {
                                    try {
                                        it.onIncomingData(data, SubmarineCommand.parseFromDataString(data))
                                    } catch(ex:java.lang.Exception){
                                        Log.d(_logTag, "Callback could not be Executed: " + ex.message)
                                    }
                                }
                                //receivedDataCallback(data)
                                receivedBytes= mutableListOf()
                            } else {
                                // CONTINUE READING TO BUFFER
                                receivedBytes += b
                            }
                        }
                    }
                } catch (ex: IOException) {
                    Log.e(_logTag, ex.message.toString())
                    // EXECUTE CALLBACKS
                    connectionStateChanged(SubMarineService.ConnectionStates.Disconnected.value)
                }
            }
        })
        _inputReaderThread?.start()
    }

    private fun stopListeningOnInputStream(){
        if(_inputReaderThread != null){
            _inputReaderThread?.stop()
        }
    }

    inner class BluetoothBroadcastReceiver: BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            Log.d(_logTag, "Broadcast Received: " + intent?.action )
            if(ctx != null && intent != null) {
                var action = intent.action
                if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                    connectionAttempt = 0
                    //connectionStateChanged(SubMarineService.ConnectionStates.Connected.value)
                }
                if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {

                    // TRY AGAIN
                    if(connectionAttempt <= _maxReconnectionAttempts){
                        connectionAttempt++
                        // SLEEP
                        Thread.sleep(1000)
                        connect(_macAddress, connectionAttempt)
                    } else {
                        connectionStateChanged(SubMarineService.ConnectionStates.Disconnected.value)
                    }

                }
            }
        }
    }
}