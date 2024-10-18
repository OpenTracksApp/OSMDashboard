package de.storchp.opentracks.osmplugin.map

import java.lang.IllegalArgumentException

enum class MapMode {
    NORTH {
        override fun getHeading(movementDirection: MovementDirection) = 0f
    },
    DIRECTION {
        override fun getHeading(movementDirection: MovementDirection) =
            -1f * movementDirection.currentDegrees
    };

    abstract fun getHeading(movementDirection: MovementDirection): Float

}

fun String.toMapMode(defaultValue: MapMode) =
    try {
        MapMode.valueOf(this)
    } catch (_: IllegalArgumentException) {
        defaultValue
    }

