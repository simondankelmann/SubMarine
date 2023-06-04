package de.simon.dankelmann.submarine.ui.SignalMap

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simon.dankelmann.submarine.BuildConfig
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Interfaces.LocationResultListener
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentSignalMapBinding
import de.simon.dankelmann.submarine.Services.LocationService
import de.simon.dankelmann.submarine.SubGhzDecoders.SubGhzDecoderRegistry
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


class SignalMapFragment : Fragment(), LocationResultListener {

    private var _binding: FragmentSignalMapBinding? = null
    private val _logTag = "SignalMapFragment"
    private var _viewModel: SignalMapViewModel? = null
    private var _locationService:LocationService? = null
    private var _map: MapView? = null
    private var _mapController: IMapController? = null
    private var _initialMapZoom = 20.5

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _firstReceivedLocation:Location? = null
    private var _lastReceivedLocation:Location? = null


    // MY POSISTION MARKER
    var _myPositionMarker:Marker? = null

    var _signalMarkerList:MutableList<Marker> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel =
            ViewModelProvider(this).get(SignalMapViewModel::class.java)
        _viewModel = viewModel


        _binding = FragmentSignalMapBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // CALL SETUP UI AFTER _viewModel and _binding are set up
        setupUi()

        // REQUEST LOCATION UPDATES
        listenForLocations()

        return root
    }

    fun listenForLocations(){
        _locationService = LocationService(requireContext(), this)
    }

    fun setupUi(){
        // SETUP UI
        val titleTextView: TextView = binding.textViewSignalMapTitle
        _viewModel!!.title.observe(viewLifecycleOwner) {
            titleTextView.text = it
        }

        val descriptionTextView: TextView = binding.textViewSignalMapDescription
        _viewModel!!.description.observe(viewLifecycleOwner) {
            descriptionTextView.text = it
        }

        val footerTextView1: TextView = binding.textviewFooter1
        _viewModel!!.footerText1.observe(viewLifecycleOwner) {
            footerTextView1.text = it
        }

        val centerMapButton: TextView = binding.centerButton
        centerMapButton.setOnClickListener{
            if(_lastReceivedLocation != null){
                locateMe(_lastReceivedLocation!!)
            }
        }

        val loadSignalsButton: TextView = binding.loadSignalsButton
        loadSignalsButton.setOnClickListener{
            addSignalMarkersToMap()
        }
    }

    fun setupMap(){
        if(_binding != null){
            Log.d(_logTag, "Setting up Map")
            var map = _binding!!.signalMapView
            if(map != null){
                _map = map
                _mapController = _map!!.controller
                Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
                //_map!!.setTileSource(TileSourceFactory.MAPNIK);
                _map!!.setTileSource(TileSourceFactory.MAPNIK);
                _map!!.setMultiTouchControls(true)
                _mapController!!.setZoom(_initialMapZoom)

                // INVERT COLOR
                _map!!.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
                _map!!.getOverlayManager().getTilesOverlay().setLoadingBackgroundColor(R.color.background_dark);
                _map!!.getOverlayManager().getTilesOverlay().setLoadingLineColor(R.color.fontcolor_component_dark_inactive);

                if(_firstReceivedLocation != null){
                    setMapCenter(_firstReceivedLocation!!)
                }

                addSignalMarkersToMap()
            }
        }
    }

    fun setMapCenter(location:Location){
        if(_map != null){
            var point = GeoPoint(location)
            _mapController!!.setCenter(point)
        } else {
            setupMap()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        setupMap()
        listenForLocations()
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

    fun locateMe(location: Location){
        setMapCenter(location)
        if(_mapController != null){
            _mapController!!.setZoom(_initialMapZoom)
        }
    }

    fun addSignalMarkersToMap(){
        if(_map != null){
            // CLEAR THE LIST
            _signalMarkerList = mutableListOf()

            // LOAD DATA FROM DB
            val signalDao = AppDatabase.getDatabase(requireContext()).signalDao()
            val locationDao = AppDatabase.getDatabase(requireContext()).locationDao()
            CoroutineScope(Dispatchers.IO).launch {
                val signalEntities = signalDao.getAll()

                // REMOVE MARKERS
                _signalMarkerList.map {
                    _map!!.overlays.remove(it)
                    _map!!.invalidate()
                }

                signalEntities.map {
                    if(it.locationId != null && it.locationId!! > 0){

                        val locationEntity = locationDao.getById(it.locationId!!)
                        var signalPoint = GeoPoint(locationEntity.latitude!!, locationEntity.longitude!!)

                        var signalMarker = Marker(_map!!)
                        signalMarker.position = signalPoint
                        var markerIcon = requireActivity().getDrawable(R.drawable.ic_baseline_signal)

                        var markerColor = resources.getColor(R.color.fontcolor_component_dark_inactive)
                        if(it.proofOfWork){
                            markerColor = resources.getColor(R.color.accent_color_darkmode)
                        }

                        // CHECK IF THE SIGNALS PROTOCOL CAN BE DETECTED
                        var validDecoders = SubGhzDecoderRegistry().validateSignal(it)
                        if(validDecoders.isNotEmpty() && !it.proofOfWork){
                            markerColor = resources.getColor(validDecoders.first().getColorId())
                        }

                        markerIcon!!.setTint(markerColor)

                        signalMarker.icon = markerIcon
                        signalMarker.setAnchor(Marker.ANCHOR_BOTTOM, Marker.ANCHOR_CENTER)
                        signalMarker.title = it.name


                        val infoWindow = SignalMarkerInfoWindow(_map!!, requireActivity(), it, validDecoders)
                        signalMarker.infoWindow = infoWindow

                        _signalMarkerList.add(signalMarker)
                        _viewModel!!.footerText1.postValue(_signalMarkerList.size.toString() + " Signals")
                        _map!!.overlays.add(signalMarker)

                    }

                }

                _map!!.invalidate()
            }
        }


    }

    fun onSignalMarkerClicked(marker:Marker, map:MapView){

    }

    override fun receiveLocationChanges(location: Location) {
        _lastReceivedLocation = location
        if(_myPositionMarker == null){
            if(_map != null){
                // INIT THE MARKER
                _myPositionMarker = Marker(_map!!)
                _myPositionMarker!!.position = GeoPoint(location)
                _myPositionMarker!!.icon = requireActivity().getDrawable(R.drawable.my_location_24)
                _myPositionMarker!!.setAnchor(Marker.ANCHOR_BOTTOM, Marker.ANCHOR_CENTER)
                _myPositionMarker!!.title = "My Location"
                _map!!.overlays.add(_myPositionMarker!!)
                _map!!.invalidate()
            }
        } else {
            _myPositionMarker!!.position = GeoPoint(location)
        }

        if(_firstReceivedLocation == null){
            _firstReceivedLocation = location
            setMapCenter(_firstReceivedLocation!!)
        }
    }

}