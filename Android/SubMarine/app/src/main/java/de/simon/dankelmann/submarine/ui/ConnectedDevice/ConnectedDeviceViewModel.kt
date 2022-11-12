package de.simon.dankelmann.submarine.ui.ConnectedDevice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.R

class ConnectedDeviceViewModel: ViewModel() {

    val animationResourceId = MutableLiveData<Int>().apply {
        value = R.raw.connecting
    }

    val dbInfoText = MutableLiveData<String>().apply {
        value = "0 Signals in Database"
    }

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