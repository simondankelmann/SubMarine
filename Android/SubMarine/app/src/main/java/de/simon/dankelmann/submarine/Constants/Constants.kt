package de.simon.dankelmann.submarine.Constants

class Constants {
    companion object {
        val BLUETOOTH_COMMAND_HEADER_LENGTH = 8
        val CC1101_ADAPTER_CONFIGURATION_LENGTH = 30

        // COMMAND IDS
        var COMMAND_ID_DUMMY = "0000"

        // COMMANDS
        var COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND = "0001"
        var COMMAND_SET_OPERATION_MODE = "0002"
        var COMMAND_TRANSFER_SIGNAL_OVER_BLUETOOTH = "0003"

        // OPERATION MODES
        var OPERATIONMODE_IDLE = "0000"
        var OPERATIONMODE_HANDLE_INCOMING_BLUETOOTH_COMMAND = "0001"
        var OPERATIONMODE_PERISCOPE = "0002"

    }
}