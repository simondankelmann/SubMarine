package de.simon.dankelmann.submarine.ui.Home

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.airbnb.lottie.LottieAnimationView
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentHomeBinding
import de.simon.dankelmann.submarine.Services.BluetoothSerial

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var _logTag:String = "HomeFragment"
    private var _bluetoothSerial:BluetoothSerial? = null
    private var _viewModel:HomeViewModel? = null
    private var _lastIncomingSignalData = ""
    private var _lastIncomingCc1101Config = ""

    private var _commandHeaderLength = 8
    private var _cc1101AdapterConfigLength = 18

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)
        _viewModel = homeViewModel

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        var animation: LottieAnimationView = binding.animationHome
        animation.setOnClickListener { view ->
            // SHOW SCAN FRAGMENT
            requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_home_to_nav_scanbt)
        }

        return root

        /*
        //CONNECTION STATUS
        val connectionStatusTextView: TextView = binding.connectionLabel
        homeViewModel.connectionStatus.observe(viewLifecycleOwner) {
            connectionStatusTextView.text = it
        }

        //SIGNAL STATUS
        val singalStatusTextView: TextView = binding.signalLabel
        homeViewModel.signalStatus.observe(viewLifecycleOwner) {
            singalStatusTextView.text = it
        }

        //REPLAY STATUS
        val replayStatusTextView: TextView = binding.replayStatus
        homeViewModel.replayStatus.observe(viewLifecycleOwner) {
            replayStatusTextView.text = it
        }

        // SINGAL DATA
        val signalDataTextView: TextView = binding.signalData
        homeViewModel.signalData.observe(viewLifecycleOwner) {
            signalDataTextView.text = it
        }

        // CONNECT BUTTON
        val connectButton: Button = binding.connectButton
        connectButton.setOnClickListener { view ->
            resetUi()
            var macAddress = "C0:49:EF:D0:C4:B6"
            _bluetoothSerial = BluetoothSerial(requireContext(), ::connectionStateChangedCallback)
            _bluetoothSerial?.connect(macAddress, ::receivedMessageCallback)
        }

        // REPLAY BUTTON
        val replayButton: Button = binding.replayButton
        replayButton.setOnClickListener { view ->
            // REPLAY
            _viewModel!!.updateReplayStatusText("Transmitting Signal to Sub Marine...")
            if(_lastIncomingSignalData != ""){
                Log.d(_logTag, "ReTransmitting: " + _lastIncomingSignalData)

                val command = "0001"
                val commandId = "1234"

                val commandString = command + commandId + _lastIncomingSignalData

                _bluetoothSerial!!.sendByteString(commandString + "\n", ::replayStatusCallback)
            }
        }

        // PERISCOPE BUTTON
        val periscopeButton: Button = binding.periscopeButton
        periscopeButton.setOnClickListener { view ->
            val command = "0002"
            val commandId = "1234"

            val commandString = command + commandId + "0002"

            _bluetoothSerial!!.sendByteString(commandString + "\n", ::replayStatusCallback)
        }*/
    }

    private fun replayStatusCallback(message: String){
        _viewModel!!.updateReplayStatusText(message)
    }

    private fun resetUi(){
        _viewModel!!.updateConnectionStatusText("Connecting...")
        _viewModel!!.updateSignalStatusText("No Singal detected yet")
        _viewModel!!.updateSignalData("")
    }

    private fun receivedMessageCallback(message: String){
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
            /*
            _lastIncomingSignalData = message
            _viewModel!!.updateSignalData(message)

            var samplesCount = message.split(',').size
            _viewModel!!.updateSignalStatusText("Detected Signal with " + samplesCount + " Samples")
            */

        }
    }

    private fun handleIncomingSignalTransfer(data:String){
        var configEndIndex = _commandHeaderLength + _cc1101AdapterConfigLength
        var cc1101ConfigString = data.substring(_commandHeaderLength, configEndIndex)
        var signalData = data.substring(configEndIndex)

        Log.d(_logTag, "Configstring: " + cc1101ConfigString)
        Log.d(_logTag, "Signaldata: " + signalData)

        var samplesCount = signalData.split(',').size
        _viewModel!!.updateSignalStatusText("Received Signal with " + samplesCount + " Samples")

        _lastIncomingSignalData = signalData
        _lastIncomingCc1101Config = cc1101ConfigString
        _viewModel!!.updateSignalData(signalData)
    }

    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
                _viewModel!!.updateConnectionStatusText("Disconnected")
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
                _viewModel!!.updateConnectionStatusText("Connecting...")
            }
            2 -> {
                Log.d(_logTag, "Connected")
                _viewModel!!.updateConnectionStatusText("Connected")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}