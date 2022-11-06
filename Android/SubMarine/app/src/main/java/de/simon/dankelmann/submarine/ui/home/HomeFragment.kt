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

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        var macAddress = "C0:49:EF:D0:C4:B6"

        _bluetoothSerial = BluetoothSerial(requireContext(), ::connectionStateChangedCallback)
        _bluetoothSerial?.connect(macAddress, ::receivedMessageCallback)

        // REPLAY BUTTON
        val replayButton: Button = binding.replayButton
        replayButton.setOnClickListener { view ->
            // REPLAY
            if(_lastIcomongSignalString != ""){
                Log.d(_logTag, "ReTransmitting: " + _lastIcomongSignalString)
                _bluetoothSerial!!.sendByteString(_lastIcomongSignalString + "\n")
            }
        }

        return root
    }


    private fun receivedMessageCallback(message: String){
        if(message != ""){
            Log.d(_logTag, "Received: " + message)
            _lastIcomongSignalString = message
            _viewModel!!.updateText(message)
        }

    }

    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
            }
            2 -> {
                Log.d(_logTag, "Connected")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}