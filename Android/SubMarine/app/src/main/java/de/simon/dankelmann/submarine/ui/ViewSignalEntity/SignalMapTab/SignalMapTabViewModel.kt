package de.simon.dankelmann.submarine.ui.ViewSignalEntity.SignalMapTab

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.Entities.SignalEntity

class SignalMapTabViewModel : ViewModel() {
    val signalEntity = MutableLiveData<SignalEntity?>().apply {
        value = null
    }
}