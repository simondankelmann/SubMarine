package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.ConfigTab

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Models.CC1101Configuration
import de.simon.dankelmann.submarine.Models.SignalGeneratorDataModel

class ConfigTabViewModel : ViewModel() {
    var signalGeneratorDataModel = MutableLiveData<SignalGeneratorDataModel>().apply {
        value = SignalGeneratorDataModel()
    }

    var cC1101Configuration = MutableLiveData<CC1101Configuration>().apply {
        value = CC1101Configuration()
    }
}