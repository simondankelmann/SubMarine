package de.simon.dankelmann.submarine.ui.ScanBt

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import de.simon.dankelmann.esp32_subghz.Adapters.BluetoothDeviceListviewAdapter
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.PermissionCheck.PermissionCheck
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.Services.BluetoothService
import de.simon.dankelmann.submarine.databinding.FragmentScanbtBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


class ScanBtFragment : Fragment(), AdapterView.OnItemClickListener {

    private var _binding: FragmentScanbtBinding? = null
    private val _logTag = "ScanBtFragment"
    private lateinit var _viewModel: ScanBtViewModel
    private var _listItemTimer: Timer? = null
    private var _listItemAdapter: BluetoothDeviceListviewAdapter? = null
    private val _lifeTimeFoundDeviceListItem = 12 * 1000 // X * Seconds
    private var _bluetoothService: BluetoothService? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val scanBtViewModel = ViewModelProvider(this).get(ScanBtViewModel::class.java)
        _viewModel = scanBtViewModel

        _binding = FragmentScanbtBinding.inflate(inflater, container, false)
        val root: View = binding.root

        var test = AppContext.submarineService

        /*
        val textView: TextView = binding.textScanBt
        scanBtViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }*/

        // SETUP LISTVIEW ADAPTER
        val listview: ListView = binding.bluetoothDeviceList
        listview.onItemClickListener = this
        _viewModel.bluetoothDevices.observe(viewLifecycleOwner) {
            _listItemAdapter = BluetoothDeviceListviewAdapter(root.context, it) //ArrayAdapter(root.context, android.R.layout.simple_list_item_1, it)
            listview.adapter = _listItemAdapter
        }

        // REGISTER RECEIVER FOR FOUND DEVICES
        var mReceiver = FoundDeviceBroadCastReceiver()
        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireContext().registerReceiver(mReceiver, intentFilter)

        // START SCANNING:
        _bluetoothService = BluetoothService(requireContext())
        _bluetoothService?.startDiscovery()

        //startListItemRemovalTimer()

        return root
    }

    fun startListItemRemovalTimer(){
        _listItemTimer = Timer()
        _listItemTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if(_listItemAdapter != null){
                    var itemCount = _listItemAdapter?.count
                    if(itemCount != null){
                        for (itemIndex in 1..itemCount) {
                            var bluetoothDeviceModel = _listItemAdapter?.getBluetoothDeviceModel(itemIndex - 1)
                            if(bluetoothDeviceModel != null){
                                val currentMillis = System.currentTimeMillis()
                                var deviceLastSeen = bluetoothDeviceModel?.lastSeen
                                if(deviceLastSeen != null){
                                    val timeDifference = currentMillis - deviceLastSeen!!
                                    if(timeDifference >= _lifeTimeFoundDeviceListItem){
                                        _listItemAdapter?.removeBluetoothDeviceListItem(itemIndex - 1)
                                        // NOTIFY UI ABOUT CHANGES
                                        requireActivity()!!.runOnUiThread(java.lang.Runnable {
                                            _listItemAdapter!!.notifyDataSetChanged()
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },0,1000)
    }

    fun stopListItemRemovalTimer(){
        if(_listItemTimer != null){
            _listItemTimer!!.cancel()
        }
    }

    inner class FoundDeviceBroadCastReceiver: BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if(ctx != null && intent != null) {
                var action = intent.action
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    var foundDevice: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (foundDevice != null) {
                        if (PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                            // HANDLE THE FOUND DEVICE
                            val rssi: Short = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                            Log.d(_logTag, foundDevice?.name + " - " + foundDevice?.address + " - " + rssi)
                            _viewModel.addFoundBluetoothDevice(
                                foundDevice,
                                rssi.toInt(),
                                System.currentTimeMillis()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        _bluetoothService?.stopDiscovery()
        stopListItemRemovalTimer()

        var selectedDevice = _viewModel.getBluetoothDeviceModel(p2)

        CoroutineScope(Dispatchers.IO).launch {
            AppContext.submarineService.setBluetoothDevice(selectedDevice!!.device!!)
            AppContext.submarineService.connect()
        }

        requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_scanbt_to_nav_connected_device)
    }
}