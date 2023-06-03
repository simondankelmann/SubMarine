package de.simon.dankelmann.submarine.SubGhzDecoders

import de.simon.dankelmann.submarine.Entities.SignalEntity

class SubGhzDecoderRegistry {
    private var _decoders: MutableList<SubGhzDecoder> = mutableListOf()

    constructor(){
        _decoders.add(Princeton24Bit())
    }

    fun validateSignal(signalEntity: SignalEntity):List<SubGhzDecoder>{
        var validDecoders = mutableListOf<SubGhzDecoder>()

        _decoders.forEach {
            if(it.isValid(signalEntity)){
                validDecoders.add(it)
            }
        }

        return  validDecoders.toList()
    }
}