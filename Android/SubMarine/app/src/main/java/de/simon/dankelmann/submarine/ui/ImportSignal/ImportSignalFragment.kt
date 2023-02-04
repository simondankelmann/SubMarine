package de.simon.dankelmann.submarine.ui.importSignal

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import de.simon.dankelmann.esp32_subghz.ui.importSignal.ImportSignalViewModel
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Entities.LocationEntity
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Interfaces.SubmarineResultListenerInterface
import de.simon.dankelmann.submarine.Models.SubmarineCommand
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.Services.SubMarineService
import de.simon.dankelmann.submarine.databinding.FragmentImportSignalBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneOffset


class ImportSignalFragment: Fragment(), SubmarineResultListenerInterface {
    private val _logTag = "ImportSignalFragment"
    private var _binding: FragmentImportSignalBinding? = null
    private var _viewModel: ImportSignalViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    //private var _bluetoothSerial: BluetoothSerial? = null

    private var _submarineService:SubMarineService = AppContext.submarineService

    private var _signalEntity:SignalEntity? = null
    private var _savedSignal:Boolean = false
    private var _isConnected:Boolean = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(ImportSignalViewModel::class.java)
        _viewModel = viewModel

        _binding = FragmentImportSignalBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // LETS GO !
        _submarineService.addResultListener(this)

        setupUi()

        _submarineService.connect()
        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111) {
            _viewModel!!.animationResourceId.postValue(R.raw.dots)

            val selectedFile = data?.data // The URI with the location of the file
            if(selectedFile.toString().endsWith(".sub")){
                // IMPORT FLIPPER ZERO SUB FILE

                val input: InputStream? = requireActivity().contentResolver.openInputStream(data!!.data!!)
                val lines = input!!.bufferedReader().use { it.readLines() }

                var samples = mutableListOf<Int>()

                var signalName = selectedFile!!.path!!.substring(selectedFile!!.path!!.lastIndexOf("/")+1);
                var signalTag = ""
                var timestamp = LocalDateTime.now().atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

                var frequency = 433.92f
                var modulation = 2
                var dRate = 512
                var rxBw = 256.0f
                var pktFormat = 3
                var lqi = 0.0f
                var rssi = 0.0f

                if(signalName == ""){
                    signalName = "IMPORT_" + frequency.toInt().toString() + "_" + LocalDateTime.now().year + "-" + LocalDateTime.now().month + "-" + + LocalDateTime.now().dayOfMonth + "-"+ LocalDateTime.now().hour + ":" + LocalDateTime.now().minute + ":" + LocalDateTime.now().second
                }

                if(lines != null){
                    lines.forEach{ it ->
                        var splitted = it.split(": ")
                        if(splitted.size == 2){
                            var flipperCmd = splitted[0]
                            var flipperVal = splitted[1]

                            when(flipperCmd){
                                "Frequency" -> {
                                    frequency = (flipperVal.toFloat() / 1000000)
                                    Log.d(_logTag, "Parsed Frequency: " + frequency)
                                }

                                "Preset" -> {
                                    when(flipperVal){
                                        "FuriHalSubGhzPresetOok650Async" -> {
                                            modulation = 2
                                        }
                                    }
                                }

                                "Protocol" -> {

                                }

                                "RAW_Data" -> {
                                    var parsedSamples = flipperVal.split(" ")
                                    parsedSamples.forEach{ it2 ->
                                        samples.add(it2.toInt())
                                    }
                                }

                            }
                            Log.d(_logTag, splitted[0])
                        }

                    }

                    var signalData = samples.joinToString(",")
                    var samplesCount = signalData.split(',').size
                    var signalEntity: SignalEntity = SignalEntity(0, signalName, signalTag, 0, timestamp?.toInt(), "RAW", frequency, modulation,dRate,rxBw,pktFormat,signalData,samplesCount,lqi,rssi,false,false)

                    _signalEntity = signalEntity

                    _viewModel!!.capturedSignalData.postValue(signalData)
                    _viewModel!!.capturedSignalInfo.postValue(signalName)
                }



                //_viewModel!!.capturedSignalData.postValue(inputAsString)

               /*
                var file = File(selectedFile!!.path)
                if(file.isFile && file.exists()){
                    Log.d(_logTag, "1")
                    var lines = file.bufferedReader().readLines()
                    Log.d(_logTag, lines.size.toString())
                    if(lines != null){
                        var contentString = ""
                        lines.forEach{
                            contentString += "-----" + it.toString()
                        }

                        _viewModel!!.capturedSignalData.postValue(contentString)
                    }
                }*/
            }


            _viewModel!!.animationResourceId.postValue(R.raw.success)
        }


    }

    fun setupUi(){
        // SET UP UI
        val animationView: LottieAnimationView = binding.animationImportSignal
        _viewModel!!.animationResourceId.observe(viewLifecycleOwner) {
            animationView.setAnimation(it)
            if(it == R.raw.success){
                animationView.repeatCount = 0
            } else {
                animationView.repeatCount = LottieDrawable.INFINITE
            }
            animationView.playAnimation()
        }

        animationView.setOnClickListener{view ->
            // OPEN A FILE
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
        }

        val description: TextView = binding.textViewRecordSignalDescription
        _viewModel!!.description.observe(viewLifecycleOwner) {
            description.text = it
        }

        val capturedSignalInfo: TextView = binding.textViewImportedSignalInfo
        _viewModel!!.capturedSignalInfo.observe(viewLifecycleOwner) {
            capturedSignalInfo.text = it
        }

        val capturedSignalData: TextView = binding.textViewCapturedSignalData
        _viewModel!!.capturedSignalData.observe(viewLifecycleOwner) {
            capturedSignalData.text = it
        }

        val editTextCapturedSignalName: EditText = binding.editTextCapturedSignalName
        _viewModel!!.capturedSignalName.observe(viewLifecycleOwner) {
            editTextCapturedSignalName.setText(it)
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

        // REPLAY BUTTON
        val replayButton: Button = binding.replayImportedSignalButton
        replayButton.setOnClickListener { view ->
            if(_signalEntity != null && _isConnected){
                replaySignalEntity(_signalEntity!!)
            }

            if(!_isConnected){
                _viewModel!!.animationResourceId.postValue(R.raw.warning)
            }
        }


        // SAVE BUTTON
        val saveButton: Button = binding.saveImportedSignalButton
        saveButton.setOnClickListener { view ->
            if(_signalEntity != null && _savedSignal == false){
                saveCurrentSignal()
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

    fun saveCurrentSignal(){
        if(_signalEntity != null){
            val signalDao = AppDatabase.getDatabase(requireContext()).signalDao()
            CoroutineScope(Dispatchers.IO).launch {

                var enteredName = _binding!!.editTextCapturedSignalName.text.toString()
                if(enteredName != "" && enteredName != _signalEntity!!.name){
                    _signalEntity!!.name = enteredName
                }


                var signalId = signalDao.insertItem(_signalEntity!!).toInt()
                Log.d(_logTag, "Saved Signal with ID: " + signalId)
                _viewModel!!.description.postValue("Signal saved successfully")
            }
        }
    }

    fun replaySignalEntity(signalEntity: SignalEntity){
        _viewModel!!.animationResourceId.postValue(R.raw.wave2)
        _viewModel!!.description.postValue("Transmitting Signal to Sub Marine...")
        /*
        val command = Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND
        val commandId = Constants.COMMAND_ID_DUMMY
        val commandString = command + commandId + _submarineService.getConfigurationStringFromSignalEntity(signalEntity) + signalEntity.signalData*/

        _submarineService.transmitSignal(signalEntity, 1, 0)

        //_submarineService.sendCommandToDevice(SubmarineCommand(Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND,Constants.COMMAND_ID_DUMMY,_submarineService.getConfigurationStringFromSignalEntity(signalEntity) + signalEntity.signalData))
        //_bluetoothSerial!!.sendByteString(commandString + "\n", ::replayStatusCallback)
    }

    private fun replayStatusCallback(message: String){
        requireActivity().runOnUiThread {
            _viewModel!!.description.postValue("Transmitting captured Signal")
            _viewModel!!.animationResourceId.postValue(R.raw.sinus)
        }

        // GIVE IT SOME TIME TO TRANSMIT THE SIGNAL
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            _viewModel!!.animationResourceId.postValue(R.raw.success)
        }, 1500)

    }

    private fun setOperationModeRecordSignalCallback(message: String){
        Log.d(_logTag, "OP MODE CB: " + message)
        _viewModel!!.animationResourceId.postValue(R.raw.record_blue)
        _viewModel!!.description.postValue("Recording Signal")
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
                /*
                "0003" -> handleIncomingSignalTransfer(message)
                else -> { // Note the block
                    Log.d(_logTag, "Icoming Command not parseable")
                }*/
            }
        }
    }



    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
                _viewModel!!.footerText3.postValue("Disconnected")
                _isConnected = false
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
                //_viewModel!!.animationResourceId.postValue(R.raw.bluetooth_scan)
                _viewModel!!.footerText3.postValue("Connecting...")
                _isConnected = false
            }
            2 -> {
                Log.d(_logTag, "Connected")
                _viewModel!!.footerText3.postValue("Connected")
                // ACTIVATE PERISCOPE MODE
                _isConnected = true
                //setOperationMode(Constants.OPERATIONMODE_RECORD_SIGNAL,::setOperationModeRecordSignalCallback)
                //Log.d(_logTag, "Setting Operation Mode")
                //_submarineService.setOperationMode(Constants.OPERATIONMODE_RECORD_SIGNAL, "")
                //_viewModel!!.animationResourceId.postValue(R.raw.dots)
            }
        }
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
        setOperationModeRecordSignalCallback(command.getCommandString())
    }

    override fun onSignalReplayed(timeElapsed: Int, command: SubmarineCommand) {
        replayStatusCallback(command.getCommandString())
    }
}