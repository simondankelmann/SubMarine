package de.simon.dankelmann.submarine.ui.scanBt

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.Models.BluetoothDeviceModel

class ScanBtViewModel : ViewModel() {

    private var _logTag = "ScanBtViewModel"

    private var _bluetoothDevices = MutableLiveData<MutableList<BluetoothDeviceModel>>().apply {
        value = mutableListOf()
    }

    fun addFoundBluetoothDevice(device: BluetoothDevice, rssi:Int? = 0, currentMillis:Long? = 0){
        var model = BluetoothDeviceModel(device, rssi, currentMillis)

        if(_bluetoothDevices.value == null) {
            _bluetoothDevices.value = mutableListOf(model)
        } else {
            var alreadyAdded = false
            var addedToIndex = -1
            _bluetoothDevices.value!!.forEachIndexed { index, it ->
                if (it.device!!.address == device.address) {
                    alreadyAdded = true
                    addedToIndex = index
                    it.rssi = rssi
                    it.lastSeen = System.currentTimeMillis()
                }
            }

            if(alreadyAdded){
                //_bluetoothDevices.value =  bluetoothDevices.value!!.toMutableList()
                _bluetoothDevices.postValue(bluetoothDevices.value)
            } else {
                //_bluetoothDevices.value =  bluetoothDevices.value!!.plus(model).toMutableList()
                _bluetoothDevices.postValue(bluetoothDevices.value!!.plus(model).toMutableList()) //=  bluetoothDevices.value!!.plus(model).toMutableList()
            }
        }
    }

    fun getBluetoothDeviceModel(index: Int) : BluetoothDeviceModel?{
        if(this._bluetoothDevices.value != null){
            if(this._bluetoothDevices.value!!.count() >= index){
                return this._bluetoothDevices.value!![index]
            }
        }
        return null
    }

    private val _text = MutableLiveData<String>().apply {
        value = "This is scanBt Fragment"
    }

    val text: LiveData<String> = _text
    val bluetoothDevices: LiveData<MutableList<BluetoothDeviceModel>> = _bluetoothDevices

}