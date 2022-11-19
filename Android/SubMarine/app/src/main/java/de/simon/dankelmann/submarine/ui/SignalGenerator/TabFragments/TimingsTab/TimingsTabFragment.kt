package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Models.SignalGeneratorDataModel
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentSignalDataTabBinding
import de.simon.dankelmann.submarine.databinding.FragmentTimingsTabBinding
import de.simon.dankelmann.submarine.ui.ViewSignalEntity.TabFragments.SignalDataTab.SignalDataTabViewModel

class TimingsTabFragment(signalGeneratorDataModel: SignalGeneratorDataModel) : Fragment() {

    private var _logTag = "TimingsTab"
    private var _signalGeneratorDataModel:SignalGeneratorDataModel? = null
    private var _binding: FragmentTimingsTabBinding? = null
    private var _viewModel: TimingsTabViewModel? = null

    init{
        _signalGeneratorDataModel = signalGeneratorDataModel
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