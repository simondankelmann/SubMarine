package de.simon.dankelmann.submarine.ui.SignalMap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SignalMapViewModel : ViewModel() {

    val title = MutableLiveData<String>().apply {
        value = "Signal Map"
    }

    val description = MutableLiveData<String>().apply {
        value = "Locate recorded Signals"
    }

    val footerText1 = MutableLiveData<String>().apply {
        value = "-"
    }


}