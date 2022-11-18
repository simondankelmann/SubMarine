package de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.ConfigTab

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentConfigTabBinding
import de.simon.dankelmann.submarine.databinding.FragmentTimingsTabBinding
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabFragment
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabViewModel

class ConfigTabFragment(signalEntity: SignalEntity?)  : Fragment() {

    private var _logTag = "ConfigTab"
    private var _signalEntity:SignalEntity? = null
    private var _binding: FragmentConfigTabBinding? = null
    private var _viewModel: ConfigTabViewModel? = null

    companion object {
        fun newInstance(signalEntity: SignalEntity?) = ConfigTabFragment(signalEntity)
    }

    init{
        _signalEntity = signalEntity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewModel = ViewModelProvider(this).get(ConfigTabViewModel::class.java)
        _binding = FragmentConfigTabBinding.inflate(inflater, container, false)

        _viewModel!!.signalEntity.postValue(_signalEntity)

        setupUi()

        return _binding!!.root
    }

    fun setupUi(){

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _viewModel = ViewModelProvider(this).get(ConfigTabViewModel::class.java)
        // TODO: Use the ViewModel
    }

}