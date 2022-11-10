package de.simon.dankelmann.submarine

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.Database.AppDatabase
import de.simon.dankelmann.submarine.Entities.LocationEntity
import de.simon.dankelmann.submarine.databinding.ActivityMainBinding
import de.simon.dankelmann.submarine.services.DatabaseService
import de.simon.dankelmann.submarine.services.ForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var appContext:AppContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // INITIALIZE THE APP CONTEXT
        AppContext.setContext(this)

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
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}