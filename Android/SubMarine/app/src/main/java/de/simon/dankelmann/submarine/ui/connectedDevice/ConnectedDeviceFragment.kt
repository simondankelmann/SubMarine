package de.simon.dankelmann.submarine.ui.connectedDevice

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.permissioncheck.PermissionCheck
import de.simon.dankelmann.submarine.databinding.FragmentConnectedDeviceBinding
import de.simon.dankelmann.submarine.services.SubMarineService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConnectedDeviceFragment: Fragment() {
    private val _logTag = "ConnectedDeviceFragment"
    private var _binding: FragmentConnectedDeviceBinding? = null
    private var _viewModel: ConnectedDeviceViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    private var _submarineService: SubMarineService = AppContext.submarineService
    //private var _bluetoothSerial: BluetoothSerial? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.M)
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
        val signalDao = AppDatabase.getDatabase(requireContext()).signalDao()
        CoroutineScope(Dispatchers.IO).launch {
            val dataSize = signalDao.getAll().size
            _viewModel!!.dbInfoText.postValue(dataSize.toString() + " Signal(s) in Database")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}