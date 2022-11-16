package de.simon.dankelmann.submarine.ui.ViewSignalEntity.TabFragments.SignalDataTab

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentSignalDataTabBinding
import de.simon.dankelmann.submarine.databinding.FragmentViewSignalEntityBinding
import de.simon.dankelmann.submarine.ui.SignalMap.SignalMapViewModel
import de.simon.dankelmann.submarine.ui.ViewSignalEntity.SignalMapTab.SignalMapTabFragment

class SignalDataTabFragment (signalEntity: SignalEntity?): Fragment() {

    private var _binding: FragmentSignalDataTabBinding? = null
    private var _signalEntity:SignalEntity? = null
    private var _viewModel:SignalDataTabViewModel? = null

    companion object {
        fun newInstance(signalEntity: SignalEntity?) = SignalDataTabFragment(signalEntity)
    }

    init{
        _signalEntity = signalEntity
    }

    fun updateSignalEntity(signalEntity: SignalEntity?){
        _signalEntity = signalEntity
        _viewModel!!.signalEntity.postValue(signalEntity)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewModel = ViewModelProvider(this).get(SignalDataTabViewModel::class.java)
        _binding = FragmentSignalDataTabBinding.inflate(inflater, container, false)

        _viewModel!!.signalEntity.postValue(_signalEntity)

        setupUi()

        return _binding!!.root
    }

    fun setupUi(){
        val signalDataTextView: TextView = _binding!!.signalDataTabTextView
        _viewModel!!.signalEntity.observe(viewLifecycleOwner) {
            if(it != null){
                signalDataTextView.text = it.signalData
            } else {
                signalDataTextView.text = "Loading..."
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _viewModel = ViewModelProvider(this).get(SignalDataTabViewModel::class.java)
        // TODO: Use the ViewModel
    }

}