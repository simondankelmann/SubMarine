package de.simon.dankelmann.submarine.ui.ViewSignalEntity

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
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Interfaces.SubmarineResultListenerInterface
import de.simon.dankelmann.submarine.Models.SubmarineCommand
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentViewSignalEntityBinding
import de.simon.dankelmann.submarine.Services.SubMarineService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ViewSignalEntityFragment : Fragment(), SubmarineResultListenerInterface{

    private var _binding: FragmentViewSignalEntityBinding? = null
    private val _logTag = "SignalEntityFragment"
    private var _viewModel: ViewSignalEntityViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    //private var _bluetoothSerial: BluetoothSerial? = null
    private var _signalEntity: SignalEntity? = null
    private var _isConnected:Boolean = false
    private var _submarineService: SubMarineService = AppContext.submarineService
    private var _signalEntityId = 0

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("LongLogTag")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel =
            ViewModelProvider(this).get(ViewSignalEntityViewModel::class.java)
        _viewModel = viewModel

        _binding = FragmentViewSignalEntityBinding.inflate(inflater, container, false)
        val root: View = binding.root

        var signalEntityId = arguments?.getInt("SignalEntityId") as Int
        if(signalEntityId != null){
            _signalEntityId = signalEntityId
            setupUi()

            _submarineService.addResultListener(this)
            _submarineService.connect()
        }
        return root
    }

    fun setupUi(){
        // SETUP UI
        _viewModel!!.animationResourceId.postValue(R.raw.inspect_signal)

        val titleText: TextView = binding.textViewSignalDetailTitle
        _viewModel!!.signalDetailTitle.observe(viewLifecycleOwner) {
            titleText.text = it
        }

        val descriptionText: TextView = binding.textViewSignalDetailDescription
        _viewModel!!.signalDetailDescription.observe(viewLifecycleOwner) {
            descriptionText.text = it
        }

        val frequencyText: TextView = binding.textViewSignalFrequency
        _viewModel!!.signalDetailFrequency.observe(viewLifecycleOwner) {
            frequencyText.text = it
        }

        val dataText: TextView = binding.textViewSignalDetailData
        _viewModel!!.signalDetailData.observe(viewLifecycleOwner) {
            dataText.text = it
        }

        val footerText1: TextView = binding.textViewFooter1
        _viewModel!!.footerText1.observe(viewLifecycleOwner) {
            footerText1.text = it
        }

        val footerText2: TextView = binding.textViewFooter2
        _viewModel!!.footerText2.observe(viewLifecycleOwner) {
            footerText2.text = it
        }

        val footerText3: TextView = binding.textViewFooter3
        _viewModel!!.footerText3.observe(viewLifecycleOwner) {
            footerText3.text = it
        }

        val animationView: LottieAnimationView = binding.animationSignalDetail
        _viewModel!!.animationResourceId.observe(viewLifecycleOwner) {
            animationView.setAnimation(it)
            animationView.playAnimation()
        }

        // REPLAY
        val replayButton: Button = binding.replaySignalDetailButton
        replayButton.setOnClickListener { view ->
            if(_signalEntity != null && _isConnected){
                replaySignalEntity(_signalEntity!!)
            }

            if(!_isConnected){
                _viewModel!!.animationResourceId.postValue(R.raw.warning)
            }
        }

        // LOAD DATA FROM DB
        val signalDao = AppDatabase.getDatabase(requireContext()).signalDao()
        CoroutineScope(Dispatchers.IO).launch {

            val signalEntity = signalDao.getById(_signalEntityId)
            if(signalEntity != null){
                _signalEntity = signalEntity!!

                Log.d(_logTag, "SIGNAL: " + _signalEntity?.name)

                //SETUP UI
                requireActivity().runOnUiThread{
                    _viewModel!!.signalDetailTitle.postValue(_signalEntity?.name)
                    _viewModel!!.signalDetailDescription.postValue(_signalEntity!!.signalDataLength.toString() + " Samples")
                    _viewModel!!.signalDetailFrequency.postValue(_signalEntity?.frequency.toString() + " Mhz")
                    _viewModel!!.signalDetailData.postValue(_signalEntity?.signalData.toString())

                    _viewModel!!.footerText1.postValue(getModulationString(_signalEntity!!.modulation!!) + " | " + _signalEntity!!.type!!)
                    _viewModel!!.footerText2.postValue("RX-BW: " + _signalEntity!!.rxBw.toString()+" Khz")
                }
            }

        }
    }


    fun getModulationString(modulation:Int):String{
        when(modulation){
            0 -> return "2-FSK"
            1 -> return "GFSK"
            2 -> return "ASK/OOK"
            3 -> return "4-FSK"
            4 -> return "MSK"
        }
        return "Unknown"
    }

    fun getConfigurationStringFromSignalEntity(signalEntity: SignalEntity):String{

        var mhz = signalEntity.frequency.toString()
        while(mhz.length < 6){
            mhz = "0$mhz"
        }

        var tx = "1"
        var modulation = signalEntity.modulation.toString()

        var dRate = signalEntity.dRate.toString()
        while(dRate.length < 3){
            dRate = "0$dRate"
        }

        var rxBw = signalEntity.rxBw.toString()
        while(rxBw.length < 6){
            rxBw = "0$rxBw"
        }

        var pktFormat = signalEntity.pktFormat.toString()

        var lqi = signalEntity.lqi.toString()
        while(lqi.length < 6){
            lqi = "0$lqi"
        }

        var rssi = signalEntity.rssi.toString()
        while(rssi.length < 6){
            rssi = "0$rssi"
        }

        /*
        Adapter Configuration Structure:
        Bytes:
        0-5 => MHZ
        6 => TX
        7 => MODULATION
        8-10 => DRATE
        11-16 => RX_BW
        17 => PKT_FORMAT
        18-23 => AVG_LQI
        24-29 => AVG_RSSI
        */

        return mhz + tx + modulation + dRate + rxBw + pktFormat + lqi + rssi
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun replaySignalEntity(signalEntity: SignalEntity){
        _viewModel!!.animationResourceId.postValue(R.raw.wave2)
        _viewModel!!.signalDetailDescription.postValue("Transmitting Signal to Sub Marine...")
        _submarineService.sendCommandToDevice(SubmarineCommand(Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND,Constants.COMMAND_ID_DUMMY, getConfigurationStringFromSignalEntity(signalEntity) + signalEntity.signalData))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun receivedDataCallback(message: String){
        if(message != ""){
            Log.d(_logTag, "Received: " + message)

            // PARSE COMMAND AND DATA
            var incomingCommand = message.substring(0,4)
            var incomingCommandId = message.substring(4,8)

            Log.d(_logTag, "Icoming Command: " + incomingCommand)
            Log.d(_logTag, "Icoming Command Id: " + incomingCommandId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun replayStatusCallback(message: String){
        _viewModel!!.signalDetailDescription.postValue("Executing Signal now")
        _viewModel!!.animationResourceId.postValue(R.raw.sinus)

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            setOperationMode(Constants.OPERATIONMODE_IDLE)
            requireActivity().runOnUiThread {
                _viewModel!!.signalDetailDescription.postValue(_signalEntity!!.signalDataLength.toString() + " Samples")
                _viewModel!!.animationResourceId.postValue(R.raw.inspect_signal)
            }
        }, 1500)

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setOperationMode(operationMode:String){
        val command = Constants.COMMAND_SET_OPERATION_MODE
        val commandId = Constants.COMMAND_ID_DUMMY
        val commandString = command + commandId + operationMode

        _submarineService.setOperationMode(operationMode)
        //_bluetoothSerial!!.sendByteString(commandString + "\n", ::setOperationModeCallback)
    }

    private fun setOperationModeCallback(message: String){
        Log.d(_logTag, "OP MODE CB: " + message)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
                _isConnected = false
                _viewModel!!.footerText3.postValue("Disconnected")
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
                _isConnected = false
                _viewModel!!.footerText3.postValue("Connecting...")
            }
            2 -> {
                Log.d(_logTag, "Connected")
                _isConnected = true
                _viewModel!!.footerText3.postValue("Connected")
                _viewModel!!.animationResourceId.postValue(R.raw.inspect_signal)
            }
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
        connectionStateChangedCallback(connectionState)
    }

    override fun onIncomingData(data: String, command: SubmarineCommand?) {
        receivedDataCallback(data)
    }

    override fun onOutgoingData(timeElapsed: Int, command: SubmarineCommand?) {
        // NOT IN USE
    }

    override fun onCommandSent(timeElapsed: Int, command: SubmarineCommand) {
        // NOT IN USE
    }

    override fun onOperationModeSet(timeElapsed: Int, command: SubmarineCommand) {
        setOperationModeCallback(command.getCommandString())
    }

    override fun onSignalReplayed(timeElapsed: Int, command: SubmarineCommand) {
        replayStatusCallback(command.getCommandString())
    }
}