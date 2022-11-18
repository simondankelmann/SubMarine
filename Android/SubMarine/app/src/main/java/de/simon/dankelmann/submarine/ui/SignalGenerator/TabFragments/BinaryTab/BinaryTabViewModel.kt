package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.BinaryTab

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.Entities.SignalEntity

class BinaryTabViewModel : ViewModel() {
    var signalEntity = MutableLiveData<SignalEntity?>().apply {
        value = null
    }
}