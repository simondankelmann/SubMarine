package de.simon.dankelmann.esp32_subghz.ui.connectedDevice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PeriscopeViewModel: ViewModel() {

    private val _title = MutableLiveData<String>().apply {
        value = "Periscope"
    }

    private val _description = MutableLiveData<String>().apply {
        value = "Looking for Signals"
    }

    val capturedSignalInfo = MutableLiveData<String>().apply {
        value = "No Signal captured yet."
    }

    val capturedSignalData = MutableLiveData<String>().apply {
        value = "-"
    }

    val infoTextFooter = MutableLiveData<String>().apply {
        value = "0 Signals captured yet."
    }

    val connectionState = MutableLiveData<String>().apply {
        value = "Connecting..."
    }

    val locationInfo = MutableLiveData<String>().apply {
        value = "Location"
    }

    fun updateTitle(text:String){
        _title.postValue(text)
    }

    fun updateDescription(text:String){
        _description.postValue(text)
    }

    val title: LiveData<String> = _title
    val description: LiveData<String> = _description
}