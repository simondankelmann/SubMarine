package de.simon.dankelmann.submarine.Interfaces

import android.location.Location
import android.service.controls.actions.CommandAction
import de.simon.dankelmann.submarine.Models.SubmarineCommand

interface SubmarineResultListenerInterface {
    fun onConnectionStateChanged(connectionState:Int)
    fun onIncomingData(data:String, command:SubmarineCommand?)
    fun onOutgoingData(timeElapsed: Int, command:SubmarineCommand?)

    fun onCommandSent(timeElapsed: Int, command:SubmarineCommand)
    fun onOperationModeSet(timeElapsed: Int, command:SubmarineCommand)
    fun onSignalReplayed(timeElapsed: Int, command:SubmarineCommand)
}