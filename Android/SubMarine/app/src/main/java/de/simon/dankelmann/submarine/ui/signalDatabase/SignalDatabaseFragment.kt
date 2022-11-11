package de.simon.dankelmann.submarine.ui.SignalDatabase

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
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import de.simon.dankelmann.esp32_subghz.Adapters.BluetoothDeviceListviewAdapter
import de.simon.dankelmann.esp32_subghz.Adapters.SignalDatabaseListviewAdapter
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.permissioncheck.PermissionCheck
import de.simon.dankelmann.submarine.databinding.FragmentSignalDatabaseBinding
import de.simon.dankelmann.submarine.services.BluetoothSerial
import de.simon.dankelmann.submarine.services.SubMarineService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignalDatabaseFragment: Fragment(), AdapterView.OnItemClickListener {
    private val _logTag = "SignalDatabaseFragment"
    private var _binding: FragmentSignalDatabaseBinding? = null
    private var _viewModel: SignalDatabaseViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    //private var _bluetoothSerial: BluetoothSerial? = null
    private var _submarineService: SubMarineService = AppContext.submarineService
    private var _listItemAdapter: SignalDatabaseListviewAdapter? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(SignalDatabaseViewModel::class.java)
        _viewModel = viewModel

        _binding = FragmentSignalDatabaseBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupUi()


        // GET DATA FROM BUNDLE
        var deviceFromBundle = arguments?.getParcelable("Device") as BluetoothDevice?
        if(deviceFromBundle != null){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
                _bluetoothDevice = deviceFromBundle

                // LETS GO !
                /*
                _bluetoothSerial = BluetoothSerial(requireContext(), ::connectionStateChangedCallback)
                Thread(Runnable {
                    _bluetoothSerial?.connect(deviceFromBundle.address, ::receivedDataCallback)
                }).start()*/


            }
        }

        return root
    }

    fun setupUi(){
        val descriptionText: TextView = binding.textViewSignalDatabaseDescription
        _viewModel!!.signalDatabaseDescription.observe(viewLifecycleOwner) {
            descriptionText.text = it
        }

        val footerText: TextView = binding.textViewSignalDatabaseFooter
        _viewModel!!.signalDatabaseFooterText.observe(viewLifecycleOwner) {
            footerText.text = it
        }

        // SETUP LISTVIEW ADAPTER
        val listview: ListView = binding.signalDatabaseListView
        listview.onItemClickListener = this
        _viewModel?.signalEntities?.observe(viewLifecycleOwner) {
            _listItemAdapter = SignalDatabaseListviewAdapter(requireContext(), it) //ArrayAdapter(root.context, android.R.layout.simple_list_item_1, it)
            listview.adapter = _listItemAdapter
        }

        // LOAD DATA FROM DB
        val signalDao = AppDatabase.getDatabase(requireContext()).signalDao()
        CoroutineScope(Dispatchers.IO).launch {
            _viewModel!!.signalDatabaseDescription.postValue("Loading items from Signal Database")
            val data = signalDao.getAll()
            _viewModel!!.signalDatabaseDescription.postValue(data.size.toString() + " Signal(s) in Database")

            _viewModel!!.signalEntities.postValue(data.toMutableList())
        }
    }

    fun registerSubmarineCallbacks(){
        _submarineService.clearCallbacks()
        /*_submarineService.registerCallback(::connectionStateChangedCallback, SubMarineService.CallbackType.BluetoothConnectionStateChanged)
        _submarineService.registerCallback(::receivedDataCallback, SubMarineService.CallbackType.IcomingData)
        _submarineService.registerCallback(::replayStatusCallback, SubMarineService.CallbackType.ReplaySignal)
        _submarineService.registerCallback(::setOperationModeCallback, SubMarineService.CallbackType.SetOperationMode)*/
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


    @RequiresApi(Build.VERSION_CODES.M)
    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
                _viewModel!!.signalDatabaseFooterText.postValue("Disconnected")
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
                _viewModel!!.signalDatabaseFooterText.postValue("Connecting...")
            }
            2 -> {
                Log.d(_logTag, "Connected")
                _viewModel!!.signalDatabaseFooterText.postValue("Connected")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        var selectedSignalEntity = _viewModel!!.getSignalEntity(position)
        /*
        var selectedSignalEntity = _viewModel!!.getSignalEntity(position)
        if(selectedSignalEntity != null){
            replaySignalEntity(selectedSignalEntity!!)
        }*/


        val bundle = Bundle()
        bundle.putInt("SignalEntityId", selectedSignalEntity!!.uid)
        requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_signal_database_to_nav_signalDetail, bundle)
    }

}