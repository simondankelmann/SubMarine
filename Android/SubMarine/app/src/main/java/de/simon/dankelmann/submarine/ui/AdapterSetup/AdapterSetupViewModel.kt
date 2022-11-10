package de.simon.dankelmann.submarine.ui.AdapterSetup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.Models.CC1101Configuration
import de.simon.dankelmann.submarine.R

class AdapterSetupViewModel : ViewModel() {

    val title = MutableLiveData<String>().apply {
        value = "Adapter Setup"
    }

    val description = MutableLiveData<String>().apply {
        value = "Setup CC1101 Configuration"
    }

    val animationResourceId = MutableLiveData<Int>().apply {
        value = R.raw.configuration
    }

    val cC1101Configuration = MutableLiveData<CC1101Configuration>().apply {
        value = CC1101Configuration()
    }

    val footerText1 = MutableLiveData<String>().apply {
        value = "-"
    }
}