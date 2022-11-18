package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.ConfigTab

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.Entities.SignalEntity

class ConfigTabViewModel : ViewModel() {
    var signalEntity = MutableLiveData<SignalEntity?>().apply {
        value = null
    }
}