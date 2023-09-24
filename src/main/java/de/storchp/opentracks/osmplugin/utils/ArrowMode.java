package de.storchp.opentracks.osmplugin.utils;

import de.storchp.opentracks.osmplugin.compass.Compass;
import de.storchp.opentracks.osmplugin.maps.MovementDirection;

public enum ArrowMode {

    DIRECTION() {
        public float getDegrees(MovementDirection movementDirection, Compass compass) {
            return movementDirection.getCurrentDegrees();
        }
    },
    COMPASS() {
        public float getDegrees(MovementDirection movementDirection, Compass compass) {
            return -compass.getBearing().getValue();
        }
    },
    NORTH() {
        public float getDegrees(MovementDirection movementDirection, Compass compass) {
            return compass.getBearing().getValue();
        }
    };

    public abstract float getDegrees(MovementDirection movementDirection, Compass compass);

    public static ArrowMode valueOf(String name, ArrowMode defaultValue) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

}
