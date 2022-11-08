package de.simon.dankelmann.esp32_subghz.ui.connectedDevice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConnectedDeviceViewModel: ViewModel() {

    private val _title = MutableLiveData<String>().apply {
        value = "Device Name"
    }

    private val _description = MutableLiveData<String>().apply {
        value = "MAC Address"
    }

    private val _text = MutableLiveData<String>().apply {
        value = "This is connected Device Fragment"
    }

    fun updateText(text:String){
        _text.postValue(text)
    }

    fun updateTitle(text:String){
        _title.postValue(text)
    }

    fun updateDescription(text:String){
        _description.postValue(text)
    }

    val text: LiveData<String> = _text
    val title: LiveData<String> = _title
    val description: LiveData<String> = _description
}