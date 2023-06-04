package de.simon.dankelmann.submarine.SubGhzDecoders

import android.util.Log
import de.simon.dankelmann.submarine.Entities.SignalEntity
import de.simon.dankelmann.submarine.Models.BinHex
import de.simon.dankelmann.submarine.Models.ToneSilencePair
import de.simon.dankelmann.submarine.R
import kotlin.math.sign
// BERNER / ELKA / TEDSEN / TELETASTER
class Bett18Bit:SubGhzDecoder {
    private var _logTag = "SubGzhDecoder_B.E.T.T."
    private var _decodedBinaryValues = mutableListOf<BinHex>()
    private var _decodedHex:String = ""
    private var _decodedSequences = mutableListOf<BinHex>()

    // SIGNAL PARAMETERS
    private var _validSequenceLength = 18
    private var _intervalDuration = 1500 // 450 microseconds
    private var _sequenceBreak = _intervalDuration * 10

    override fun getProtocolName(): String {
        return "B.E.T.T. 18 Bit"
    }

    override fun isValid(signalEntity: SignalEntity): Boolean {
        // GET TONE / SILENCE PAIRS
        var toneSilencePairs = getToneSilencePairs(signalEntity)
        //Log.d(_logTag, "Detected: " + toneSilencePairs.size.toString() + " Tone Silence Pairs")

        // SORT INTO SEQUENCES
        var validSequences = getValidSequences(toneSilencePairs)
        //Log.d(_logTag, "Detected: " + validSequences.size.toString() + " valid Sequences")

        // DECODE SEQUENCES
        var decodedSequences = decodeSequences(validSequences)
        decodedSequences.forEach {
            //Log.d(_logTag, "Detected Sequence: "+ it.getHexString() + " | " + it.getBinaryString())
        }

        if(decodedSequences.isNotEmpty()){
            _decodedSequences = decodedSequences.toMutableList()
            _decodedHex = decodedSequences.first().getHexString()
            return true
        }

        return false
    }

    override fun getInfoText(): String {
        return getProtocolName() + ", Hex: " + this._decodedHex
    }

    override fun getColorId(): Int {
        return R.color.decoded_signal_color_bett_18_bit
    }

    fun getToneSilencePairs(signalEntity: SignalEntity):List<ToneSilencePair>{
        var toneSilencePairs = mutableListOf<ToneSilencePair>()
        val rawData = signalEntity.signalData
        if(rawData != null){
            val rawList = rawData.split(',')
            var toneSilencePair:ToneSilencePair = ToneSilencePair()
            if(rawList != null && rawList.size > 0){
                rawList.forEach {
                    var rawValue = it.toInt()
                    if(rawValue < 0){
                        toneSilencePair.silence = rawValue
                        if(toneSilencePair.silence != 0 && toneSilencePair.tone != 0){
                            // VALID TONE SILENCE PAIR
                            toneSilencePairs.add(toneSilencePair)
                        }
                        // RESET
                        toneSilencePair = ToneSilencePair()
                    } else {
                        toneSilencePair.tone = rawValue
                    }
                }
            }
        }
        return toneSilencePairs.toList()
    }

    fun getValidSequences(toneSilencePairs:List<ToneSilencePair>):List<List<ToneSilencePair>>{
        var validSequences = mutableListOf<List<ToneSilencePair>>()
        var sequence = mutableListOf<ToneSilencePair>()
        toneSilencePairs.forEach { toneSilencePair ->
            var unsignedSilence = toneSilencePair.silence * -1
            if(unsignedSilence >= _sequenceBreak){
                // ALSO ADD THE TRAILING ZERO
                sequence.add(toneSilencePair)
                // BEGINNING OR ENDING SEQUENCE
                if(sequence.size == _validSequenceLength){
                    validSequences.add(sequence.toList())
                } else {
                    Log.d(_logTag, "Sequence Lenght: " + sequence.size.toString())
                }
                // RESET FOR NEW SEQUENCE
                sequence = mutableListOf<ToneSilencePair>()
            } else {
                sequence.add(toneSilencePair)
            }
        }
        return validSequences
    }

    fun decodeSequences(sequences: List<List<ToneSilencePair>>):List<BinHex>{
        var decodedSequences = mutableListOf<BinHex>()

        sequences.forEach { sequence ->
            var binaryData = BinHex()
            sequence.forEach { toneSilencePair ->
                if(toneSilencePair.tone < _intervalDuration && (toneSilencePair.silence * -1) > _intervalDuration ){
                    binaryData.addBit(0)
                } else {
                    binaryData.addBit(1)
                }
            }
            decodedSequences.add(binaryData)
        }

        return decodedSequences.toList()
    }

}