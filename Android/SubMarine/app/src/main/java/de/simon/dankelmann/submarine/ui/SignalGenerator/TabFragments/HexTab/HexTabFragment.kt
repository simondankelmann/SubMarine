package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.HexTab

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.simon.dankelmann.submarine.Adapters.SignalGeneratorTabCollectionAdapter
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Models.SignalGeneratorDataModel
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.Services.SignalAnalyzer
import de.simon.dankelmann.submarine.databinding.FragmentHexTabBinding
import de.simon.dankelmann.submarine.databinding.FragmentTimingsTabBinding
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabFragment
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabViewModel

class HexTabFragment(signalGeneratorDataModel: SignalGeneratorDataModel, tabCollectionAdapter: SignalGeneratorTabCollectionAdapter)  : Fragment() {

    private var _logTag = "HexTab"
    private var _signalGeneratorDataModel:SignalGeneratorDataModel? = null
    private var _binding: FragmentHexTabBinding? = null
    private var _viewModel: HexTabViewModel? = null
    private var _signalAnalyzer = SignalAnalyzer()
    private var _tabCollectionAdapter:SignalGeneratorTabCollectionAdapter? = null

    private var _hexStringList:MutableList<String> = mutableListOf()


    init{
        _signalGeneratorDataModel = signalGeneratorDataModel
        _tabCollectionAdapter = tabCollectionAdapter
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

        val updateSignalEntityButton = _binding!!.updateSignalEntityButtonHex
        val editTextHexSignal = _binding!!.editTextHexSignal

        updateSignalEntityButton.setOnClickListener{
            // GET HEX STRINGS FROM UI
            var hexStringFromUi = editTextHexSignal.text
            var hexStringLines = hexStringFromUi.split("\n")
            var signalTimings: MutableList<String> = mutableListOf()

            hexStringLines.map {
                var timings = _signalAnalyzer.convertHexStringToTimingsList(it, _signalGeneratorDataModel!!.samplesPerSymbol)
                timings.map {
                    signalTimings.add(it)
                }
                // ADD A PAUSE AFTER EACH LINE
                signalTimings.add(_signalGeneratorDataModel!!.pauseBetweenLines.toString())
            }

            var signalDataString = signalTimings.joinToString(",")
            var signalDataLength = signalTimings.size

            _signalGeneratorDataModel!!.signalEntity!!.signalData = signalDataString
            _signalGeneratorDataModel!!.signalEntity!!.signalDataLength = signalDataLength

            _tabCollectionAdapter!!.updateSignalGeneratorDataModel(_signalGeneratorDataModel!!)
        }


        _viewModel!!.signalGeneratorDataModel.observe(viewLifecycleOwner) {
            if(it != null && it.signalEntity != null){

                var timingsList:MutableList<Int> = mutableListOf()
                it!!.signalEntity!!.signalData!!.split(",").map {
                    timingsList.add(it.toInt())
                }

                editTextHexSignal.setText("")
                var prepend = ""
                var binaryList = _signalAnalyzer.ConvertTimingsToBinaryStringList(timingsList, it.samplesPerSymbol)

                _hexStringList = mutableListOf()

                binaryList.map {
                    val hexString = _signalAnalyzer.ConvertBinaryStringToHexString(it)
                    _hexStringList.add(hexString)
                    editTextHexSignal.append(prepend + hexString)
                    prepend = "\n"
                }

            } else {
                editTextHexSignal.setText("-")
            }
        }
    }

    fun getTimings():List<String>{
        var timingsList:MutableList<String> = mutableListOf()

        _hexStringList.map {
            var convertedTimings = _signalAnalyzer.convertHexStringToTimingsList(it, _signalGeneratorDataModel!!.samplesPerSymbol)
            convertedTimings.map {
                timingsList.add(it)
            }
            // PAUSE BETWEEN THE LINES !
            timingsList.add(_signalGeneratorDataModel!!.pauseBetweenLines.toString())
        }

        return timingsList.toList()
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