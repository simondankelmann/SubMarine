package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.HexTab

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.Services.SignalAnalyzer
import de.simon.dankelmann.submarine.databinding.FragmentHexTabBinding
import de.simon.dankelmann.submarine.databinding.FragmentTimingsTabBinding
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabFragment
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabViewModel

class HexTabFragment(signalEntity: SignalEntity?)  : Fragment() {

    private var _logTag = "HexTab"
    private var _signalEntity:SignalEntity? = null
    private var _binding: FragmentHexTabBinding? = null
    private var _viewModel: HexTabViewModel? = null
    private var _signalAnalyzer = SignalAnalyzer()
    private var _symbolsPerBit = 300

    companion object {
        fun newInstance(signalEntity: SignalEntity?) = HexTabFragment(signalEntity)
    }

    init{
        _signalEntity = signalEntity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewModel = ViewModelProvider(this).get(HexTabViewModel::class.java)
        _binding = FragmentHexTabBinding.inflate(inflater, container, false)

        _viewModel!!.signalEntity.postValue(_signalEntity)

        setupUi()

        return _binding!!.root
    }

    fun setupUi(){
        val editTextHexSignal = _binding!!.editTextHexSignal
        _viewModel!!.signalEntity.observe(viewLifecycleOwner) {
            if(it != null){

                var timingsList:MutableList<Int> = mutableListOf()
                it!!.signalData!!.split(",").map {
                    timingsList.add(it.toInt())
                }

                editTextHexSignal.setText("")
                var prepend = ""
                var binaryList = _signalAnalyzer.ConvertTimingsToBinaryStringList(timingsList, _symbolsPerBit)
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _viewModel = ViewModelProvider(this).get(HexTabViewModel::class.java)
        // TODO: Use the ViewModel
    }

}