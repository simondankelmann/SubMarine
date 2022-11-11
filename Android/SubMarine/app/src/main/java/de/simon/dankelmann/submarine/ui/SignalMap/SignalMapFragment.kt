package de.simon.dankelmann.submarine.ui.SignalMap

import android.Manifest
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
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Models.CC1101Configuration
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentSignalMapBinding
import de.simon.dankelmann.submarine.permissioncheck.PermissionCheck
import de.simon.dankelmann.submarine.services.BluetoothSerial
import de.simon.dankelmann.submarine.services.SubMarineService
import de.simon.dankelmann.submarine.ui.SignalMap.SignalMapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignalMapFragment : Fragment() {

    private var _binding: FragmentSignalMapBinding? = null
    private val _logTag = "SignalMapFragment"
    private var _viewModel: SignalMapViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    private var _submarineService:SubMarineService = AppContext.submarineService

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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

        var deviceFromBundle = arguments?.getParcelable("Device") as BluetoothDevice?
        if(deviceFromBundle != null){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
                _bluetoothDevice = deviceFromBundle

                // LETS GO !
                /*
                _submarineService.clearCallbacks()
                _submarineService.registerCallback(::connectionStateChangedCallback, SubMarineService.CallbackType.BluetoothConnectionStateChanged)
                _submarineService.registerCallback(::commandSentCallback, SubMarineService.CallbackType.CommandSent)
                _submarineService.registerCallback(::receivedDataCallback, SubMarineService.CallbackType.IcomingData)
                _submarineService.deviceAddress = deviceFromBundle.address
                _viewModel!!.animationResourceId.postValue(R.raw.bluetooth_scan)
                _submarineService.connect()
                */
            }
        }

        return root
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}