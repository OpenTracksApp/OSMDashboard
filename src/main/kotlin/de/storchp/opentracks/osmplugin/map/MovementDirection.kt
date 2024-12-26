package de.storchp.opentracks.osmplugin.map

import org.oscim.core.GeoPoint

class MovementDirection {
    private var secondToLastPos: GeoPoint? = null
    var currentDegrees = 0f
        private set

    fun updatePos(endPos: GeoPoint?) {
        if (endPos != null && endPos != secondToLastPos) {
            currentDegrees = MapUtils.bearingInDegrees(secondToLastPos, endPos)
            secondToLastPos = endPos
        } else {
            currentDegrees = 0f
        }
    }

}
