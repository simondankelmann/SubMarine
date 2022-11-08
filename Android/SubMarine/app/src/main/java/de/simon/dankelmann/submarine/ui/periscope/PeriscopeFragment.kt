package de.simon.dankelmann.submarine.ui.periscope

import android.Manifest
import android.bluetooth.BluetoothDevice
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import de.simon.dankelmann.esp32_subghz.ui.connectedDevice.PeriscopeViewModel
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentPeriscopeBinding
import de.simon.dankelmann.submarine.permissioncheck.PermissionCheck
import de.simon.dankelmann.submarine.services.BluetoothSerial


class PeriscopeFragment: Fragment() {
    private val _logTag = "PeriscopeFragment"
    private var _binding: FragmentPeriscopeBinding? = null
    private var _viewModel: PeriscopeViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    private var _bluetoothSerial: BluetoothSerial? = null

    private var _capturedSignals = 0
    private var _lastIncomingSignalData = ""
    private var _lastIncomingCc1101Config = ""

    private var _animationView:LottieAnimationView? = null

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

        // REPLAY BUTTON
        val replayButton: Button = binding.replaySignalButton
        replayButton.setOnClickListener { view ->
            // REPLAY
            _viewModel!!.updateDescription("Transmitting Signal to Sub Marine...")
            if(_lastIncomingSignalData != ""){

                //_binding!!.animationPeriscope.setAnimation(R.raw.loadcircle)
                //_binding!!.animationPeriscope.playAnimation()
                _viewModel!!.updateDescription("Transmitting Signal to Sub Marine Device")

                Log.d(_logTag, "ReTransmitting: " + _lastIncomingSignalData)

                val command = Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND
                val commandId = Constants.COMMAND_ID_DUMMY
                val commandString = command + commandId + _lastIncomingCc1101Config + _lastIncomingSignalData
/*
                Thread(Runnable {
                    _bluetoothSerial!!.sendByteString(commandString + "\n", ::replayStatusCallback)
                }).start()*/

                Handler(Looper.getMainLooper()).post(Runnable {
                    _bluetoothSerial!!.sendByteString(commandString + "\n", ::replayStatusCallback)
                })

            }
        }

        // ANIMATION VIEW
        _animationView = binding.animationPeriscope

        return root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun replayStatusCallback(message: String){
        activity?.runOnUiThread {
            _viewModel!!.updateDescription("Executing Signal")
        }


        //_animationView!!.setAnimation(R.raw.sinus)
        //_animationView!!.playAnimation()
        // GIVE IT SOME TIME TO TRANSMIT THE SIGNAL
        Thread.sleep(1_500)
        // GO BACK TO PERISCOPE MODE
        /*activity?.runOnUiThread {
            _binding!!.animationPeriscope.setAnimation(R.raw.radar)
            _binding!!.animationPeriscope.playAnimation()
        }*/
        setOperationMode(Constants.OPERATIONMODE_PERISCOPE)
        activity?.runOnUiThread {
            _viewModel!!.updateDescription("Looking for Signals")
        }

    }

    private fun setOperationModeCallback(message: String){
        Log.d(_logTag, "OP MODE CB: " + message)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setOperationMode(operationMode:String){
        /*
        Thread(Runnable {

        }).start()*/

        val command = Constants.COMMAND_SET_OPERATION_MODE
        val commandId = Constants.COMMAND_ID_DUMMY

        val commandString = command + commandId + operationMode
        /*
        Handler(Looper.getMainLooper()).post(Runnable {

        })*/

        Handler(Looper.getMainLooper()).post(Runnable {
            _bluetoothSerial!!.sendByteString(commandString + "\n", ::setOperationModeCallback)
        })


        /*
        Thread(Runnable {

        }).start()*/
    }

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

        Log.d(_logTag, "Configstring: " + cc1101ConfigString)
        Log.d(_logTag, "Signaldata: " + signalData)

        // CLEAR EMPTY LAST SAMPLES:

        var samples = signalData.split(",").toMutableList()
        while(samples.last().toInt() <= 0){
            samples.removeLast()
        }

        signalData = samples.joinToString(",")



        var samplesCount = signalData.split(',').size
        _viewModel!!.capturedSignalInfo.postValue("Received " + samplesCount + " Samples")

        _lastIncomingSignalData = signalData
        _lastIncomingCc1101Config = cc1101ConfigString
        _viewModel!!.capturedSignalData.postValue(signalData)
        _capturedSignals++;
        _viewModel!!.infoTextFooter.postValue(_capturedSignals.toString() + " Signals captured");
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
                _viewModel!!.connectionState.postValue("Connecting...")
            }
            2 -> {
                Log.d(_logTag, "Connected")
                _viewModel!!.connectionState.postValue("Connected")
                // ACTIVATE PERISCOPE MODE
                setOperationMode(Constants.OPERATIONMODE_PERISCOPE)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}