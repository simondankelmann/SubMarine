package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab

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
import de.simon.dankelmann.submarine.databinding.FragmentSignalDataTabBinding
import de.simon.dankelmann.submarine.databinding.FragmentTimingsTabBinding
import de.simon.dankelmann.submarine.ui.ViewSignalEntity.TabFragments.SignalDataTab.SignalDataTabViewModel

class TimingsTabFragment(signalGeneratorDataModel: SignalGeneratorDataModel, tabCollectionAdapter: SignalGeneratorTabCollectionAdapter) : Fragment() {

    private var _logTag = "TimingsTab"
    private var _signalGeneratorDataModel:SignalGeneratorDataModel? = null
    private var _binding: FragmentTimingsTabBinding? = null
    private var _viewModel: TimingsTabViewModel? = null
    private var _tabCollectionAdapter:SignalGeneratorTabCollectionAdapter? = null

    init{
        _signalGeneratorDataModel = signalGeneratorDataModel
        _tabCollectionAdapter = tabCollectionAdapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewModel = ViewModelProvider(this).get(TimingsTabViewModel::class.java)
        _binding = FragmentTimingsTabBinding.inflate(inflater, container, false)

        _viewModel!!.signalGeneratorDataModel.postValue(_signalGeneratorDataModel)

        setupUi()

        return _binding!!.root
    }

    fun setupUi(){
        val editTextTimings = _binding!!.editTextSignalTimings
        val updateSignalEntityButton = _binding!!.updateSignalEntityButtonTimings

        updateSignalEntityButton.setOnClickListener{
            var timingsList:MutableList<String> = mutableListOf()

            val textFromUi = editTextTimings.text
            textFromUi.split(",").map {
                var parsedInt = it.toInt()
                timingsList.add(parsedInt.toString())
            }

            var newTimingsText = timingsList.joinToString(",")

            _signalGeneratorDataModel!!.signalEntity!!.signalData = newTimingsText
            _signalGeneratorDataModel!!.signalEntity!!.signalDataLength = timingsList.size
            _tabCollectionAdapter!!.updateSignalGeneratorDataModel(_signalGeneratorDataModel!!)

        }

        _viewModel!!.signalGeneratorDataModel.observe(viewLifecycleOwner) {
            if(it != null && it.signalEntity != null){
                editTextTimings.setText(it!!.signalEntity!!.signalData)
            } else {
                editTextTimings.setText("-")
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
        _viewModel = ViewModelProvider(this).get(TimingsTabViewModel::class.java)
        // TODO: Use the ViewModel
    }

}