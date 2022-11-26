package de.simon.dankelmann.submarine.ui.SignalGenerator

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Models.CC1101Configuration
import de.simon.dankelmann.submarine.Models.SignalGeneratorDataModel
import de.simon.dankelmann.submarine.R

class SignalGeneratorViewModel : ViewModel() {

    var signalEntity = MutableLiveData<SignalEntity?>().apply {
        value = null
    }

    var samplesPerSymbol = MutableLiveData<Int>().apply {
        value = 300
    }

    var pausePerLine = MutableLiveData<Int>().apply {
        value = -6000
    }

    val title = MutableLiveData<String>().apply {
        value = "Signal Generator"
    }

    val description = MutableLiveData<String>().apply {
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
}