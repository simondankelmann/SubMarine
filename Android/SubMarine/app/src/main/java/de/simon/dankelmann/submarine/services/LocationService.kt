package de.simon.dankelmann.submarine.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import de.simon.dankelmann.submarine.Interfaces.LocationResultListener

class LocationService : LocationListener {
    private var _context : Context? = null;
    var _resultListener: LocationResultListener? = null
    var _locationManager: LocationManager? = null

    constructor (context: Context, resultListener : LocationResultListener){
        // CONSTRUCTOR
        _context = context
        _resultListener = resultListener
        _locationManager = _context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        startScanning()
    }


    fun startScanning() {
        //START LISTENING
        if(ContextCompat.checkSelfPermission(
                _context!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                _context!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED  ){
            var minTime:Long = 0
            var minDistance:Float = 0f
            _locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER,minTime,minDistance,this)
            _locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,minTime,minDistance,this)


        } else {
            // Request Permission
            Log.e("LocationProvider", "Permission not granted")
        }
    }


    fun stopScanning() {
        _locationManager?.removeUpdates(this)
    }

    // LocationListener Methods
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onLocationChanged(location: Location) {
        _resultListener?.receiveLocationChanges(location!!)
    }
}