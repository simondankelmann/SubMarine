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
import de.simon.dankelmann.submarine.ui.SignalMap.SignalMapViewModel

class SignalDataTabFragment (signalEntity: SignalEntity?): Fragment() {



    companion object {
        fun newInstance() = SignalDataTabFragment(null)
    }

    private var _signalEntity:SignalEntity? = null

    init{
        _signalEntity = signalEntity
    }

    private lateinit var viewModel: SignalDataTabViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewModel = ViewModelProvider(this).get(SignalDataTabViewModel::class.java)
        var view = inflater.inflate(R.layout.fragment_signal_data_tab, container, false)

        if(_signalEntity != null){
            viewModel.signalData.postValue(_signalEntity!!.signalData)
        }

        val signalDataTextView: TextView = view.findViewById(R.id.signalDataTabTextView)
        viewModel.signalData.observe(viewLifecycleOwner) {
            signalDataTextView.text = it
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SignalDataTabViewModel::class.java)
        // TODO: Use the ViewModel
    }

}