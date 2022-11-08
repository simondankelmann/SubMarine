package de.simon.dankelmann.submarine.Models

import android.bluetooth.BluetoothDevice

class BluetoothDeviceModel (bluetoothDevice: BluetoothDevice, receivedRssi:Int? = 0, currentMillis:Long? = 0) {
    var rssi:Int? = null
    var device:BluetoothDevice? = null
    var lastSeen:Long? = 0

    init {
        rssi = receivedRssi
        device = bluetoothDevice
        lastSeen = currentMillis
    }
}