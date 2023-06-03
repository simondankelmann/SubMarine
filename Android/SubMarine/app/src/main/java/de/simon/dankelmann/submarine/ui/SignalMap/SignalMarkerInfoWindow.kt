package de.simon.dankelmann.submarine.ui.SignalMap

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.navigation.findNavController
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.SubGhzDecoders.SubGhzDecoder
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow

class SignalMarkerInfoWindow(mapView: MapView, activity: Activity, signalEntity: SignalEntity, validDecoders : List<SubGhzDecoder>) : InfoWindow(R.layout.info_window_marker, mapView) {

    var _activity:Activity? = null
    var _mapView:MapView? = null
    var _signalEntity:SignalEntity? = null
    var _validDecoders:List<SubGhzDecoder> = mutableListOf()

    init{
        _activity = activity
        _mapView = mapView
        _signalEntity = signalEntity
        _validDecoders = validDecoders
    }

    override fun onOpen(item: Any?) {
        // Following command
        closeAllInfoWindowsOn(mapView)

        // Here we are settings onclick listeners for the buttons in the layouts.
        val viewDetailsButton = mView.findViewById<Button>(R.id.detailsButton)
        val titleTextView = mView.findViewById<TextView>(R.id.textviewTitle)
        val descriptionTextView = mView.findViewById<TextView>(R.id.textViewSignalInfoWindow)

        titleTextView.setText(_signalEntity!!.name)

        var desriptionText = ""

        desriptionText += "Samples: " + _signalEntity!!.signalDataLength.toString() + "" + "\n"
        desriptionText += "Frequency: " + _signalEntity!!.frequency.toString() + " Mhz" + "\n"
        desriptionText += "Type: " + _signalEntity!!.type.toString() + "" + "\n"

        if(_validDecoders.isNotEmpty()){
            _validDecoders.forEach {
                desriptionText += "Decoded: " + it.getInfoText() + "" + "\n"
            }
        }

        desriptionText += "Modulation: " + _signalEntity!!.modulation.toString() + "" + "\n"
        desriptionText += "Rx Bandwith: " + _signalEntity!!.rxBw.toString() + "" + "\n"

        descriptionTextView.setText(desriptionText)

        viewDetailsButton.setOnClickListener {
            // How to create a moveMarkerMapListener is not covered here.
            // Use the Map Listeners guide for this instead
            //mapView.addMapListener(MoveMarkerMapListener)

            val bundle = Bundle()
            bundle.putInt("SignalEntityId", _signalEntity!!.uid)
            _activity!!.findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_signal_map_to_nav_signalDetail, bundle)
        }

        /*
        deleteButton.setOnClickListener {
            // Do Something

            // In order to delete the marker,
            // You would probably have to pass the "map class"
            // where the map was created,
            // along with an ID to reference the marker.

            // Using a HashMap to store markers would be useful here
            // so that the markers can be referenced by ID.

            // Once you get the marker,
            // you would do map.overlays.remove(marker)
        }*/

        // You can set an onClickListener on the InfoWindow itself.
        // This is so that you can close the InfoWindow once it has been tapped.

        // Instead, you could also close the InfoWindows when the map is pressed.
        // This is covered in the Map Listeners guide.

        mView.setOnClickListener {
            close()
        }
    }

    override fun onClose() {
        /* NOTHING TO DO HERE */
    }

}

/*
abstract class SignalMarkerInfoWindow (mapView: MapView, signalEntity: SignalEntity, bluetoothDevice: BluetoothDevice) : InfoWindow(R.layout.info_window_marker, mapView) {

    override fun onOpen(item: Any?) {
        // Following command
        closeAllInfoWindowsOn(mapView)

        // Here we are settings onclick listeners for the buttons in the layouts.

        val viewDetailsButton = mView.findViewById<Button>(R.id.detailsButton)
        val titleTextView = mView.findViewById<TextView>(R.id.textviewTitle)

        viewDetailsButton.setOnClickListener {
            // How to create a moveMarkerMapListener is not covered here.
            // Use the Map Listeners guide for this instead
            //mapView.addMapListener(MoveMarkerMapListener)

            val bundle = Bundle()
            bundle.putParcelable("Device", bluetoothDevice)
            bundle.putInt("SignalEntityId", this.)
            requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_signal_map_to_nav_signalDetail, bundle)
        }
        deleteButton.setOnClickListener {
            // Do Something

            // In order to delete the marker,
            // You would probably have to pass the "map class"
            // where the map was created,
            // along with an ID to reference the marker.

            // Using a HashMap to store markers would be useful here
            // so that the markers can be referenced by ID.

            // Once you get the marker,
            // you would do map.overlays.remove(marker)
        }

        // You can set an onClickListener on the InfoWindow itself.
        // This is so that you can close the InfoWindow once it has been tapped.

        // Instead, you could also close the InfoWindows when the map is pressed.
        // This is covered in the Map Listeners guide.

        mView.setOnClickListener {
            close()
        }
    }

}*/

