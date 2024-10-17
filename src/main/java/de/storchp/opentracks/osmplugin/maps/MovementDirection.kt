package de.storchp.opentracks.osmplugin.maps

import de.storchp.opentracks.osmplugin.utils.MapUtils
import org.oscim.core.GeoPoint

class MovementDirection {
    private var secondToLastPos: GeoPoint? = null
    var currentDegrees = 0f
        get() = currentDegrees

    fun updatePos(endPos: GeoPoint?) {
        if (endPos != null && endPos != secondToLastPos) {
            currentDegrees = MapUtils.bearingInDegrees(secondToLastPos, endPos)
            secondToLastPos = endPos
        }
    }

}
