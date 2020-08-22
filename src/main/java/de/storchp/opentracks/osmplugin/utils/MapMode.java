package de.storchp.opentracks.osmplugin.utils;

import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.compass.Compass;
import de.storchp.opentracks.osmplugin.maps.MovementDirection;

public enum MapMode {

    NORTH(R.string.map_mode_north_top) {
        public float getHeading(final MovementDirection movementDirection, final Compass compass) {
            return 0;
        }
    },
    DIRECTION(R.string.map_mode_direction) {
        public float getHeading(final MovementDirection movementDirection, final Compass compass) {
            return movementDirection.getCurrentDegrees();
        }
    },
    COMPASS(R.string.map_mode_compass) {
        public float getHeading(final MovementDirection movementDirection, final Compass compass) {
            return compass.getBearing().getValue();
        }
    };

    private final int messageId;

    MapMode(final int messageId) {
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

    public abstract float getHeading(final MovementDirection movementDirection, final Compass compass);

    public static MapMode valueOf(final String name, final MapMode defaultValue) {
        try {
            return valueOf(name);
        } catch (final IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

}
