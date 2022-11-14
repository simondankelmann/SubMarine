package de.simon.dankelmann.submarine.ui.ConnectedDevice

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.airbnb.lottie.LottieAnimationView
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Interfaces.SubmarineResultListenerInterface
import de.simon.dankelmann.submarine.Models.SubmarineCommand
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.PermissionCheck.PermissionCheck
import de.simon.dankelmann.submarine.databinding.FragmentConnectedDeviceBinding
import de.simon.dankelmann.submarine.Services.SubMarineService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConnectedDeviceFragment: Fragment(), SubmarineResultListenerInterface {
    private val _logTag = "ConnectedDeviceFragment"
    private var _binding: FragmentConnectedDeviceBinding? = null
    private var _viewModel: ConnectedDeviceViewModel? = null
    private var _submarineService: SubMarineService = AppContext.submarineService

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(ConnectedDeviceViewModel::class.java)
        _viewModel = viewModel

        _binding = FragmentConnectedDeviceBinding.inflate(inflater, container, false)
        val root: View = binding.root

        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
            var bluetoothDevice = _submarineService.getBluetoothDevice()
            if(bluetoothDevice != null){
                _viewModel!!.updateTitle(bluetoothDevice!!.name);
                _viewModel!!.updateDescription(bluetoothDevice!!.address);
            }
        }

        _submarineService.addResultListener(this)
        _submarineService.connect()

        setupUi()

        return root
    }

    fun setupUi(){
        // TITLE AND DESCRIPTION
        val titleTextView: TextView = binding.textViewConnectedDeviceTitle
        _viewModel!!.title.observe(viewLifecycleOwner) {
            titleTextView.text = it
        }

        val descriptionTextView: TextView = binding.textViewConnectedDeviceInfo
        _viewModel!!.description.observe(viewLifecycleOwner) {
            descriptionTextView.text = it
        }

        val databaseInfoText: TextView = binding.textViewDatabaseInfo
        _viewModel!!.dbInfoText.observe(viewLifecycleOwner) {
            databaseInfoText.text = it
        }

        val periscopeButton: Button = binding.periscopeButton
        periscopeButton.setOnClickListener { view ->
            requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_connected_device_to_nav_periscope)
        }

        // SIGNAL DB BUTTON
        val signalDbButton: Button = binding.signalDatabaseButton
        signalDbButton.setOnClickListener { view ->
            requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_connected_device_to_nav_signal_database)
        }

        // RECORD SIGNALBUTTON
        val recordSignalButton: Button = binding.recordsignalButton
        recordSignalButton.setOnClickListener { view ->
            requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_connected_device_to_nav_record_signal)
        }

        // DETECT SIGNALBUTTON
        val detectSignalButton: Button = binding.detectSignalButton
        detectSignalButton.setOnClickListener { view ->
            requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_connected_device_to_nav_detect_signal)
        }

        // ADAPTER SETUP BUTTON
        val adapterSetupButton: Button = binding.adapterSetupButton
        adapterSetupButton.setOnClickListener { view ->
            requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_connected_device_to_nav_adapter_setup)
        }

        // SIGNAL MAP BUTTON
        val signalMapButton: Button = binding.signalMapButton
        signalMapButton.setOnClickListener { view ->
            requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_connected_device_to_nav_signal_map)
        }

        // DB COUNTER
        var db = AppDatabase.getDatabase(requireContext());
        val signalDao = db.signalDao()
        CoroutineScope(Dispatchers.IO).launch {
            val dataSize = signalDao.getAll().size
            _viewModel!!.dbInfoText.postValue(dataSize.toString() + " Signal(s) in Database")

            //Log.d(_logTag, "DB PATH: " + AppDatabase.getDatabase(requireContext()).openHelper.writableDatabase.path)
            //AppDatabase.exportToSdCard();
        }

        // ANIMATION
        val animationView: LottieAnimationView = binding.animationConnectedDevice
        _viewModel!!.animationResourceId.observe(viewLifecycleOwner) {
            animationView.setAnimation(it)
            animationView.playAnimation()
        }

        animationView.setOnClickListener{
            _submarineService.connect()
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
        Log.d(_logTag, "Connection State: " + connectionState)
        when(connectionState){
            SubMarineService.ConnectionStates.Connected.value-> {
                _viewModel!!.animationResourceId.postValue(R.raw.submarine)
            }

            SubMarineService.ConnectionStates.Connecting.value-> {
                _viewModel!!.animationResourceId.postValue(R.raw.connecting)
            }

            SubMarineService.ConnectionStates.Disconnected.value-> {
                _viewModel!!.animationResourceId.postValue(R.raw.disconnected_button)
            }
        }
    }

    override fun onIncomingData(data: String, command: SubmarineCommand?) {
        // NOT IN USE
    }

    override fun onOutgoingData(timeElapsed: Int, command: SubmarineCommand?) {
        // NOT IN USE
    }

    override fun onCommandSent(timeElapsed: Int, command: SubmarineCommand) {
        // NOT IN USE
    }

    override fun onOperationModeSet(timeElapsed: Int, command: SubmarineCommand) {
        // NOT IN USE
    }

    override fun onSignalReplayed(timeElapsed: Int, command: SubmarineCommand) {
        // NOT IN USE
    }

}