package de.simon.dankelmann.submarine.Services

import android.util.Log
import java.lang.Math.round

class SignalAnalyzer {
    private var _logTag = "SignalAnalzyer"

    fun ConvertTimingsToBinaryStringList(timings:List<Int>, samplesPerSymbol:Int):List<String>{

        var stringList:MutableList<String> = mutableListOf()

        var binaryString = ""
        var samplesPerSymbolHalf:Int = samplesPerSymbol / 2

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
        if(binaryString.length == 0){
            return ""
        }
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

    fun convertHexStringToBinaryString(hexString:String, autoComplete:Boolean=true):String{
        var inputString = hexString
        var binaryString = ""

        if(hexString.startsWith("0x")){
            inputString = hexString.substring(2)
            Log.d(_logTag, "Removed prepending 0x: " + inputString)
        }

        inputString.chunked(1).map {
            var current = it

            var int16 = current.toInt(16)
            var binString = int16.toString(2)

            // FILL IN LEADING ZEROS
            while (binString.length < 4){
                binString = "0" + binString
            }

            // DEBUG LOG
            /*
            Log.d(_logTag, "CHUNK: " + current)
            Log.d(_logTag, "INT16: " + int16)
            Log.d(_logTag, "BINSTRING: " + binString)
            */

            binaryString += binString
        }

        return binaryString
    }

    fun convertBinaryStringToTimingsList(binaryString:String, samplesPerSymbol:Int):List<Int>{
        var buffer = mutableListOf<Int>()
        var timingsList:MutableList<Int> = mutableListOf()

        var currentChar:Char? = null
        var currentOccurences = 0
        binaryString.map {

            if(currentChar == null){
                // FIRST ONE
                currentChar = it
                currentOccurences ++
            } else {
                if(it == currentChar){
                    // RAISE OCCURENCES
                    currentOccurences++
                } else {
                    // ADD TO LIST , CHANGE COMPARE CHAR
                    var addableValue:Int = currentOccurences
                    if(currentChar == '0'){
                        addableValue = addableValue * -1
                    } else {
                    }

                    buffer.add(addableValue)
                    currentOccurences = 1
                    currentChar = it
                }
            }
        }

        buffer.map {
            var timing = it * (samplesPerSymbol / 2)
            timingsList.add(timing)
        }

        return timingsList
    }


    fun convertHexStringToTimingsList(hexString:String, samplesPerSymbol:Int):List<String>{
        Log.d(_logTag, "Converting Hex:" + hexString + " to Timings with " + samplesPerSymbol + " Samples/Symbol")
        var timingsList:MutableList<String> = mutableListOf()

        var binString = convertHexStringToBinaryString(hexString)

        var timings = convertBinaryStringToTimingsList(binString, samplesPerSymbol)
        timings.map {
            timingsList.add(it.toString())
        }

        Log.d(_logTag, "Timings: " + timingsList.size)
        return timingsList
    }


}