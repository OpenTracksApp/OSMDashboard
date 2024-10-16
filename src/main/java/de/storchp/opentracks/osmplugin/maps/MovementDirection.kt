package de.storchp.opentracks.osmplugin.maps

import de.storchp.opentracks.osmplugin.utils.MapUtils
import org.oscim.core.GeoPoint

class MovementDirection {
    private var secondToLastPos: GeoPoint? = null
    private var currentDegrees = 0f

    fun updatePos(endPos: GeoPoint?) {
        if (endPos != null && endPos != secondToLastPos) {
            currentDegrees = MapUtils.bearingInDegrees(secondToLastPos, endPos)
            secondToLastPos = endPos
        }
    }

    fun getCurrentDegrees(): Float {
        return currentDegrees
    }
}
