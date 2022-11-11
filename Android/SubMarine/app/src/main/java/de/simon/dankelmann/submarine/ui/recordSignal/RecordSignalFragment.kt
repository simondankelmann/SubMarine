package de.simon.dankelmann.submarine.ui.recordSignal

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import de.simon.dankelmann.esp32_subghz.ui.recordSignal.RecordSignalViewModel
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Entities.LocationEntity
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Interfaces.LocationResultListener
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentPeriscopeBinding
import de.simon.dankelmann.submarine.databinding.FragmentRecordSignalBinding
import de.simon.dankelmann.submarine.permissioncheck.PermissionCheck
import de.simon.dankelmann.submarine.services.BluetoothSerial
import de.simon.dankelmann.submarine.services.ForegroundService
import de.simon.dankelmann.submarine.services.LocationService
import de.simon.dankelmann.submarine.services.SubMarineService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.math.round
import kotlin.reflect.KFunction1


class RecordSignalFragment: Fragment(), LocationResultListener {
    private val _logTag = "RecordSignalFragment"
    private var _binding: FragmentRecordSignalBinding? = null
    private var _viewModel: RecordSignalViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    //private var _bluetoothSerial: BluetoothSerial? = null

    private var _capturedSignals = 0
    private var _lastIncomingSignalData = ""
    private var _lastIncomingCc1101Config = ""

    private var _locationService:LocationService? = null

    private var _lastLocation:Location? = null
    private var _lastLocationDateTime:LocalDateTime? = null
    private var _submarineService:SubMarineService = AppContext.submarineService

    private var _signalEntity:SignalEntity? = null
    private var _savedSignal:Boolean = false
    private var _signalLocation:Location? = null
    private var _signalRecordDate:LocalDateTime? = null
    private var _isConnected:Boolean = false

    // Notification Service Intent
    var serviceIntent: Intent? = null


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(RecordSignalViewModel::class.java)
        _viewModel = viewModel

        // Start Foreground Service to scan Locations in Background
        serviceIntent = Intent(requireContext(), ForegroundService::class.java)
        serviceIntent!!.putExtra("inputExtra", "Foreground Service Example in Android FROM FRAGMENT")
        serviceIntent!!.action = "ACTION_START_FOREGROUND_SERVICE"
        ContextCompat.startForegroundService(requireContext(), serviceIntent!!)

        _binding = FragmentRecordSignalBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // REQUEST LOCATION UPDATES
        _locationService = LocationService(requireContext(), this)

        setupUi()

        // LETS GO !
        _submarineService.clearCallbacks()
        registerSubmarineCallbacks()

        _submarineService.connect()

        return root
    }

    fun setupUi(){
        // SET UP UI
        val animationView: LottieAnimationView = binding.animationRecordSignal
        _viewModel!!.animationResourceId.observe(viewLifecycleOwner) {
            animationView.setAnimation(it)
            if(it == R.raw.success){
                animationView.repeatCount = 0
            } else {
                animationView.repeatCount = LottieDrawable.INFINITE
            }
            animationView.playAnimation()
        }

        animationView.setOnClickListener{view ->
            if(_viewModel!!.animationResourceId.value == R.raw.success){
                //setOperationMode(Constants.OPERATIONMODE_RECORD_SIGNAL, ::setOperationModeRecordSignalCallback)
                _submarineService.setOperationMode(Constants.OPERATIONMODE_RECORD_SIGNAL)
                _savedSignal = false
                _viewModel!!.capturedSignalName.postValue("")
                _viewModel!!.capturedSignalData.postValue("")
                _viewModel!!.capturedSignalInfo.postValue("No Signal recorded yet")
            }
        }

        val description: TextView = binding.textViewRecordSignalDescription
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

        val editTextCapturedSignalName: EditText = binding.editTextCapturedSignalName
        _viewModel!!.capturedSignalName.observe(viewLifecycleOwner) {
            editTextCapturedSignalName.setText(it)
        }

        val footerText1: TextView = binding.textViewFooter1
        _viewModel!!.footerText1.observe(viewLifecycleOwner) {
            footerText1.text = it
        }

        val footerText2: TextView = binding.textViewFooter2
        _viewModel!!.footerText2.observe(viewLifecycleOwner) {
            footerText2.text = it
        }

        val footerText3: TextView = binding.textViewFooter3
        _viewModel!!.footerText3.observe(viewLifecycleOwner) {
            footerText3.text = it
        }

        // REPLAY BUTTON
        val replayButton: Button = binding.replaySignalButton
        replayButton.setOnClickListener { view ->
            if(_signalEntity != null && _isConnected){
                replaySignalEntity(_signalEntity!!)
            }

            if(!_isConnected){
                _viewModel!!.animationResourceId.postValue(R.raw.warning)
            }
        }


        // SAVE BUTTON
        val saveButton: Button = binding.saveignalButton
        saveButton.setOnClickListener { view ->
            if(_signalEntity != null && _savedSignal == false){
                saveCurrentSignal()
            }
        }
    }

    fun registerSubmarineCallbacks(){
        _submarineService.clearCallbacks()
        _submarineService.registerCallback(::connectionStateChangedCallback, SubMarineService.CallbackType.BluetoothConnectionStateChanged)
        _submarineService.registerCallback(::receivedDataCallback, SubMarineService.CallbackType.IcomingData)
        _submarineService.registerCallback(::setOperationModeRecordSignalCallback, SubMarineService.CallbackType.SetOperationMode)
        _submarineService.registerCallback(::replayStatusCallback, SubMarineService.CallbackType.ReplaySignal)
    }
    override fun onDestroyView() {
        _submarineService.clearCallbacks()
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        _submarineService.clearCallbacks()
        super.onPause()
    }

    override fun onResume() {
        registerSubmarineCallbacks()
        super.onResume()
    }

    fun saveCurrentSignal(){
        _viewModel!!.description.postValue("Saving the Signal")

        val locationDao = AppDatabase.getDatabase(requireContext()).locationDao()
        val signalDao = AppDatabase.getDatabase(requireContext()).signalDao()
        CoroutineScope(Dispatchers.IO).launch {
            var locationId = 0
            // SAVE LOCATION ?!
            if(_signalLocation != null && _lastLocationDateTime != null){
                var locationEntity: LocationEntity = LocationEntity(0, _lastLocation!!.accuracy,_lastLocation!!.altitude,_lastLocation!!.latitude,_lastLocation!!.longitude,_lastLocation!!.speed)
                locationId = locationDao.insertItem(locationEntity).toInt()
                Log.d(_logTag, "Saved Location with ID: " + locationId)
            }

            _signalEntity!!.locationId = locationId

            var enteredName = _binding!!.editTextCapturedSignalName.text.toString()
            if(enteredName != "" && enteredName != _signalEntity!!.name){
                _signalEntity!!.name = enteredName
            }


            var signalId = signalDao.insertItem(_signalEntity!!).toInt()
            Log.d(_logTag, "Saved Signal with ID: " + signalId)
            _viewModel!!.description.postValue("Signal saved successfully")
        }



    }

    fun replaySignalEntity(signalEntity: SignalEntity){
        _viewModel!!.animationResourceId.postValue(R.raw.wave2)
        _viewModel!!.description.postValue("Transmitting Signal to Sub Marine...")
        /*
        val command = Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND
        val commandId = Constants.COMMAND_ID_DUMMY
        val commandString = command + commandId + _submarineService.getConfigurationStringFromSignalEntity(signalEntity) + signalEntity.signalData*/

        _submarineService.sendCommandToDevice(Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND, Constants.COMMAND_ID_DUMMY, _submarineService.getConfigurationStringFromSignalEntity(signalEntity) + signalEntity.signalData)
        //_bluetoothSerial!!.sendByteString(commandString + "\n", ::replayStatusCallback)
    }

    private fun replayStatusCallback(message: String){
        requireActivity().runOnUiThread {
            _viewModel!!.description.postValue("Transmitting captured Signal")
            _viewModel!!.animationResourceId.postValue(R.raw.sinus)
        }

        // GIVE IT SOME TIME TO TRANSMIT THE SIGNAL
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            _viewModel!!.animationResourceId.postValue(R.raw.success)
        }, 1500)

    }

    private fun setOperationModeRecordSignalCallback(message: String){
        Log.d(_logTag, "OP MODE CB: " + message)
        _viewModel!!.animationResourceId.postValue(R.raw.record_blue)
        _viewModel!!.description.postValue("Recording Signal")
    }

    /*
    private fun setOperationMode(operationMode:String, statusCallback: KFunction1<String, Unit>){
        val command = Constants.COMMAND_SET_OPERATION_MODE
        val commandId = Constants.COMMAND_ID_DUMMY

        val commandString = command + commandId + operationMode

        if(operationMode == Constants.OPERATIONMODE_RECORD_SIGNAL){
            _savedSignal = false
        }

        _viewModel!!.description.postValue("Setting Operation Mode: " + operationMode)
        _bluetoothSerial!!.sendByteString(commandString + "\n", statusCallback)
    }*/

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

    private fun handleIncomingSignalTransfer(data:String){
        var configEndIndex = Constants.BLUETOOTH_COMMAND_HEADER_LENGTH + Constants.CC1101_ADAPTER_CONFIGURATION_LENGTH
        var cc1101ConfigString = data.substring(Constants.BLUETOOTH_COMMAND_HEADER_LENGTH, configEndIndex)
        var signalData = data.substring(configEndIndex)

        _viewModel!!.animationResourceId.postValue(R.raw.success)

        Log.d(_logTag, "Configstring: " + cc1101ConfigString)
        Log.d(_logTag, "Signaldata: " + signalData)

        var signalEntity = _submarineService.parseSignalEntityFromDataString(data, 0)
        if(signalEntity != null){
            _signalEntity = signalEntity
            _signalLocation = _lastLocation
            _signalRecordDate = LocalDateTime.now()
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
        _viewModel!!.capturedSignalData.postValue(signalData)

        _lastIncomingSignalData = signalData
        _lastIncomingCc1101Config = cc1101ConfigString
        _viewModel!!.capturedSignalData.postValue(signalData)
        _viewModel!!.capturedSignalName.postValue(_signalEntity!!.name)
        _capturedSignals++;
        //_viewModel!!.infoTextFooter.postValue(_capturedSignals.toString() + " Signals captured");
    }

    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
                _viewModel!!.footerText3.postValue("Disconnected")
                _isConnected = false
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
                _viewModel!!.animationResourceId.postValue(R.raw.bluetooth_scan)
                _viewModel!!.footerText3.postValue("Connecting...")
                _isConnected = false
            }
            2 -> {
                Log.d(_logTag, "Connected")
                _viewModel!!.footerText3.postValue("Connected")
                // ACTIVATE PERISCOPE MODE
                _isConnected = true
                //setOperationMode(Constants.OPERATIONMODE_RECORD_SIGNAL,::setOperationModeRecordSignalCallback)
                _submarineService.setOperationMode(Constants.OPERATIONMODE_RECORD_SIGNAL)
                _viewModel!!.animationResourceId.postValue(R.raw.dots)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun receiveLocationChanges(location: Location) {
        //Log.d(_logTag, "Location updated")
        _lastLocation = location
        _lastLocationDateTime = LocalDateTime.now()

        var decimals = 4
        _viewModel!!.footerText2.postValue(location.longitude.round(decimals).toString() + " | " + location.latitude.round(decimals).toString())
    }

    fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }
}