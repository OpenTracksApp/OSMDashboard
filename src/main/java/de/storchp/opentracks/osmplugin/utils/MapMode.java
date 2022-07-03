package de.storchp.opentracks.osmplugin.utils;

import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.compass.Compass;
import de.storchp.opentracks.osmplugin.maps.MovementDirection;

public enum MapMode {

    NORTH(R.string.map_mode_north_top) {
        public float getHeading(MovementDirection movementDirection, Compass compass) {
            return 0;
        }
    },
    DIRECTION(R.string.map_mode_direction) {
        public float getHeading(MovementDirection movementDirection, Compass compass) {
            return movementDirection.getCurrentDegrees();
        }
    },
    COMPASS(R.string.map_mode_compass) {
        public float getHeading(MovementDirection movementDirection, Compass compass) {
            return compass.getBearing().getValue();
        }
    };

    private final int messageId;

    MapMode(int messageId) {
        this.messageId = messageId;
    }

    public int getMessageId() {
        return messageId;
    }

    public MapMode next() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) {
            nextOrdinal = 0;
        }
        return values()[nextOrdinal];
    }

    public abstract float getHeading(MovementDirection movementDirection, Compass compass);

    public static MapMode valueOf(String name, MapMode defaultValue) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

}
