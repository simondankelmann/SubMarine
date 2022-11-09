package de.simon.dankelmann.submarine.Interfaces

import android.location.Location

interface LocationResultListener {
    fun receiveLocationChanges(location: Location)
}