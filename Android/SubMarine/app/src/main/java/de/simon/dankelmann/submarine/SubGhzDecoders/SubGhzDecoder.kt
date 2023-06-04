package de.simon.dankelmann.submarine.SubGhzDecoders

import android.graphics.Color
import de.simon.dankelmann.submarine.Entities.SignalEntity

interface SubGhzDecoder {
    fun getProtocolName():String
    fun isValid(signalEntity: SignalEntity):Boolean
    fun getInfoText():String
    fun getColorId():Int
}