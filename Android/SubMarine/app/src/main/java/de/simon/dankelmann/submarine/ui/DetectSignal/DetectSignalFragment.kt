package de.simon.dankelmann.submarine.ui.DetectSignal

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import de.simon.dankelmann.submarine.ui.DetectSignal.DetectSignalViewModel
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Entities.LocationEntity
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Interfaces.LocationResultListener
import de.simon.dankelmann.submarine.Interfaces.SubmarineResultListenerInterface
import de.simon.dankelmann.submarine.Models.SubmarineCommand
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentDetectSignalBinding
import de.simon.dankelmann.submarine.Services.ForegroundService
import de.simon.dankelmann.submarine.Services.LocationService
import de.simon.dankelmann.submarine.Services.SubMarineService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.round


class DetectSignalFragment: Fragment(), SubmarineResultListenerInterface {
    private val _logTag = "DetectSignalFragment"
    private var _binding: FragmentDetectSignalBinding? = null
    private var _viewModel: DetectSignalViewModel? = null
    private var _submarineService:SubMarineService = AppContext.submarineService
    private var _signalDetectDate:LocalDateTime? = null
    private var _minRssi = "-065"
    private var _detectedSignals = 0



    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(DetectSignalViewModel::class.java)
        _viewModel = viewModel

        _binding = FragmentDetectSignalBinding.inflate(inflater, container, false)
        val root: View = binding.root


        _submarineService.addResultListener(this)
        _submarineService.connect()

        setupUi()

        return root
    }

    fun setupUi(){
        // SET UP UI
        val animationView: LottieAnimationView = binding.animationDetectSignal
        _viewModel!!.animationResourceId.observe(viewLifecycleOwner) {
            animationView.setAnimation(it)
            if(it == R.raw.success){
                animationView.repeatCount = 0
            } else {
                animationView.repeatCount = LottieDrawable.INFINITE
            }
            animationView.playAnimation()
        }

        animationView.setOnClickListener{
            _submarineService.setOperationMode(Constants.OPERATIONMODE_DETECT_SIGNAL, _minRssi)
        }

        //SEEKBAR
        val minRssiSeekbar = binding.detectSignalMinRssiSeekbar
        val minRssiLabel = binding.textViewMinRssiLabel
        minRssiSeekbar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                _minRssi = progress.toString()
                while(_minRssi.length < 3){
                    _minRssi = "0"+ _minRssi
                }
                _minRssi = "-" + _minRssi
                minRssiLabel.text = "Min. RSSI: "+(progress * -1).toString() + " dBm"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // NOT IN USE
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                _submarineService.setOperationMode(Constants.OPERATIONMODE_DETECT_SIGNAL, _minRssi)
            }
        });

            val title: TextView = binding.textViewDetectSignalTitle
        _viewModel!!.title.observe(viewLifecycleOwner) {
            title.text = it
        }

        val description: TextView = binding.textViewDetectSignalDescription
        _viewModel!!.description.observe(viewLifecycleOwner) {
            description.text = it
        }

        val detectedFrequency: TextView = binding.textViewDetectedFrequency
        _viewModel!!.detectedFrequency.observe(viewLifecycleOwner) {
            detectedFrequency.text = it
        }

        var _continousDetectionCheckbox = binding.continuousDetectionCheckbox
        _continousDetectionCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            _viewModel!!.continuosDetection.postValue(isChecked)
        }

        val detectedRssi: TextView = binding.textViewDetectedRssi
        _viewModel!!.detectedRssi.observe(viewLifecycleOwner) {
            detectedRssi.text = it
        }

        val logView: TextView = binding.textViewLog
        _viewModel!!.log.observe(viewLifecycleOwner) {
            logView.text = it
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

    private fun setOperationModeCallback(message: String){
        Log.d(_logTag, "OP MODE CB: " + message)
        _viewModel!!.description.postValue("Detecting Signals")
        _viewModel!!.animationResourceId.postValue(R.raw.searching)
    }

    private fun receivedDataCallback(data: String, command: SubmarineCommand?){
        if(data != ""){
            Log.d(_logTag, "Received: " + data)

            // PARSE COMMAND AND DATA
            var incomingCommand = data.substring(0,4)
            var incomingCommandId = data.substring(4,8)

            Log.d(_logTag, "Icoming Command: " + incomingCommand)
            Log.d(_logTag, "Icoming Command Id: " + incomingCommandId)

            when (incomingCommand) {
                Constants.COMMAND_DETECTED_FREQUENCY -> {
                    handleIncomingDetectedFrequency(data, command)
                }
                else -> { // Note the block
                    Log.d(_logTag, "Icoming Command not parseable")
                }
            }
        }
    }

    private fun handleIncomingDetectedFrequency(data: String, command: SubmarineCommand?){
        Log.d(_logTag, "Handling incoming Frequency")
        _viewModel!!.animationResourceId.postValue(R.raw.success)

        var cmd:SubmarineCommand
        if(command == null){
            cmd = SubmarineCommand.parseFromDataString(data)!!
        } else {
            cmd = command
        }

        var detectedFrequency = 0.0f
        var detectedRssi = 0
        if(cmd._data.length >= 6){
            detectedFrequency = cmd._data.substring(0,6).toFloat()
            _viewModel!!.detectedFrequency.postValue(detectedFrequency.toString() + " Mhz")
        }

        if(cmd._data.length >= 6 + 4){
            detectedRssi = cmd._data.substring(6).toInt()
            _viewModel!!.detectedRssi.postValue(detectedRssi.toString() + " dBm")
        }

        var timestamp = LocalDateTime.now().toString()


        val previousLog = _viewModel!!.log.value
        _viewModel!!.log.postValue(timestamp + " | " + detectedFrequency.toString() + " Mhz" + " | " + detectedRssi.toString() + " dBm" +"\n" + previousLog)

        _detectedSignals++
        _viewModel!!.footerText1.postValue(_detectedSignals.toString() + " Signals detected")


        if(_viewModel!!.continuosDetection.value == true){
            _submarineService.setOperationMode(Constants.OPERATIONMODE_DETECT_SIGNAL, _minRssi)
        }

    }


    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
                _viewModel!!.footerText3.postValue("Disconnected")
                _viewModel!!.animationResourceId.postValue(R.raw.disconnected_button)
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
                _viewModel!!.footerText3.postValue("Connecting...")
                _viewModel!!.animationResourceId.postValue(R.raw.connecting)
            }
            2 -> {
                Log.d(_logTag, "Connected")
                _viewModel!!.footerText3.postValue("Connected")
                // ACTIVATE OPERATIONMODE_DETECT_SIGNAL
                _submarineService.setOperationMode(Constants.OPERATIONMODE_DETECT_SIGNAL, _minRssi)
                _viewModel!!.animationResourceId.postValue(R.raw.wave2)
            }
        }
    }

    override fun onConnectionStateChanged(connectionState: Int) {
        connectionStateChangedCallback(connectionState)
    }

    override fun onIncomingData(data: String, command: SubmarineCommand?) {
        receivedDataCallback(data, command)
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
        // NOT IN USE
    }
}