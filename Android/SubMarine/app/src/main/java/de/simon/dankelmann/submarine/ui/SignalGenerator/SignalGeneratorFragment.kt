package de.simon.dankelmann.submarine.ui.SignalGenerator

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import de.simon.dankelmann.submarine.Adapters.DetectedSignalTabCollectionAdapter
import de.simon.dankelmann.submarine.Adapters.SignalGeneratorTabCollectionAdapter
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Interfaces.SubmarineResultListenerInterface
import de.simon.dankelmann.submarine.Models.CC1101Configuration
import de.simon.dankelmann.submarine.Models.SignalGeneratorDataModel
import de.simon.dankelmann.submarine.Models.SubmarineCommand
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentSignalGeneratorBinding
import de.simon.dankelmann.submarine.Services.SubMarineService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sign

class SignalGeneratorFragment : Fragment(), SubmarineResultListenerInterface {

    private var _binding: FragmentSignalGeneratorBinding? = null
    private val _logTag = "SignalGeneratorFragment"
    private var _viewModel: SignalGeneratorViewModel? = null
    private var _submarineService:SubMarineService = AppContext.submarineService
    private lateinit var _collectionAdapter: SignalGeneratorTabCollectionAdapter


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel =
            ViewModelProvider(this).get(SignalGeneratorViewModel::class.java)
        _viewModel = viewModel
        _binding = FragmentSignalGeneratorBinding.inflate(inflater, container, false)
        val root: View = binding.root

        var signalEntityId = arguments?.getInt("SignalEntityId") as Int
        if(signalEntityId != null){
            val signalDao = AppDatabase.getDatabase(requireContext()).signalDao()
            CoroutineScope(Dispatchers.IO).launch {
                val signalEntity = signalDao.getById(signalEntityId)
                if(signalEntity != null){
                    _viewModel!!.signalEntity.postValue(signalEntity)
                    setupTabs()
                }
            }
        }

        // CALL SETUP UI AFTER _viewModel and _binding are set up
        setupUi()

        _submarineService.addResultListener(this)
        _submarineService.connect()

        return root
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
        setupTabs()
        super.onResume()
    }


    private fun receivedDataCallback(message: String, outgoingCommand:SubmarineCommand?){
        if(message != ""){
            Log.d(_logTag, "Received: " + message)

            // PARSE COMMAND AND DATA
            var incomingCommand = message.substring(0,4)
            var incomingCommandId = message.substring(4,8)

            Log.d(_logTag, "Icoming Command: " + incomingCommand)
            Log.d(_logTag, "Icoming Command Id: " + incomingCommandId)
        }
    }



    fun commandSentCallback(command:SubmarineCommand){
        Log.d(_logTag, "commandSentCallback")
    }

    fun setupUi(){
        // SETUP UI
        val titleTextView: TextView = binding.textViewSignalGeneratorTitle
        _viewModel!!.title.observe(viewLifecycleOwner) {
            titleTextView.text = it
        }

        val descriptionTextView: TextView = binding.textViewSignalGeneratorDescription
        _viewModel!!.description.observe(viewLifecycleOwner) {
            descriptionTextView.text = it
        }

        val samplesPerSymbolTextView: TextView = binding.textviewSamplePerSymbol
        _viewModel!!.samplesPerSymbol.observe(viewLifecycleOwner) {
            samplesPerSymbolTextView.text = it.toString() + " Samples / Symbol"
        }

        //SEEKBAR
        val seekbarSamplesPerSymbol = binding.seekbarSamplesPerSymbol
        seekbarSamplesPerSymbol.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                _viewModel!!.samplesPerSymbol.postValue(seek.progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // NOT IN USE
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // POST THE NEW VALUE TO THE TAB FRAGMENTS
                var signalGeneratorDataModel = SignalGeneratorDataModel()
                signalGeneratorDataModel.signalEntity =  _viewModel!!.signalEntity.value
                signalGeneratorDataModel.samplesPerSymbol = seekBar!!.progress
                _collectionAdapter.updateSignalGeneratorDataModel(signalGeneratorDataModel)

            }
        });

        // TRANSMIT BUTTON
        val transmitGeneratedSignalButton = binding.transmitGeneratedSignalButton
        transmitGeneratedSignalButton.setOnClickListener{
            var signalEntity = _collectionAdapter.getGeneratedSignalEntity()

            Log.d(_logTag, "Got signalEntity: " + signalEntity.signalDataLength)

            _submarineService.sendCommandToDevice(SubmarineCommand(Constants.COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND,Constants.COMMAND_ID_DUMMY,_submarineService.getConfigurationStringFromSignalEntity(signalEntity) + signalEntity.signalData))
        }

        val footerTextView1: TextView = binding.textviewFooter1
        _viewModel!!.footerText1.observe(viewLifecycleOwner) {
            footerTextView1.text = it
        }

        val footerTextView2: TextView = binding.textviewFooter2
        _viewModel!!.footerText2.observe(viewLifecycleOwner) {
            footerTextView2.text = it
        }
        val footerTextView3: TextView = binding.textviewFooter3
        _viewModel!!.footerText3.observe(viewLifecycleOwner) {
            footerTextView3.text = it
        }
    }

    fun setupTabs(){
        requireActivity().runOnUiThread{
            // TAB LAYOUT
            var signalGeneratorDataModel = SignalGeneratorDataModel()
            signalGeneratorDataModel.signalEntity =  _viewModel!!.signalEntity.value
            signalGeneratorDataModel.samplesPerSymbol = _viewModel!!.samplesPerSymbol.value!!

            _collectionAdapter = SignalGeneratorTabCollectionAdapter(this, signalGeneratorDataModel)
            val tabLayout: TabLayout = binding.tabLayoutSignalGenerator
            var viewPager: ViewPager2 =  binding.viewPagerSignalGenerator
            viewPager.adapter = _collectionAdapter

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                when(position){
                    0 -> {
                        tab.text = "Timings"
                    }

                    1 -> {
                        tab.text = "Binary"
                    }

                    2 -> {
                        tab.text = "Hex"
                    }

                    3 -> {
                        tab.text = "Config"
                    }
                }
            }.attach()
        }
    }


    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                Log.d(_logTag, "Disconnected")
                _viewModel!!.footerText3.postValue("Disconnected")
            }
            1 -> {
                Log.d(_logTag, "Connecting...")
                _viewModel!!.footerText3.postValue("Connecting...")
            }
            2 -> {
                Log.d(_logTag, "Connected")
                _viewModel!!.footerText3.postValue("Connected")
            }
        }
    }

    override fun onConnectionStateChanged(connectionState: Int) {
        connectionStateChangedCallback(connectionState)
    }

    override fun onIncomingData(data: String, command: SubmarineCommand?) {
        Log.d(_logTag, "Data comes in: " + data)
        receivedDataCallback(data, command)
    }

    override fun onOutgoingData(timeElapsed: Int, command: SubmarineCommand?) {
        // NOT IN USE
    }

    override fun onCommandSent(timeElapsed: Int, command: SubmarineCommand) {
        commandSentCallback(command)
    }

    override fun onOperationModeSet(timeElapsed: Int, command: SubmarineCommand) {
        // NOT IN USE
    }

    override fun onSignalReplayed(timeElapsed: Int, command: SubmarineCommand) {
        // NOT IN USE
    }

}