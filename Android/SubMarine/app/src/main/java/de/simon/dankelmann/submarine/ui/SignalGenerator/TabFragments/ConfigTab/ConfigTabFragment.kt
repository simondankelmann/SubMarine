package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.ConfigTab

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import de.simon.dankelmann.submarine.Adapters.SignalGeneratorTabCollectionAdapter
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Models.CC1101Configuration
import de.simon.dankelmann.submarine.Models.SignalGeneratorDataModel
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.Services.SubMarineService
import de.simon.dankelmann.submarine.databinding.FragmentConfigTabBinding
import de.simon.dankelmann.submarine.databinding.FragmentTimingsTabBinding
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabFragment
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabViewModel

class ConfigTabFragment(signalGeneratorDataModel: SignalGeneratorDataModel, tabCollectionAdapter: SignalGeneratorTabCollectionAdapter)  : Fragment() {

    private var _logTag = "ConfigTab"
    private var _signalGeneratorDataModel:SignalGeneratorDataModel? = null
    private var _binding: FragmentConfigTabBinding? = null
    private var _viewModel: ConfigTabViewModel? = null
    private var _tabCollectionAdapter:SignalGeneratorTabCollectionAdapter? = null
    private var _submarineService: SubMarineService = AppContext.submarineService

    init{
        _signalGeneratorDataModel = signalGeneratorDataModel
        _tabCollectionAdapter = tabCollectionAdapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewModel = ViewModelProvider(this).get(ConfigTabViewModel::class.java)
        _binding = FragmentConfigTabBinding.inflate(inflater, container, false)

        _viewModel!!.signalGeneratorDataModel.postValue(_signalGeneratorDataModel)

        var configuration = CC1101Configuration()
        configuration.loadFromSignalEntity(_signalGeneratorDataModel?.signalEntity!!)
        _viewModel!!.cC1101Configuration.postValue(configuration)

        setupUi()

        return _binding!!.root
    }

    fun setupUi(){
        val editTextFrequency = _binding!!.editTextViewMhz
        val spinnerModulation = _binding!!.modulationSpinner
        var spinnerAdapter: ArrayAdapter<String>? = null
        val editTextDrate = _binding!!.editTextViewDrate
        val editTextRxBw = _binding!!.editTextViewRxBw
        val spinnerPktFormat = _binding!!.pktFormatSpinner
        var spinnerAdapterPktFormat: ArrayAdapter<String>? = null
        val updateSignalEntityButton = _binding!!.updateSignalEntityButtonConfig

        updateSignalEntityButton.setOnClickListener{
            var configuration = getCC1101ConfigurationFromUi()
            _submarineService.setConfigurationToSignalEntity(_signalGeneratorDataModel!!.signalEntity!!, configuration)
            _tabCollectionAdapter!!.updateSignalGeneratorDataModel(_signalGeneratorDataModel!!)
        }

        if (spinnerModulation != null) {
            spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_simple, resources.getStringArray(R.array.Modulations))
            spinnerModulation.adapter = spinnerAdapter
        }

        if (spinnerPktFormat != null) {
            spinnerAdapterPktFormat = ArrayAdapter(requireContext(), R.layout.spinner_item_simple, resources.getStringArray(R.array.PktFormats))
            spinnerPktFormat.adapter = spinnerAdapterPktFormat
        }

        _viewModel!!.cC1101Configuration.observe(viewLifecycleOwner) {
            editTextFrequency.setText(it.mhz.toString())
            if(spinnerAdapter != null){
                spinnerModulation.setSelection(it.modulation)
            }
            editTextDrate.setText(it.dRate.toString())
            editTextRxBw.setText(it.rxBw.toString())
            if(spinnerAdapterPktFormat != null){
                spinnerPktFormat.setSelection(it.pktFormat)
            }
        }






    }

    fun updateSignalGeneratorDataModel(signalGeneratorDataModel: SignalGeneratorDataModel){
        _signalGeneratorDataModel = signalGeneratorDataModel
        if(_viewModel != null){
            _viewModel!!.signalGeneratorDataModel.postValue(signalGeneratorDataModel)
        }
    }

    fun getCC1101ConfigurationFromUi(): CC1101Configuration {
        var configuration = CC1101Configuration()

        val editTextFrequency = _binding!!.editTextViewMhz
        //val editTextModulation = binding.editTextViewModulation
        val spinnerModulation = _binding!!.modulationSpinner
        val editTextDrate = _binding!!.editTextViewDrate
        val editTextRxBw = _binding!!.editTextViewRxBw
        //val editTextPktFormat = binding.editTextViewPktFormat
        val spinnerPktFormat = _binding!!.pktFormatSpinner

        configuration.mhz = editTextFrequency.text.toString().toFloat()
        //configuration.modulation = editTextModulation.text.toString().toInt()
        configuration.modulation = spinnerModulation.selectedItemPosition
        configuration.dRate = editTextDrate.text.toString().toInt()
        configuration.rxBw = editTextRxBw.text.toString().toFloat()
        //configuration.pktFormat = editTextPktFormat.text.toString().toInt()
        configuration.pktFormat = spinnerPktFormat.selectedItemPosition

        Log.d(_logTag, "ConfigString from UI:")

        return configuration
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _viewModel = ViewModelProvider(this).get(ConfigTabViewModel::class.java)
        // TODO: Use the ViewModel
    }

}