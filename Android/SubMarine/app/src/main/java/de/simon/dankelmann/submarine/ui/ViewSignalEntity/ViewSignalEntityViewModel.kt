package de.simon.dankelmann.submarine.ui.ViewSignalEntity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ViewSignalEntityViewModel : ViewModel() {



    val signalDetailTitle = MutableLiveData<String>().apply {
        value = "-"
    }

    val signalDetailDescription = MutableLiveData<String>().apply {
        value = "-"
    }

    val signalDetailFrequency = MutableLiveData<String>().apply {
        value = "-"
    }

    val signalDetailData = MutableLiveData<String>().apply {
        value = "-"
    }

    val animationResourceId = MutableLiveData<Int>().apply {
        value = 0
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