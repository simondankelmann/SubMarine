package de.simon.dankelmann.submarine.services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.net.wifi.EasyConnectStatusCallback
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
//import com.google.android.things.bluetooth.BluetoothConnectionManager
import de.simon.dankelmann.submarine.permissioncheck.PermissionCheck
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.reflect.KFunction1

@RequiresApi(Build.VERSION_CODES.M)
class BluetoothSerial (context: Context, connectionChangedCallback:KFunction1<Int, Unit>?){
    // PUBLIC
    val connectionState_Disconnected = 0
    val connectionState_Connecting = 1
    val connectionState_Connected = 2

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
    private var _callback: KFunction1<String, Unit>? = null
    private var _connectionChangedCallback: KFunction1<Int, Unit>? = null
    // PUBLIC VAR
    var isConnected = false

    init{
        _bluetoothManager = _context.getSystemService(BluetoothManager::class.java)
        _bluetoothAdapter = _bluetoothManager.adapter
        _connectionChangedCallback = connectionChangedCallback
    }

    fun connect(macAddress: String, receivedDataCallback: KFunction1<String, Unit>){
        _callback = receivedDataCallback
        if(_bluetoothAdapter != null){
            _bluetoothDevice = _bluetoothAdapter?.getRemoteDevice(macAddress)
            if(_bluetoothDevice != null){
                if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
                    Log.d(_logTag, "Connecting to Socket")
                    connectSocket()
                }
            }
        }
    }

    private fun resetConnection() {
        _connectionChangedCallback!!(connectionState_Disconnected)
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
    }

    fun connectSocket(){
        resetConnection()
        _connectionChangedCallback!!(connectionState_Connecting)

        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
            _bluetoothSocket = _bluetoothDevice?.createInsecureRfcommSocketToServiceRecord(_bluetoothSerialUuid)
            if(_bluetoothSocket != null){
                _bluetoothSocket?.connect()
                _bluetoothSocketOutputStream = _bluetoothSocket?.getOutputStream()
                _bluetoothSocketInputStream = _bluetoothSocket?.getInputStream()
                if(_bluetoothSocket!!.isConnected){
                    isConnected = true
                    _connectionChangedCallback!!(connectionState_Connected)
                    // BEGIN LISTENING
                    beginListeningOnInputStream(_callback!!)
                }
            }
        }
    }

    fun disconnect(){
        stopListeningOnInputStream()
        _connectionChangedCallback!!(connectionState_Disconnected)
        isConnected = false
    }

    fun sendByteString(message:String, statusCallback: KFunction1<String, Unit>){
        Log.d(_logTag, "ByteSize:" + message.toByteArray().size)
        Log.d(_logTag, "StrLen:" + message.length)

        var max = 100
        var delay = 100

        GlobalScope.launch {
            try {
                var bytes = message.toByteArray()
                var fRepeatitions:Float = bytes.size.toFloat() / max
                var repeatitions = fRepeatitions.toInt()

                //Log.d(_logTag, "REPEATS:" + fRepeatitions)

                var res = fRepeatitions.rem(1)
                if (res.equals(0.0F)){
                    //Log.d(_logTag, "EVEN")
                } else {
                    //Log.d(_logTag, "UNEVEN ")
                    repeatitions++;
                }

               // Log.d(_logTag, "repeatitions:  " + repeatitions)
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
                        Thread.sleep(delay.toLong())  // wait for 1 second
                    } catch (ex:java.lang.Exception){
                        Log.d(_logTag, "Exception caught: " + ex.message)
                    }

                    if(i == (repeatitions - 1)){
                        val end = System.currentTimeMillis()
                        val duration = end - begin
                        statusCallback("Transmisson completed after " + duration + "ms")
                    }

                    alreadySent += length
                    /*
                    Log.d(_logTag, "OFFSET: " + offset)
                    Log.d(_logTag, "LENGTH: " + length)
                    Log.d(_logTag, "SENT: " + alreadySent)*/
                }

                //_bluetoothSocketOutputStream!!.write(message.toByteArray(), 0, message.toByteArray().size)

                _bluetoothSocketOutputStream!!.flush()

            } catch (ex: java.lang.Exception) {
                Log.e(_logTag, ex.message.toString())
                isConnected = false
                _connectionChangedCallback!!(connectionState_Disconnected)

                connectSocket()
                //sendString(message)
            }
        }

        Handler(Looper.getMainLooper()).post(Runnable {

        })
    }
    fun sendString(message:String) {
        try {
            if(!_bluetoothSocket!!.isConnected){
                connectSocket()
            }
            if(isConnected && _bluetoothSocketOutputStream != null){
                Thread(Runnable {
                    try {
                        _bluetoothSocketOutputStream!!.write(message.toByteArray())
                    } catch (ex: java.lang.Exception) {
                        Log.e(_logTag, ex.message.toString())
                        isConnected = false
                        _connectionChangedCallback!!(connectionState_Disconnected)

                        connectSocket()
                        //sendString(message)
                    }

                }).start()
            }
        }catch (ex: java.lang.Exception) {
            Log.e(_logTag, ex.message.toString())
            isConnected = false
            _connectionChangedCallback!!(connectionState_Disconnected)

            connectSocket()
            //sendString(message)
        }
    }

    private fun beginListeningOnInputStream(receivedDataCallback: (input: String) -> Unit){
        _inputReaderThread = Thread(Runnable {
            val delimiter: Byte = 10 //This is the ASCII code for a newline character
            var receivedBytes: MutableList<Byte>  = mutableListOf()
            while (!Thread.currentThread().isInterrupted && isConnected && _bluetoothSocket!!.isConnected){
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
                                receivedDataCallback(data)
                                receivedBytes= mutableListOf()
                            } else {
                                // CONTINUE READING TO BUFFER
                                receivedBytes += b
                            }
                        }
                    }
                } catch (ex: IOException) {
                    Log.e(_logTag, ex.message.toString())
                    isConnected = false
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
}