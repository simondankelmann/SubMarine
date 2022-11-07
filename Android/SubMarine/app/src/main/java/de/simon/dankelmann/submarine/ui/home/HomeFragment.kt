package de.simon.dankelmann.submarine.ui.home

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import de.simon.dankelmann.submarine.databinding.FragmentHomeBinding
import de.simon.dankelmann.submarine.services.BluetoothSerial
import org.json.JSONObject

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var _logTag:String = "HomeFragment"
    private var _bluetoothSerial:BluetoothSerial? = null
    private var _viewModel:HomeViewModel? = null
    private var _lastIcomongSignalString = ""

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
            if(_lastIcomongSignalString != ""){
                Log.d(_logTag, "ReTransmitting: " + _lastIcomongSignalString)

                val command = "0001"
                val commandId = "1234"

                val commandString = command + commandId + _lastIcomongSignalString

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
        }

        return root
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

            _lastIcomongSignalString = message
            _viewModel!!.updateSignalData(message)

            var samplesCount = message.split(',').size
            _viewModel!!.updateSignalStatusText("Detected Signal with " + samplesCount + " Samples")
        }

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