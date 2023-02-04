package de.simon.dankelmann.esp32_subghz.ui.importSignal

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.R

class ImportSignalViewModel: ViewModel() {

    val animationResourceId = MutableLiveData<Int>().apply {
        value = R.raw.upload
    }

    val title = MutableLiveData<String>().apply {
        value = "Import a Signal"
    }

    val description = MutableLiveData<String>().apply {
        value = "Import a local Signal File"
    }

    val capturedSignalInfo = MutableLiveData<String>().apply {
        value = "No Signal imported yet."
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