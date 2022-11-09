package de.simon.dankelmann.submarine.ui.periscope

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airbnb.lottie.LottieAnimationView
import de.simon.dankelmann.esp32_subghz.ui.connectedDevice.PeriscopeViewModel
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Entities.LocationEntity
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Interfaces.LocationResultListener
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentPeriscopeBinding
import de.simon.dankelmann.submarine.permissioncheck.PermissionCheck
import de.simon.dankelmann.submarine.services.BluetoothSerial
import de.simon.dankelmann.submarine.services.ForegroundService
import de.simon.dankelmann.submarine.services.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.math.round
import kotlin.math.roundToLong


class PeriscopeFragment: Fragment(), LocationResultListener {
    private val _logTag = "PeriscopeFragment"
    private var _binding: FragmentPeriscopeBinding? = null
    private var _viewModel: PeriscopeViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    private var _bluetoothSerial: BluetoothSerial? = null

    private var _capturedSignals = 0
    private var _lastIncomingSignalData = ""
    private var _lastIncomingCc1101Config = ""

    private var _animationView:LottieAnimationView? = null
    private var _locationService:LocationService? = null

    private var _lastLocation:Location? = null
    private var _lastLocationDateTime:LocalDateTime? = null

    // Notification Service Intent
    var serviceIntent: Intent? = null


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(PeriscopeViewModel::class.java)
        _viewModel = viewModel

        _binding = FragmentPeriscopeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // REQUEST LOCATION UPDATES
        _locationService = LocationService(requireContext(), this)

        // GET DATA FROM BUNDLE
        var deviceFromBundle = arguments?.getParcelable("Device") as BluetoothDevice?
        if(deviceFromBundle != null){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
                //_viewModel?.updateText(deviceFromBundle.name + " - " + deviceFromBundle.address)
                _bluetoothDevice = deviceFromBundle

                // LETS GO !
                _bluetoothSerial = BluetoothSerial(requireContext(), ::connectionStateChangedCallback)
                Thread(Runnable {
                    _bluetoothSerial?.connect(deviceFromBundle.address, ::receivedDataCallback)
                }).start()

            }
        }

        // SET UP UI
        val description: TextView = binding.textViewPersicopeDescription
        _viewModel!!.description.observe(viewLifecycleOwner) {
            description.text = it
        }

        val capturedSignalInfo: TextView = binding.textViewCapturedSignalInfo
        _viewModel!!.capturedSignalInfo.observe(viewLifecycleOwner) {
            capturedSignalInfo.text = it
        }

        val capturedSignalData: TextView = binding.textViewCapturedSignalData
        _viewModel!!.capturedSignalData.observe(viewLifecycleOwner) {
            capturedSignalData.text = it
        }

        val infoTextFooter: TextView = binding.textViewSignalCounter
        _viewModel!!.infoTextFooter.observe(viewLifecycleOwner) {
            infoTextFooter.text = it
        }

        val connectionState: TextView = binding.textViewConnectionState
        _viewModel!!.connectionState.observe(viewLifecycleOwner) {
            connectionState.text = it
        }

        val locationInfo: TextView = binding.textViewLocation
        _viewModel!!.locationInfo.observe(viewLifecycleOwner) {
            locationInfo.text = it
        }

        // REPLAY BUTTON
        val replayButton: Button = binding.replaySignalButton
        replayButton.setOnClickListener { view ->
            // REPLAY
            _viewModel!!.updateDescription("Transmitting Signal to Sub Marine...")
            if(_lastIncomingSignalData != ""){
                requireActivity().runOnUiThread {
                    _binding!!.animationPeriscope!!.setAnimation(R.raw.wave2)
                    _binding!!.animationPeriscope.playAnimation()
                }
                _viewModel!!.updateDescription("Transmitting Signal to Sub Marine Device")

                Log.d(_logTag, "ReTransmitting: " + _lastIncomingSignalData)

                val command = Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND
                val commandId = Constants.COMMAND_ID_DUMMY
                val commandString = command + commandId + _lastIncomingCc1101Config + _lastIncomingSignalData

                _bluetoothSerial!!.sendByteString(commandString + "\n", ::replayStatusCallback)
            }
        }

        // ANIMATION VIEW
        _animationView = binding.animationPeriscope

        // Start Foreground Service to scan Locations in Background
        serviceIntent = Intent(requireContext(), ForegroundService::class.java)
        serviceIntent!!.putExtra("inputExtra", "Foreground Service Example in Android FROM FRAGMENT")
        serviceIntent!!.action = "ACTION_START_FOREGROUND_SERVICE"
        ContextCompat.startForegroundService(requireContext(), serviceIntent!!)

        return root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun replayStatusCallback(message: String){

        requireActivity().runOnUiThread {
            //_binding?.animationPeriscope!!.cancelAnimation()
            _viewModel!!.updateDescription("Transmitting captured Signal")
            _binding!!.animationPeriscope!!.setAnimation(R.raw.sinus)
            _binding!!.animationPeriscope.playAnimation()
        }

        //_animationView!!.setAnimation(R.raw.sinus)
        //_animationView!!.playAnimation()
        // GIVE IT SOME TIME TO TRANSMIT THE SIGNAL
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            //setOperationMode(Constants.OPERATIONMODE_PERISCOPE)
            requireActivity().runOnUiThread {
                //_binding?.animationPeriscope!!.cancelAnimation()
                _viewModel!!.updateDescription("Looking for Signals")
                _binding!!.animationPeriscope!!.setAnimation(R.raw.radar)
                _binding!!.animationPeriscope.playAnimation()
            }
        }, 1500)
        //Thread.sleep(1_500)
        // GO BACK TO PERISCOPE MODE
        /*activity?.runOnUiThread {
            _binding!!.animationPeriscope.setAnimation(R.raw.radar)
            _binding!!.animationPeriscope.playAnimation()
        }*/
    }

    private fun setOperationModeCallback(message: String){
        Log.d(_logTag, "OP MODE CB: " + message)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setOperationMode(operationMode:String){
        val command = Constants.COMMAND_SET_OPERATION_MODE
        val commandId = Constants.COMMAND_ID_DUMMY

        val commandString = command + commandId + operationMode

        //Handler(Looper.getMainLooper()).post(Runnable {
            _bluetoothSerial!!.sendByteString(commandString + "\n", ::setOperationModeCallback)
        //})

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

            when (incomingCommand) {
                "0003" -> handleIncomingSignalTransfer(message)
                else -> { // Note the block
                    Log.d(_logTag, "Icoming Command not parseable")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun parseSignalEntityFromDataString(data:String, locationId:Int):SignalEntity{
            var configEndIndex = Constants.BLUETOOTH_COMMAND_HEADER_LENGTH + Constants.CC1101_ADAPTER_CONFIGURATION_LENGTH
            var cc1101ConfigString = data.substring(Constants.BLUETOOTH_COMMAND_HEADER_LENGTH, configEndIndex)
            var signalData = data.substring(configEndIndex)

            // CLEAR EMPTY FIRST SAMPLES:
            var samples = signalData.split(",").toMutableList()
            while(samples.last().toInt() <= 0){
                samples.removeLast()
            }

            // CLEAR EMPTY LAST SAMPLES:
            while(samples.first().toInt() <= 0){
                samples.removeFirst()
            }

            signalData = samples.joinToString(",")
            var samplesCount = signalData.split(',').size

            var signalName = ""
            var signalTag = ""
            var timestamp = LocalDateTime.now().atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            var frequency = cc1101ConfigString.substring(0,6).toFloat()
            var modulation = cc1101ConfigString[7].toString().toInt()
            var dRate = cc1101ConfigString.substring(8,11).toInt()
            var rxBw = cc1101ConfigString.substring(11,17).toFloat()
            var pktFormat = cc1101ConfigString[17].toString().toInt()
            var lqi = cc1101ConfigString.substring(18,24).toFloat()
            var rssi = cc1101ConfigString.substring(24,30).toFloat()

            if(signalName == ""){
                signalName = frequency.toInt().toString() + "_" + LocalDateTime.now().year + "-" + LocalDateTime.now().month + "-" + + LocalDateTime.now().dayOfMonth + "-"+ LocalDateTime.now().hour + ":" + LocalDateTime.now().minute + ":" + LocalDateTime.now().second
            }

            var signalEntity:SignalEntity = SignalEntity(0, signalName, signalTag, locationId, timestamp?.toInt(), "RAW", frequency, modulation,dRate,rxBw,pktFormat,signalData,samplesCount,lqi,rssi,false,false)
            return signalEntity
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleIncomingSignalTransfer(data:String){
        var configEndIndex = Constants.BLUETOOTH_COMMAND_HEADER_LENGTH + Constants.CC1101_ADAPTER_CONFIGURATION_LENGTH
        var cc1101ConfigString = data.substring(Constants.BLUETOOTH_COMMAND_HEADER_LENGTH, configEndIndex)
        var signalData = data.substring(configEndIndex)

        Log.d(_logTag, "Configstring: " + cc1101ConfigString)
        Log.d(_logTag, "Signaldata: " + signalData)

        val locationDao = AppDatabase.getDatabase(requireContext()).locationDao()
        val signalDao = AppDatabase.getDatabase(requireContext()).signalDao()
        CoroutineScope(Dispatchers.IO).launch {
            var locationId = 0
            // SAVE LOCATION ?!
            if(_lastLocation != null && _lastLocationDateTime != null){

                var locationEntity: LocationEntity = LocationEntity(0, _lastLocation!!.accuracy,_lastLocation!!.altitude,_lastLocation!!.latitude,_lastLocation!!.longitude,_lastLocation!!.speed)
                locationId = locationDao.insertItem(locationEntity).toInt()
                Log.d(_logTag, "Saved Location with ID: " + locationId)
            }

            var signalEntity = parseSignalEntityFromDataString(data, locationId)
            var signalId = signalDao.insertItem(signalEntity).toInt()
            Log.d(_logTag, "Saved Signal with ID: " + signalId)
        }


        // CLEAR EMPTY FIRST SAMPLES:
        var samples = signalData.split(",").toMutableList()
        while(samples.last().toInt() <= 0){
            samples.removeLast()
        }

        // CLEAR EMPTY LAST SAMPLES:
        while(samples.first().toInt() <= 0){
            samples.removeFirst()
        }

        signalData = samples.joinToString(",")

        var samplesCount = signalData.split(',').size
        _viewModel!!.capturedSignalInfo.postValue("Received " + samplesCount + " Samples")

        _lastIncomingSignalData = signalData
        _lastIncomingCc1101Config = cc1101ConfigString
        _viewModel!!.capturedSignalData.postValue(signalData)
        _capturedSignals++;
        _viewModel!!.infoTextFooter.postValue(_capturedSignals.toString() + " Signals captured");

        setOperationMode(Constants.OPERATIONMODE_IDLE)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
                _viewModel!!.connectionState.postValue("Disconnected")
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
                requireActivity().runOnUiThread {
                    //_binding?.animationPeriscope!!.cancelAnimation()
                    _binding!!.animationPeriscope!!.setAnimation(R.raw.dots)
                    _binding!!.animationPeriscope.playAnimation()
                }

                _viewModel!!.connectionState.postValue("Connecting...")
            }
            2 -> {
                Log.d(_logTag, "Connected")
                _viewModel!!.connectionState.postValue("Connected")
                // ACTIVATE PERISCOPE MODE
                setOperationMode(Constants.OPERATIONMODE_PERISCOPE)
                requireActivity().runOnUiThread {
                    //_binding?.animationPeriscope!!.cancelAnimation()
                    _binding!!.animationPeriscope!!.setAnimation(R.raw.radar)
                    _binding!!.animationPeriscope.playAnimation()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun receiveLocationChanges(location: Location) {
        //Log.d(_logTag, "Location updated")
        _lastLocation = location
        _lastLocationDateTime = LocalDateTime.now()

        var decimals = 4
        _viewModel!!.locationInfo.postValue(location.longitude.round(decimals).toString() + " | " + location.latitude.round(decimals).toString())
    }

    fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

}