package de.storchp.opentracks.osmplugin.utils;

import de.storchp.opentracks.osmplugin.compass.Compass;
import de.storchp.opentracks.osmplugin.maps.MovementDirection;

public enum MapMode {

    NORTH() {
        public float getHeading(MovementDirection movementDirection, Compass compass) {
            return 0;
        }
    },
    DIRECTION() {
        public float getHeading(MovementDirection movementDirection, Compass compass) {
            return movementDirection.getCurrentDegrees();
        }
    },
    COMPASS() {
        public float getHeading(MovementDirection movementDirection, Compass compass) {
            return compass.getBearing().getValue();
        }
    };

    public abstract float getHeading(MovementDirection movementDirection, Compass compass);

    public static MapMode valueOf(String name, MapMode defaultValue) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

}
