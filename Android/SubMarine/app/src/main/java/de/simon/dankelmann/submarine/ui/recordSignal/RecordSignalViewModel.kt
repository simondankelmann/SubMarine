package de.simon.dankelmann.esp32_subghz.ui.recordSignal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecordSignalViewModel: ViewModel() {

    val animationResourceId = MutableLiveData<Int>().apply {
        value = 0
    }

    val title = MutableLiveData<String>().apply {
        value = "Periscope"
    }

    val description = MutableLiveData<String>().apply {
        value = "Looking for Signals"
    }

    val capturedSignalInfo = MutableLiveData<String>().apply {
        value = "No Signal recorded yet."
    }

    val capturedSignalName = MutableLiveData<String>().apply {
        value = ""
    }

    val capturedSignalData = MutableLiveData<String>().apply {
        value = "-"
    }

    val footerText1 = MutableLiveData<String>().apply {
        value = "-"
    }

    val footerText2 = MutableLiveData<String>().apply {
        value = "-"
    }

    val footerText3 = MutableLiveData<String>().apply {
        value = "-"
    }

    /*


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
    }*/


}