package de.simon.dankelmann.submarine.Services

import android.util.Log
import java.lang.Math.log
import java.lang.Math.round

class SignalAnalyzer {
    private var _logTag = "SignalAnalzyer"

    fun ConvertTimingsToBinaryStringList(timings:List<Int>, symbolsPerBit:Int):List<String>{

        var stringList:MutableList<String> = mutableListOf()

        var binaryString = ""
        var samplesPerSymbolHalf:Int = symbolsPerBit / 2

        timings.map {
            // DETERMINE IF ITS A ONE OR A ZERO
            var symbol = "0"
            if(it > 0){ symbol = "1" }

            // DETECT THE NUMBER OF SYMBOLS
            var numberOfSymbols = round(it / samplesPerSymbolHalf.toDouble())
            if(numberOfSymbols < 0){
                numberOfSymbols *= -1
            }

            for (i in 0 until numberOfSymbols) {
                binaryString += symbol
            }

            if (numberOfSymbols > 32){
                // END OF SIGNAL, NEXT ROW

                // CLEAR ALL ZEROS AT THE END BECAUSE WE DONT NEED THEM ANYMORE
                while(binaryString.endsWith("0")){
                    binaryString = binaryString.dropLast(1)
                }

                // ADD TO THE RETURNED LIST
                stringList.add(binaryString)

                // RESET
                binaryString = ""
            }
        }

        return stringList
    }

    fun ConvertBinaryStringToHexString(binaryString:String):String{
        //Log.d(_logTag, "BINARY STRING: " + binaryString)
        var hexString = "0x"

        // GET SUBSTRINGS OF 8 CHARS
        val bitStringLength = 4
        var numberOfBytes = binaryString.length / bitStringLength
        for (i in 0 .. numberOfBytes) {
            val startIndex = i * bitStringLength
            var endIndex = startIndex + bitStringLength
            if(binaryString.length <= endIndex){
                endIndex = binaryString.length
            }
            var byteString = binaryString.substring(startIndex, endIndex)

            //Log.d(_logTag, "ByteString: " + byteString)


            while(byteString.length < bitStringLength){
                byteString = byteString + "0"
            }

            try {
                val byte: Byte = Integer.valueOf(byteString, 2).toByte()
                var byteAsHex = byte.toString(16)// String.format("%02X", byte)
                hexString += byteAsHex
            } catch (e:Exception){
                Log.d(_logTag, e.message.toString())
            }


        }

        Log.d(_logTag,"hexString: " + hexString)
        return hexString
    }

}