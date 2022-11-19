package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.HexTab

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Models.SignalGeneratorDataModel
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.Services.SignalAnalyzer
import de.simon.dankelmann.submarine.databinding.FragmentHexTabBinding
import de.simon.dankelmann.submarine.databinding.FragmentTimingsTabBinding
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabFragment
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabViewModel

class HexTabFragment(signalGeneratorDataModel: SignalGeneratorDataModel)  : Fragment() {

    private var _logTag = "HexTab"
    private var _signalGeneratorDataModel:SignalGeneratorDataModel? = null
    private var _binding: FragmentHexTabBinding? = null
    private var _viewModel: HexTabViewModel? = null
    private var _signalAnalyzer = SignalAnalyzer()


    init{
        _signalGeneratorDataModel = signalGeneratorDataModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewModel = ViewModelProvider(this).get(HexTabViewModel::class.java)
        _binding = FragmentHexTabBinding.inflate(inflater, container, false)

        _viewModel!!.signalGeneratorDataModel.postValue(_signalGeneratorDataModel)

        setupUi()

        return _binding!!.root
    }

    fun setupUi(){
        val editTextHexSignal = _binding!!.editTextHexSignal
        _viewModel!!.signalGeneratorDataModel.observe(viewLifecycleOwner) {
            if(it != null && it.signalEntity != null){

                var timingsList:MutableList<Int> = mutableListOf()
                it!!.signalEntity!!.signalData!!.split(",").map {
                    timingsList.add(it.toInt())
                }

                editTextHexSignal.setText("")
                var prepend = ""
                var binaryList = _signalAnalyzer.ConvertTimingsToBinaryStringList(timingsList, it.samplesPerSymbol)
                binaryList.map {
                    val hexString = _signalAnalyzer.ConvertBinaryStringToHexString(it)
                    editTextHexSignal.append(prepend + hexString)
                    prepend = "\n\n"
                }

            } else {
                editTextHexSignal.setText("-")
            }
        }
    }

    fun updateSignalGeneratorDataModel(signalGeneratorDataModel: SignalGeneratorDataModel){
        _signalGeneratorDataModel = signalGeneratorDataModel
        if(_viewModel != null){
            _viewModel!!.signalGeneratorDataModel.postValue(signalGeneratorDataModel)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _viewModel = ViewModelProvider(this).get(HexTabViewModel::class.java)
        // TODO: Use the ViewModel
    }

}