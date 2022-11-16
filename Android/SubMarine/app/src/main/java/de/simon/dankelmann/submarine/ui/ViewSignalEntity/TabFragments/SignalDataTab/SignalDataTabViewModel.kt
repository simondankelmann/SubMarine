package de.simon.dankelmann.submarine.ui.ViewSignalEntity.TabFragments.SignalDataTab

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.Entities.SignalEntity

class SignalDataTabViewModel : ViewModel() {
    val signalEntity = MutableLiveData<SignalEntity?>().apply {
        value = null
    }
}