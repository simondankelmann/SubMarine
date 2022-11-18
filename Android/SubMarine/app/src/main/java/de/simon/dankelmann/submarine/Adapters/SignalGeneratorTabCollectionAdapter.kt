package de.simon.dankelmann.submarine.Adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.BinaryTab.BinaryTabFragment
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.ConfigTab.ConfigTabFragment
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.HexTab.HexTabFragment
import de.simon.dankelmann.submarine.ui.SignalGenerator.TabFragments.TimingsTab.TimingsTabFragment
import de.simon.dankelmann.submarine.ui.ViewSignalEntity.SignalMapTab.SignalMapTabFragment
import de.simon.dankelmann.submarine.ui.ViewSignalEntity.TabFragments.SignalDataTab.SignalDataTabFragment

class SignalGeneratorTabCollectionAdapter (fragment: Fragment, signalEntity: SignalEntity?) : FragmentStateAdapter(fragment) {

    private var _signalEntity:SignalEntity? = null

    private var _signalDataTabFragment:SignalDataTabFragment? = null
    private var _signalMapTabFragment:SignalMapTabFragment? = null

    init{
        _signalEntity = signalEntity

        _signalDataTabFragment = SignalDataTabFragment(_signalEntity)
        _signalMapTabFragment = SignalMapTabFragment(_signalEntity)
    }

    fun updateSignalEntity(signalEntity: SignalEntity?){
        _signalEntity = signalEntity
        _signalDataTabFragment!!.updateSignalEntity(signalEntity)
    }

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return TimingsTabFragment.newInstance(_signalEntity)
            1 -> return BinaryTabFragment.newInstance(_signalEntity)
            2 -> return HexTabFragment.newInstance(_signalEntity)
            3 -> return ConfigTabFragment.newInstance(_signalEntity)
        }
        return SignalDataTabFragment.newInstance(_signalEntity)
    }


}