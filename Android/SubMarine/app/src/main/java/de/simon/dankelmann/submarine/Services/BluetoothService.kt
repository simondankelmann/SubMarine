package de.simon.dankelmann.submarine.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import de.simon.dankelmann.submarine.PermissionCheck.PermissionCheck


@RequiresApi(Build.VERSION_CODES.M)
class BluetoothService (context: Context){
    private val _logTag = "BluetoothService"
    private var _context:Context = context
    private var _bluetoothManager: BluetoothManager
    private var _bluetoothAdapter: BluetoothAdapter? = null


    init{
        _bluetoothManager = _context.getSystemService(BluetoothManager::class.java)
        _bluetoothAdapter = _bluetoothManager.adapter
    }

    fun startDiscovery(){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_SCAN)){
            if (_bluetoothAdapter?.isDiscovering == false){
                Log.d(_logTag, "Starting Discovery")
                _bluetoothAdapter?.startDiscovery()
            }
        }
    }

    fun stopDiscovery(){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_SCAN)){
            if (_bluetoothAdapter?.isDiscovering == true){
                _bluetoothAdapter?.cancelDiscovery()
            }
        }
    }

}