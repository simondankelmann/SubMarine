package de.simon.dankelmann.submarine.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }

    fun updateText(message:String){
        _text.postValue(message)
    }

    val text: LiveData<String> = _text
}