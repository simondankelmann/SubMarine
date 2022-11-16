package de.simon.dankelmann.submarine.ui.ViewSignalEntity.SignalMapTab

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.submarine.BuildConfig
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Entities.LocationEntity
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentSignalDataTabBinding
import de.simon.dankelmann.submarine.databinding.FragmentSignalDatabaseBinding
import de.simon.dankelmann.submarine.databinding.FragmentSignalMapTabBinding
import de.simon.dankelmann.submarine.ui.ViewSignalEntity.TabFragments.SignalDataTab.SignalDataTabViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

class SignalMapTabFragment (signalEntity: SignalEntity?): Fragment() {

    private var _map: MapView? = null
    private var _mapController: IMapController? = null
    private var _initialMapZoom = 20.5
    private lateinit var viewModel: SignalMapTabViewModel
    private var _logTag = "SignalMapTab"

    private var _binding: FragmentSignalMapTabBinding? = null
    private var _signalEntity:SignalEntity? = null
    private var _viewModel:SignalDataTabViewModel? = null

    init{
        _signalEntity = signalEntity
    }

    companion object {
        fun newInstance(signalEntity: SignalEntity?) = SignalMapTabFragment(signalEntity)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewModel = ViewModelProvider(this).get(SignalDataTabViewModel::class.java)
        _binding = FragmentSignalMapTabBinding.inflate(inflater, container, false)
        _viewModel!!.signalEntity.postValue(_signalEntity)

        setupUi()

        return _binding!!.root
    }

    fun setupUi(){
        loadMap(_signalEntity)

        _viewModel!!.signalEntity.observe(viewLifecycleOwner) {
            loadMap(_signalEntity)
        }
    }

    override fun onResume() {
        super.onResume()
        //loadMap(_signalEntity)
        if(_map != null){
            _map!!.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        if(_map != null){
            _map!!.onPause()
        }
    }

    fun loadMap(signalEntity: SignalEntity?){
        if(_signalEntity != null){
            val locationId = _signalEntity!!.locationId
            if(locationId != null ){
                // GET LOCATION ENTITY
                val locationDao = AppDatabase.getDatabase(requireContext()).locationDao()
                CoroutineScope(Dispatchers.IO).launch {
                    val locationEntity = locationDao.getById(locationId)
                    if(locationEntity != null){
                        requireActivity().runOnUiThread {
                            setupMap(locationEntity, _signalEntity!!)
                        }
                    }
                }
            }
        }
    }

    fun setupMap(locationEntity: LocationEntity, signalEntity: SignalEntity){
        if(_binding != null){
            Log.d(_logTag, "Setting up Map")
            var map:MapView = _binding!!.signalMapTapMapView
            if(map != null){
                var signalPoint = GeoPoint(locationEntity.latitude!!, locationEntity.longitude!!)

                // SHOW THE MAP
                map.visibility = View.VISIBLE

                _map = map
                _mapController = map.controller
                Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
                _map!!.setTileSource(TileSourceFactory.MAPNIK);
                _map!!.setMultiTouchControls(true)
                _mapController!!.setZoom(_initialMapZoom)
                _mapController!!.setCenter(signalPoint)

                // INVERT COLOR
                _map!!.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
                _map!!.getOverlayManager().getTilesOverlay().setLoadingBackgroundColor(R.color.background_dark);
                _map!!.getOverlayManager().getTilesOverlay().setLoadingLineColor(R.color.fontcolor_component_dark_inactive);

                var signalMarker = Marker(_map!!)
                signalMarker.position = signalPoint
                var markerIcon = requireActivity().getDrawable(R.drawable.ic_baseline_signal)

                var markerColor = resources.getColor(R.color.fontcolor_component_dark_inactive)
                if(signalEntity.proofOfWork){
                    markerColor = resources.getColor(R.color.accent_color_darkmode)
                }
                markerIcon!!.setTint(markerColor)

                signalMarker.icon = markerIcon
                signalMarker.setAnchor(Marker.ANCHOR_BOTTOM, Marker.ANCHOR_CENTER)
                signalMarker.title = signalEntity.name

                _map!!.overlays.add(signalMarker)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SignalMapTabViewModel::class.java)
        // TODO: Use the ViewModel
    }

}