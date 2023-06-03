package de.simon.dankelmann.submarine.Models

import de.simon.dankelmann.submarine.Services.SignalAnalyzer

class BinHex {
    private var _bits = mutableListOf<Int>()

    fun addBit(bit:Int){
        _bits.add(bit)
    }

    fun getBinaryString():String{
        return _bits.joinToString("")
    }

    fun getHexString():String{
        var hexString = ""
        var chunkedBits = _bits.chunked(4)
        chunkedBits.forEach{ chunked ->
            val byte: Byte = Integer.valueOf(chunked.joinToString(""), 2).toByte()
            var byteAsHex = byte.toString(16)
            hexString += byteAsHex
        }

        return hexString
    }
}