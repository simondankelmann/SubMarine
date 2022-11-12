package de.simon.dankelmann.esp32_subghz.Adapters

import android.Manifest
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import de.simon.dankelmann.submarine.Models.BluetoothDeviceModel
import de.simon.dankelmann.submarine.PermissionCheck.PermissionCheck
import de.simon.dankelmann.submarine.R

class BluetoothDeviceListviewAdapter(private val context: Context, private var deviceList: MutableList<BluetoothDeviceModel>) : BaseAdapter() {
    private lateinit var bluetoothName: TextView
    private lateinit var bluetoothAddress: TextView
    private lateinit var bluetoothRssi: TextView

    override fun getCount(): Int {
        return deviceList.size
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun getBluetoothDeviceModel(position: Int):BluetoothDeviceModel?{
        if(deviceList.size >= position){
            return deviceList[position]
        }
        return null
    }

    fun removeBluetoothDeviceListItem(itemIndex:Int){
        deviceList.removeAt(itemIndex)
        //notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var convertView = convertView
        convertView = LayoutInflater.from(context).inflate(R.layout.listrow_bluetooth_device, parent, false)

        bluetoothName = convertView!!.findViewById(R.id.bluetoothName)
        bluetoothAddress = convertView.findViewById(R.id.bluetoothAddress)
        bluetoothRssi = convertView.findViewById(R.id.bluetoothRssi)

        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
            bluetoothName.text = deviceList[position].device!!.name
            bluetoothAddress.text = deviceList[position].device!!.address
            bluetoothRssi.text = deviceList[position].rssi.toString() + " dB"
        }

        return convertView
    }
}