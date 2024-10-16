package de.storchp.opentracks.osmplugin.utils

import de.storchp.opentracks.osmplugin.maps.MovementDirection
import java.lang.IllegalArgumentException

enum class MapMode {
    NORTH {
        override fun getHeading(movementDirection: MovementDirection): Float {
            return 0f
        }
    },
    DIRECTION {
        override fun getHeading(movementDirection: MovementDirection): Float {
            return -1f * movementDirection.getCurrentDegrees()
        }
    };

    abstract fun getHeading(movementDirection: MovementDirection): Float

    companion object {
        fun valueOf(name: String?, defaultValue: MapMode) = try {
            MapMode.valueOf(name!!)
        } catch (_: IllegalArgumentException) {
            defaultValue
        }
    }
}
