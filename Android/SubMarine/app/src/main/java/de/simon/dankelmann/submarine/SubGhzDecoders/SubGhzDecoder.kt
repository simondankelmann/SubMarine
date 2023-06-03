package de.simon.dankelmann.submarine.SubGhzDecoders

import de.simon.dankelmann.submarine.Entities.SignalEntity

interface SubGhzDecoder {
    fun getProtocolName():String
    fun isValid(signalEntity: SignalEntity):Boolean
    fun getInfoText():String
}