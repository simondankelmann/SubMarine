package de.simon.dankelmann.submarine.ui.SignalDatabase

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Models.BluetoothDeviceModel

class SignalDatabaseViewModel: ViewModel() {

    var signalEntities = MutableLiveData<MutableList<SignalEntity>>().apply {
        value = mutableListOf()
    }


    /*
    var signalEntities = MutableLiveData<List<SignalEntity>>().apply {
        value = mutableListOf()
    }*/

    /*
    fun addSignalEntity(signalEntity: SignalEntity){
        if(signalEntities.value == null) {
            signalEntities.value = mutableListOf(signalEntity)
        } else {
            signalEntities.postValue(signalEntities.value!!.plus(signalEntity).toMutableList()) //=  bluetoothDevices.value!!.plus(model).toMutableList()
            //signalEntities.value = signalEntities.value!!.plus(signalEntity).toMutableList()
        }
    }*/

    fun getSignalEntity(index: Int) : SignalEntity?{
        if(this.signalEntities.value != null){
            if(this.signalEntities.value!!.count() >= index){
                return this.signalEntities.value!![index]
            }
        }
        return null
    }

    val signalDatabaseDescription = MutableLiveData<String>().apply {
        value = "0 Signals in Database"
    }

    val signalDatabaseFooterText = MutableLiveData<String>().apply {
        value = "-"
    }

}