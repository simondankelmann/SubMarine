package de.simon.dankelmann.submarine.ui.DetectSignal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.R

class DetectSignalViewModel: ViewModel() {

    val animationResourceId = MutableLiveData<Int>().apply {
        value = R.raw.connecting
    }

    val title = MutableLiveData<String>().apply {
        value = "Detect Signal"
    }

    val description = MutableLiveData<String>().apply {
        value = "Looking for Signals"
    }

    val detectedFrequency = MutableLiveData<String>().apply {
        value = "-"
    }

    val detectedRssi = MutableLiveData<String>().apply {
        value = "-"
    }

    val continuosDetection = MutableLiveData<Boolean>().apply {
        value = true
    }

    val log = MutableLiveData<String>().apply {
        value = ""
    }

    val footerText1 = MutableLiveData<String>().apply {
        value = "0 Signals detected"
    }

    val footerText2 = MutableLiveData<String>().apply {
        value = "-"
    }

    val footerText3 = MutableLiveData<String>().apply {
        value = "-"
    }

}