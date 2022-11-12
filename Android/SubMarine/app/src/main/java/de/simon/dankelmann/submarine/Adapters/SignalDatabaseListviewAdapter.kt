package de.simon.dankelmann.esp32_subghz.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.R

class SignalDatabaseListviewAdapter(private val context: Context, private var signalEntityList: MutableList<SignalEntity>) : BaseAdapter() {
    private lateinit var signalName: TextView
    private lateinit var signalFrequency: TextView
    private lateinit var signalInfo: TextView

    override fun getCount(): Int {
        return signalEntityList.size
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun getSignalEntity(position: Int):SignalEntity?{
        if(signalEntityList.size >= position){
            return signalEntityList[position]
        }
        return null
    }

    fun removeBluetoothDeviceListItem(itemIndex:Int){
        signalEntityList.removeAt(itemIndex)
        //notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var convertView = convertView
        convertView = LayoutInflater.from(context).inflate(R.layout.listrow_signal_entity, parent, false)

        signalName = convertView!!.findViewById(R.id.signalName)
        signalInfo = convertView.findViewById(R.id.signalInfo)
        signalFrequency = convertView.findViewById(R.id.signalFrequency)

        signalName.text = signalEntityList[position].name
        signalInfo.text = signalEntityList[position].type + " | " + signalEntityList[position].signalDataLength.toString() + " Samples"
        signalFrequency.text = signalEntityList[position].frequency.toString() + " Mhz"

        return convertView
    }
}