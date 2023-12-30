package de.storchp.opentracks.osmplugin.utils;

import de.storchp.opentracks.osmplugin.maps.MovementDirection;

public enum MapMode {

    NORTH() {
        public float getHeading(MovementDirection movementDirection) {
            return 0;
        }
    },
    DIRECTION() {
        public float getHeading(MovementDirection movementDirection) {
            return -1 * movementDirection.getCurrentDegrees();
        }
    };

    public abstract float getHeading(MovementDirection movementDirection);

    public static MapMode valueOf(String name, MapMode defaultValue) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

}
