package de.simon.dankelmann.submarine.ui.ViewSignalEntity.TabFragments.SignalDataTab

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SignalDataTabViewModel : ViewModel() {
    val signalData = MutableLiveData<String>().apply {
        value = "-"
    }
}