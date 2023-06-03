package de.simon.dankelmann.submarine

import android.app.AlertDialog
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import androidx.annotation.RequiresApi
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.PermissionCheck.PermissionCheck
import de.simon.dankelmann.submarine.Services.SignalAnalyzer
import de.simon.dankelmann.submarine.SubGhzDecoders.SubGhzDecoderRegistry
import de.simon.dankelmann.submarine.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.Manifest
import kotlin.math.sign

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var appContext:AppContext
    private var _logTag = "MainActivity"

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // INITIALIZE THE APP CONTEXT
        AppContext.setContext(this)
        AppContext.setActivity(this)

        // REQUIRE ALL PERMISSIONS
       this.requestAllPermissions()

        AppContext.initializeSubmarineService()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        /*
        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }*/
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        /*
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )*/

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_scanbt, R.id.nav_periscope, R.id.nav_signal_database, R.id.nav_record_signal, R.id.nav_adapter_setup, R.id.nav_signal_map
            ), drawerLayout
        )

        // CREATE DB BACKUP
        //AppDatabase.exportToFile()

        // TEST CONVERSION
        //var signalAnalyzer = SignalAnalyzer()
        //var binString = signalAnalyzer.convertHexStringToBinaryString("0xf421084210843d087bd0f7bdef42108")
        //Log.d(_logTag, "RECEIVED BIN STRING: " + binString)
        //var timingsList = signalAnalyzer.convertHexStringToTimingsList("0xf421084210843d087bd0f7bdef42108", 300)

        // TEST DECODER
        /*
        var decoderRegistry: SubGhzDecoderRegistry = SubGhzDecoderRegistry()
        val signalDao = AppDatabase.getDatabase(applicationContext).signalDao()
        CoroutineScope(Dispatchers.IO).launch {
            val data = signalDao.search("SOME SIGNAL NAME")
            Log.d("TAG__", "Signal found: " + data.size.toString())
            if(data.size > 0){
                var signalEntity = data[0]
                var validDecoders = decoderRegistry.validateSignal(signalEntity)
                Log.d("TAG__", "Signal valid for: " + validDecoders.size.toString() + " Protocols")
                validDecoders.forEach {
                    Log.d("TAG__","Decoded Data: " + it.getInfoText())
                }
            }
        }*/






        // FOREGROUND SERVICE
        /*
        var serviceIntent = Intent(applicationContext.applicationContext, ForegroundService::class.java)
        serviceIntent!!.putExtra("inputExtra", "Foreground Service Example in Android FROM FRAGMENT")
        serviceIntent!!.action = "ACTION_START_FOREGROUND_SERVICE"
        ContextCompat.startForegroundService(applicationContext.applicationContext, serviceIntent!!)
        */

        /* DB TEST
        val locationDao = AppDatabase.getDatabase(applicationContext).locationDao()
        CoroutineScope(Dispatchers.IO).launch {
            val data = locationDao.getAll()
            Log.d("TAG__", "Locations found: " + data.size.toString())
            var le:LocationEntity = LocationEntity(0, 1.0f,1.0,1.0,1.0,1.0f)
            var newId = locationDao.insertItem(le)
            Log.d("TAG__", "ID: " + newId)
        }
        */

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    fun requestAllPermissions(){

        val allPermissions = arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.BLUETOOTH_CONNECT,

            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,

            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE,

            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,

            android.Manifest.permission.FOREGROUND_SERVICE
        )

        PermissionCheck.requireAllPermissions(this, allPermissions)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        PermissionCheck.processPermissionsResult(requestCode, permissions, grantResults)

        /*
        when (requestCode) {

            RECORD_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(_logTag, "Permission has been denied by user")
                } else {
                    Log.i(_logTag, "Permission has been granted by user")
                }
            }
        }*/
    }
}