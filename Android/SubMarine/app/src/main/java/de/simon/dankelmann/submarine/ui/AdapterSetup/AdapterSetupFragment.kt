package de.simon.dankelmann.submarine.ui.AdapterSetup

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
import de.simon.dankelmann.submarine.databinding.FragmentAdapterSetupBinding
import de.simon.dankelmann.submarine.permissioncheck.PermissionCheck
import de.simon.dankelmann.submarine.services.BluetoothSerial
import de.simon.dankelmann.submarine.services.SubMarineService
import de.simon.dankelmann.submarine.ui.AdapterSetup.AdapterSetupViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdapterSetupFragment : Fragment() {

    private var _binding: FragmentAdapterSetupBinding? = null
    private val _logTag = "AdapterSetupFragment"
    private var _viewModel: AdapterSetupViewModel? = null
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
            ViewModelProvider(this).get(AdapterSetupViewModel::class.java)
        _viewModel = viewModel


        _binding = FragmentAdapterSetupBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // CALL SETUP UI AFTER _viewModel and _binding are set up
        setupUi()

        var deviceFromBundle = arguments?.getParcelable("Device") as BluetoothDevice?
        if(deviceFromBundle != null){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
                _bluetoothDevice = deviceFromBundle

                // LETS GO !
                _submarineService.clearCallbacks()
                _submarineService.registerCallback(::connectionStateChangedCallback, SubMarineService.CallbackType.BluetoothConnectionStateChanged)
                _submarineService.registerCallback(::commandSentCallback, SubMarineService.CallbackType.CommandSent)
                _submarineService.registerCallback(::receivedDataCallback, SubMarineService.CallbackType.IcomingData)
                _submarineService.deviceAddress = deviceFromBundle.address

                _viewModel!!.animationResourceId.postValue(R.raw.bluetooth_scan)
                _submarineService.connect()
            }
        }

        return root
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
                Constants.COMMAND_SET_ADAPTER_CONFIGURATION -> handleIncomingAdapterConfiguration(message)
                else -> { // Note the block
                    Log.d(_logTag, "Icoming Command not parseable")
                }
            }
        }
    }

    fun handleIncomingAdapterConfiguration(data:String){
        var configEndIndex = Constants.BLUETOOTH_COMMAND_HEADER_LENGTH + Constants.CC1101_ADAPTER_CONFIGURATION_LENGTH
        var cc1101ConfigString = data.substring(Constants.BLUETOOTH_COMMAND_HEADER_LENGTH, configEndIndex)

        var configuration = CC1101Configuration()
        configuration.loadFromString(cc1101ConfigString)
        _viewModel!!.cC1101Configuration.postValue(configuration)
        _viewModel!!.animationResourceId.postValue(R.raw.configuration)
    }

    fun commandSentCallback(data:String){
        Log.d(_logTag, "commandSentCallback")
        _viewModel!!.animationResourceId.postValue(R.raw.success)
    }

    fun setupUi(){
        // SETUP UI
        val animationView: LottieAnimationView = binding.animationAdapterSetup
        _viewModel!!.animationResourceId.observe(viewLifecycleOwner) {
            animationView.setAnimation(it)
            animationView.playAnimation()

            if(it == R.raw.success){
                animationView.repeatCount = 0
            } else {
                animationView.repeatCount = LottieDrawable.INFINITE
            }
        }

        val titleTextView: TextView = binding.textViewAdapterSetupTitle
        _viewModel!!.title.observe(viewLifecycleOwner) {
            titleTextView.text = it
        }

        val descriptionTextView: TextView = binding.textViewAdapterSetupDescription
        _viewModel!!.description.observe(viewLifecycleOwner) {
            descriptionTextView.text = it
        }

        val footerTextView1: TextView = binding.textviewFooter1
        _viewModel!!.footerText1.observe(viewLifecycleOwner) {
            footerTextView1.text = it
        }

        val editTextFrequency = binding.editTextViewMhz
        val editTextModulation = binding.editTextViewModulation
        val editTextDrate = binding.editTextViewDrate
        val editTextRxBw = binding.editTextViewRxBw
        val editTextPktFormat = binding.editTextViewPktFormat
        _viewModel!!.cC1101Configuration.observe(viewLifecycleOwner) {
            editTextFrequency.setText(it.mhz.toString())
            editTextModulation.setText(it.modulation.toString())
            editTextDrate.setText(it.dRate.toString())
            editTextRxBw.setText(it.rxBw.toString())
            editTextPktFormat.setText(it.pktFormat.toString())
        }

        // SAVE BUTTON
        val saveButton = binding.saveConfigurationButton
        saveButton.setOnClickListener{
            writeConfigrationToAdapter()
        }

        // LOAD BUTTON
        val loadButton = binding.loadConfigurationButton
        loadButton.setOnClickListener{
            getConfigrationFromAdapter()
        }
    }

    fun getConfigrationFromAdapter(){
        _viewModel!!.animationResourceId.postValue(R.raw.wave2)
        _submarineService.sendCommandToDevice(Constants.COMMAND_GET_ADAPTER_CONFIGURATION, Constants.COMMAND_ID_DUMMY, "")
    }

    fun writeConfigrationToAdapter(){
        var enteredConfiguration = getCC1101ConfigurationFromUi()
        _viewModel!!.animationResourceId.postValue(R.raw.wave2)
        _submarineService.sendCommandToDevice(Constants.COMMAND_SET_ADAPTER_CONFIGURATION, Constants.COMMAND_ID_DUMMY, enteredConfiguration.getConfigurationString())
    }

    fun getCC1101ConfigurationFromUi():CC1101Configuration{
        var configuration = CC1101Configuration()

        val editTextFrequency = binding.editTextViewMhz
        val editTextModulation = binding.editTextViewModulation
        val editTextDrate = binding.editTextViewDrate
        val editTextRxBw = binding.editTextViewRxBw
        val editTextPktFormat = binding.editTextViewPktFormat

        configuration.mhz = editTextFrequency.text.toString().toFloat()
        configuration.modulation = editTextModulation.text.toString().toInt()
        configuration.dRate = editTextDrate.text.toString().toInt()
        configuration.rxBw = editTextRxBw.text.toString().toFloat()
        configuration.pktFormat = editTextPktFormat.text.toString().toInt()

        Log.d(_logTag, "ConfigString from UI:")

        return configuration
    }

    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
                _viewModel!!.footerText1.postValue("Disconnected")
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
                _viewModel!!.footerText1.postValue("Connecting...")
            }
            2 -> {
                Log.d(_logTag, "Connected")
                _viewModel!!.footerText1.postValue("Connected")
                getConfigrationFromAdapter()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}