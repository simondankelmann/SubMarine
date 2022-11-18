package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.Entities.SignalEntity

class TimingsTabViewModel : ViewModel() {
    var signalEntity = MutableLiveData<SignalEntity?>().apply {
        value = null
    }
}