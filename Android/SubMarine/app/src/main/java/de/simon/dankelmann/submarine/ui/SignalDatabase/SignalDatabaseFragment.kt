package de.simon.dankelmann.submarine.ui.SignalDatabase

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import de.simon.dankelmann.esp32_subghz.Adapters.SignalDatabaseListviewAdapter
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.databinding.FragmentSignalDatabaseBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignalDatabaseFragment: Fragment(), AdapterView.OnItemClickListener {
    private val _logTag = "SignalDatabaseFragment"
    private var _binding: FragmentSignalDatabaseBinding? = null
    private var _viewModel: SignalDatabaseViewModel? = null
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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