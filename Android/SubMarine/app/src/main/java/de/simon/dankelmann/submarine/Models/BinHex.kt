package de.simon.dankelmann.submarine.Models

import android.util.Log
import de.simon.dankelmann.submarine.Services.SignalAnalyzer

class BinHex {
    private val _logTag = "HexBin"
    private var _bits = mutableListOf<Int>()

    fun addBit(bit:Int){
        _bits.add(bit)
    }

    fun getBinaryString():String{
        return _bits.joinToString("")
    }

    fun getHexString():String{
        var hexString = ""

        // GET VALID BYTES FROM END TO START
        var reversed = _bits.reversed()
        var chunkedBits = reversed.chunked(4).reversed()

        chunkedBits.forEach{ chunked ->
            val byte: Byte = Integer.valueOf(chunked.reversed().joinToString(""), 2).toByte()
            var byteAsHex = byte.toString(16)
            hexString += byteAsHex
        }

        return hexString
    }
}