package de.simon.dankelmann.submarine.ui.ViewSignalEntity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.BuildConfig
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Entities.LocationEntity
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Interfaces.SubmarineResultListenerInterface
import de.simon.dankelmann.submarine.Models.SubmarineCommand
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentViewSignalEntityBinding
import de.simon.dankelmann.submarine.Services.SubMarineService
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

class ViewSignalEntityFragment : Fragment(), SubmarineResultListenerInterface{

    private var _binding: FragmentViewSignalEntityBinding? = null
    private val _logTag = "SignalEntityFragment"
    private var _viewModel: ViewSignalEntityViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    //private var _bluetoothSerial: BluetoothSerial? = null
    //private var _signalEntity: SignalEntity? = null
    private var _isConnected:Boolean = false
    private var _submarineService: SubMarineService = AppContext.submarineService
    private var _signalEntity:SignalEntity? = null
    private var _signalEntityId = 0
    private var _locationId = -1

    private var _map: MapView? = null
    private var _mapController: IMapController? = null
    private var _initialMapZoom = 20.5

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("LongLogTag")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel =
            ViewModelProvider(this).get(ViewSignalEntityViewModel::class.java)
        _viewModel = viewModel

        _binding = FragmentViewSignalEntityBinding.inflate(inflater, container, false)
        val root: View = binding.root

        var signalEntityId = arguments?.getInt("SignalEntityId") as Int
        if(signalEntityId != null){
            _signalEntityId = signalEntityId
            // LOAD DATA FROM DB
            val signalDao = AppDatabase.getDatabase(requireContext()).signalDao()
            val locationDao = AppDatabase.getDatabase(requireContext()).locationDao()
            CoroutineScope(Dispatchers.IO).launch {
                val signalEntity = signalDao.getById(_signalEntityId)
                if(signalEntity != null){
                    _signalEntity = signalEntity

                    if(signalEntity.locationId != null){
                        _locationId = signalEntity.locationId!!
                    }

                    _viewModel!!.signalEntity.postValue(signalEntity)
                    Log.d(_logTag, "POW IS: " + signalEntity.proofOfWork)

                    //MAP SETUP
                    if(_signalEntity != null && _locationId != -1){
                        val locationEntity = locationDao.getById(_locationId)
                        if(locationEntity != null){
                            setupMap(locationEntity, _signalEntity!!)
                        }
                    } else {
                        Log.d(_logTag, "No Map Setup")
                    }
                }
            }

            setupUi()

            _submarineService.addResultListener(this)
            _submarineService.connect()
        }
        return root
    }

    fun setupUi(){
        // SETUP UI
        val replayButton: ImageButton = binding.replaySignalDetailButton
        val animationView: LottieAnimationView = binding.animationSignalDetail
        val mapView = binding.signalMapView
        val titleText: EditText = binding.textViewSignalDetailTitle
        val descriptionText: TextView = binding.textViewSignalDetailDescription
        //val frequencyText: TextView = binding.textViewSignalFrequency
        val dataText: EditText = binding.textViewSignalDetailData
        val footerText1: TextView = binding.textViewFooter1
        val footerText2: TextView = binding.textViewFooter2
        val footerText3: TextView = binding.textViewFooter3

        val powButton: ImageButton = binding.proofOfWorkButton
        var layoutPowButton:LinearLayout = binding.layoutPOWButton

        val saveButton: ImageButton = binding.SaveButton


        _viewModel!!.footerText3.observe(viewLifecycleOwner) {
            footerText3.text = it
        }

        _viewModel!!.signalEntity.observe(viewLifecycleOwner) {
            if(_viewModel!!.signalEntity.value != null){
                _signalEntity = it!!

                if(_viewModel!!.signalEntity.value!!.locationId != null){
                    _locationId = _viewModel!!.signalEntity.value!!.locationId!!
                }

                titleText.setText( _viewModel!!.signalEntity.value!!.name)
                descriptionText.text = _viewModel!!.signalEntity.value!!.frequency.toString() + " Mhz" + " | " +_viewModel!!.signalEntity.value!!.signalDataLength.toString() + " Samples"
                //frequencyText.text =
                dataText.setText( _viewModel!!.signalEntity.value!!.signalData)
                footerText1.text = getModulationString(_viewModel!!.signalEntity.value!!.modulation!!) + " | " + _viewModel!!.signalEntity.value!!.type!!
                footerText2.text = "RX-BW: " + _viewModel!!.signalEntity.value!!.rxBw.toString()+" Khz"

                if(_viewModel!!.signalEntity.value!!.proofOfWork){
                    layoutPowButton.background.setTint(resources.getColor(R.color.backgroundcolor_component_dark_active))
                    powButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.fontcolor_component_dark_active))
                } else {
                    layoutPowButton.background.setTint(resources.getColor(R.color.backgroundcolor_component_dark_inactive))
                    powButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.fontcolor_component_dark_inactive))
                }
            }
        }

        _viewModel!!.animationResourceId.postValue(R.raw.inspect_signal)
        _viewModel!!.animationResourceId.observe(viewLifecycleOwner) {
            animationView.setAnimation(it)
            animationView.playAnimation()

            if(it == R.raw.save_folder){
                animationView.repeatCount = 0
            } else {
                animationView.repeatCount = LottieDrawable.INFINITE
            }
        }

        // REPLAY
        replayButton.setOnClickListener { view ->
            if(_viewModel!!.signalEntity.value != null && _submarineService.isConnected()){
                replaySignalEntity(_viewModel!!.signalEntity.value!!)
            }

            if(!_isConnected){
                _viewModel!!.animationResourceId.postValue(R.raw.warning)
            }
        }

        // TOGGLE POW
        powButton.setOnClickListener { view ->
            if(_viewModel!!.signalEntity.value != null){
                var newState = !_viewModel!!.signalEntity.value!!.proofOfWork
                var signalId = _viewModel!!.signalEntity.value!!.uid

                CoroutineScope(Dispatchers.IO).launch {
                    val signalDao = AppDatabase.getDatabase(requireContext()).signalDao()
                    signalDao.updateProofOfWork(signalId, newState)

                    val signalEntity = signalDao.getById(signalId)
                    if(signalEntity != null){
                        _viewModel!!.signalEntity.postValue(signalEntity)
                    }
                }
            }
        }

        // SAVE CHANGES
        saveButton.setOnClickListener { view ->
            if(_viewModel!!.signalEntity.value != null){
                var signalId = _viewModel!!.signalEntity.value!!.uid
                var signalName = titleText.text.toString()
                var signalData = dataText.text.toString()
                var signalDataLength = dataText.text.toString().split(",").size

                CoroutineScope(Dispatchers.IO).launch {
                    val signalDao = AppDatabase.getDatabase(requireContext()).signalDao()
                    signalDao.updateValues(signalId, signalName, signalData, signalDataLength )
                    val signalEntity = signalDao.getById(signalId)
                    if(signalEntity != null){
                        _viewModel!!.signalEntity.postValue(signalEntity)

                        _viewModel!!.animationResourceId.postValue(R.raw.save_folder)
                    }
                }
            }
        }
    }

    fun setupMap(locationEntity:LocationEntity, signalEntity: SignalEntity){
        if(_binding != null){
            Log.d(_logTag, "Setting up Map")
            var map = _binding!!.signalMapView
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


    fun getModulationString(modulation:Int):String{
        when(modulation){
            0 -> return "2-FSK"
            1 -> return "GFSK"
            2 -> return "ASK/OOK"
            3 -> return "4-FSK"
            4 -> return "MSK"
        }
        return "Unknown"
    }

    fun getConfigurationStringFromSignalEntity(signalEntity: SignalEntity):String{

        var mhz = signalEntity.frequency.toString()
        while(mhz.length < 6){
            mhz = "0$mhz"
        }

        var tx = "1"
        var modulation = signalEntity.modulation.toString()

        var dRate = signalEntity.dRate.toString()
        while(dRate.length < 3){
            dRate = "0$dRate"
        }

        var rxBw = signalEntity.rxBw.toString()
        while(rxBw.length < 6){
            rxBw = "0$rxBw"
        }

        var pktFormat = signalEntity.pktFormat.toString()

        var lqi = signalEntity.lqi.toString()
        while(lqi.length < 6){
            lqi = "0$lqi"
        }

        var rssi = signalEntity.rssi.toString()
        while(rssi.length < 6){
            rssi = "0$rssi"
        }

        /*
        Adapter Configuration Structure:
        Bytes:
        0-5 => MHZ
        6 => TX
        7 => MODULATION
        8-10 => DRATE
        11-16 => RX_BW
        17 => PKT_FORMAT
        18-23 => AVG_LQI
        24-29 => AVG_RSSI
        */

        return mhz + tx + modulation + dRate + rxBw + pktFormat + lqi + rssi
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun replaySignalEntity(signalEntity: SignalEntity){
        _viewModel!!.animationResourceId.postValue(R.raw.wave2)
        _viewModel!!.signalDetailDescription.postValue("Transmitting Signal to Sub Marine...")
        _submarineService.sendCommandToDevice(SubmarineCommand(Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND,Constants.COMMAND_ID_DUMMY, getConfigurationStringFromSignalEntity(signalEntity) + signalEntity.signalData))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun receivedDataCallback(message: String){
        if(message != ""){
            Log.d(_logTag, "Received: " + message)

            // PARSE COMMAND AND DATA
            var incomingCommand = message.substring(0,4)
            var incomingCommandId = message.substring(4,8)

            Log.d(_logTag, "Icoming Command: " + incomingCommand)
            Log.d(_logTag, "Icoming Command Id: " + incomingCommandId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun replayStatusCallback(message: String){
        _viewModel!!.signalDetailDescription.postValue("Executing Signal now")
        _viewModel!!.animationResourceId.postValue(R.raw.sinus)

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            setOperationMode(Constants.OPERATIONMODE_IDLE)
            requireActivity().runOnUiThread {
                _viewModel!!.signalDetailDescription.postValue(_viewModel!!.signalEntity.value!!.signalDataLength.toString() + " Samples")
                _viewModel!!.animationResourceId.postValue(R.raw.inspect_signal)
            }
        }, 1500)

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setOperationMode(operationMode:String){
        val command = Constants.COMMAND_SET_OPERATION_MODE
        val commandId = Constants.COMMAND_ID_DUMMY
        val commandString = command + commandId + operationMode

        _submarineService.setOperationMode(operationMode)
        //_bluetoothSerial!!.sendByteString(commandString + "\n", ::setOperationModeCallback)
    }

    private fun setOperationModeCallback(message: String){
        Log.d(_logTag, "OP MODE CB: " + message)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
                _isConnected = false
                _viewModel!!.footerText3.postValue("Disconnected")
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
                _isConnected = false
                _viewModel!!.footerText3.postValue("Connecting...")
            }
            2 -> {
                Log.d(_logTag, "Connected")
                _isConnected = true
                _viewModel!!.footerText3.postValue("Connected")
                _viewModel!!.animationResourceId.postValue(R.raw.inspect_signal)
            }
        }
    }

    override fun onDestroyView() {
        _submarineService.removeResultListener(this)
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        _submarineService.removeResultListener(this)
        super.onPause()
    }

    override fun onResume() {
        _submarineService.addResultListener(this)
        super.onResume()
    }

    override fun onConnectionStateChanged(connectionState: Int) {
        connectionStateChangedCallback(connectionState)
    }

    override fun onIncomingData(data: String, command: SubmarineCommand?) {
        receivedDataCallback(data)
    }

    override fun onOutgoingData(timeElapsed: Int, command: SubmarineCommand?) {
        // NOT IN USE
    }

    override fun onCommandSent(timeElapsed: Int, command: SubmarineCommand) {
        // NOT IN USE
    }

    override fun onOperationModeSet(timeElapsed: Int, command: SubmarineCommand) {
        setOperationModeCallback(command.getCommandString())
    }

    override fun onSignalReplayed(timeElapsed: Int, command: SubmarineCommand) {
        replayStatusCallback(command.getCommandString())
    }
}