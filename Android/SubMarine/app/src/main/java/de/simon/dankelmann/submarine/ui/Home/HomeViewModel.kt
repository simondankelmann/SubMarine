package de.simon.dankelmann.submarine.ui.Home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _signalData = MutableLiveData<String>().apply {
        value = "-"
    }

    private val _connectionStatusText = MutableLiveData<String>().apply {
        value = "Please connect"
    }

    private val _replayStatusText = MutableLiveData<String>().apply {
        value = "Nothing replayed yet"
    }


    private val _signalStatusText = MutableLiveData<String>().apply {
        value = "No Signal detected yet"
    }

    fun updateSignalData(message:String){
        _signalData.postValue(message)
    }

    fun updateConnectionStatusText(message:String){
        _connectionStatusText.postValue(message)
    }

    fun updateReplayStatusText(message:String){
        _replayStatusText.postValue(message)
    }

    fun updateSignalStatusText(message:String){
        _signalStatusText.postValue(message)
    }

    val signalData: LiveData<String> = _signalData
    val connectionStatus: LiveData<String> = _connectionStatusText
    val replayStatus: LiveData<String> = _replayStatusText
    val signalStatus: LiveData<String> = _signalStatusText
}