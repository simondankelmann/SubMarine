package de.simon.dankelmann.submarine.ui.ViewSignalEntity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.Entities.SignalEntity

class ViewSignalEntityViewModel : ViewModel() {


    val signalEntity = MutableLiveData<SignalEntity?>().apply {
        value = null
    }

    val animationResourceId = MutableLiveData<Int>().apply {
        value = 0
    }

    val signalDetailTitle = MutableLiveData<String>().apply {
        value = "-"
    }

    val signalDetailDescription = MutableLiveData<String>().apply {
        value = "-"
    }

    val footerText3 = MutableLiveData<String>().apply {
        value = "-"
    }
}