package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.BinaryTab

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.Services.SignalAnalyzer
import de.simon.dankelmann.submarine.databinding.FragmentBinaryTabBinding
import de.simon.dankelmann.submarine.databinding.FragmentTimingsTabBinding
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabFragment
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabViewModel

class BinaryTabFragment(signalEntity: SignalEntity?)  : Fragment() {

    private var _logTag = "BinaryTab"
    private var _signalEntity:SignalEntity? = null
    private var _binding: FragmentBinaryTabBinding? = null
    private var _viewModel: BinaryTabViewModel? = null
    private var _signalAnalyzer = SignalAnalyzer()
    private var _symbolsPerBit = 300

    companion object {
        fun newInstance(signalEntity: SignalEntity?) = BinaryTabFragment(signalEntity)
    }

    init{
        _signalEntity = signalEntity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewModel = ViewModelProvider(this).get(BinaryTabViewModel::class.java)
        _binding = FragmentBinaryTabBinding.inflate(inflater, container, false)

        _viewModel!!.signalEntity.postValue(_signalEntity)

        setupUi()

        return _binding!!.root
    }

    fun setupUi(){
        val editTextBinarySignal = _binding!!.editTextBinarySignal
        _viewModel!!.signalEntity.observe(viewLifecycleOwner) {
            if(it != null){

                var timingsList:MutableList<Int> = mutableListOf()
                it!!.signalData!!.split(",").map {
                    timingsList.add(it.toInt())
                }

                editTextBinarySignal.setText("")
                var prepend = ""
                var binaryList = _signalAnalyzer.ConvertTimingsToBinaryStringList(timingsList, _symbolsPerBit)
                binaryList.map {
                    if(it.length > 0){
                        editTextBinarySignal.append(prepend + it)
                        prepend = "\n\n"
                    }

                }
            } else {
                editTextBinarySignal.setText("-")
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _viewModel = ViewModelProvider(this).get(BinaryTabViewModel::class.java)
        // TODO: Use the ViewModel
    }

}