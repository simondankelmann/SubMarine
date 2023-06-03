package de.simon.dankelmann.esp32_subghz.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.R
import de.simon.dankelmann.submarine.SubGhzDecoders.SubGhzDecoderRegistry

class SignalDatabaseListviewAdapter(private val context: Context, private var signalEntityList: MutableList<SignalEntity>) : BaseAdapter() {
    private lateinit var signalName: TextView
    //private lateinit var signalFrequency: TextView
    private lateinit var signalInfo: TextView
    private lateinit var signalIcon: ImageView

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
        //signalFrequency = convertView.findViewById(R.id.signalFrequency)
        signalIcon = convertView.findViewById(R.id.signalIcon)

        var iconColorId = R.color.fontcolor_component_dark_inactive
        if(signalEntityList[position].proofOfWork){
            iconColorId = R.color.accent_color_darkmode
        }

        // CHECK IF THE SIGNALS PROTOCOL CAN BE DETECTED
        var decodedProtocols = ""
        var pre = ""
        var validDecoders = SubGhzDecoderRegistry().validateSignal(signalEntityList[position])
        if(validDecoders.isNotEmpty() && !signalEntityList[position].proofOfWork){
            iconColorId = R.color.decoded_signal_color_darkmode

            validDecoders.forEach {
                decodedProtocols += pre + it.getProtocolName()
                pre = ","
            }
        }

        var signalType = signalEntityList[position].type
        if(decodedProtocols.isNotEmpty()){
            signalType = decodedProtocols
        }

        signalName.text = signalEntityList[position].name
        signalInfo.text = signalEntityList[position].frequency.toString() + " Mhz" + " | " + signalType + " | " + signalEntityList[position].signalDataLength.toString() + " Samples"
        //signalFrequency.text = signalEntityList[position].frequency.toString() + " Mhz"
        signalIcon.setColorFilter(ContextCompat.getColor(convertView.context,iconColorId))

        return convertView
    }
}