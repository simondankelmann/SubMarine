package de.simon.dankelmann.submarine.Adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.ui.ViewSignalEntity.SignalMapTab.SignalMapTabFragment
import de.simon.dankelmann.submarine.ui.ViewSignalEntity.TabFragments.SignalDataTab.SignalDataTabFragment

class DetectedSignalTabCollectionAdapter (fragment: Fragment, signalEntity: SignalEntity?) : FragmentStateAdapter(fragment) {

    private var _signalEntity:SignalEntity? = null
    init{
        _signalEntity = signalEntity
    }

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return SignalDataTabFragment(_signalEntity)
            1 -> return SignalMapTabFragment(_signalEntity)
        }
        return SignalDataTabFragment(_signalEntity)
    }
}