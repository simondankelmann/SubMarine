package de.simon.dankelmann.submarine.Models

import android.util.Log
import de.simon.dankelmann.submarine.Constants.Constants
import de.simon.dankelmann.submarine.Interfaces.SubmarineResultListenerInterface

class SubmarineCommand (command:String, commandId: String, data:String){

    private var _logTag = "SubmarineCommand"
    var _command = ""
    var _commandId = ""
    var _data = ""

    init{
        _command = command
        _commandId = commandId
        _data = data
    }

    fun isValid():Boolean{
        return _command != "" && _commandId != ""
    }

    fun getCommandString():String{
        if(!_data.endsWith(Constants.EOL_CHAR)){
            Log.d(_logTag, "Appending EOL Char: " + Constants.EOL_CHAR)
            _data += Constants.EOL_CHAR
        }
        return _command + _commandId + _data
    }

    companion object{
        fun parseFromDataString(dataString:String):SubmarineCommand?{
            // PARSE COMMAND AND DATA
            if(dataString.length >= Constants.BLUETOOTH_COMMAND_HEADER_LENGTH){
                var command = dataString.substring(0,4)
                var commandId = dataString.substring(4,8)
                var commandData = dataString.substring(Constants.BLUETOOTH_COMMAND_HEADER_LENGTH)

                var submarineCommand:SubmarineCommand = SubmarineCommand(command, commandId, commandData)
                if(submarineCommand.isValid()){
                    return submarineCommand
                }
            }
            return null
        }
    }

}