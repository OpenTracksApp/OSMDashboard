package de.storchp.opentracks.osmplugin.utils;

import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.compass.VectorCompass;
import de.storchp.opentracks.osmplugin.maps.MovementDirection;

public enum ArrowMode {

    DIRECTION(R.string.arrow_mode_direction) {
        public float getDegrees(final MovementDirection movementDirection, final VectorCompass compass) {
            return movementDirection.getCurrentDegrees();
        }
    },
    COMPASS(R.string.arrow_mode_compass) {
        public float getDegrees(final MovementDirection movementDirection, final VectorCompass compass) {
            return compass.getBearing().getCurrentDegrees();
        }
    },
    NORTH(R.string.arrow_mode_north) {
        public float getDegrees(final MovementDirection movementDirection, final VectorCompass compass) {
            return -compass.getBearing().getCurrentDegrees();
        }
    };

    private final int messageId;

    ArrowMode(final int messageId) {
        this.messageId = messageId;
    }

    public int getMessageId() {
        return messageId;
    }

    public ArrowMode next() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) {
            nextOrdinal = 0;
        }
        return values()[nextOrdinal];
    }

    public abstract float getDegrees(final MovementDirection movementDirection, final VectorCompass compass);

    public static ArrowMode valueOf(final String name, final ArrowMode defaultValue) {
        try {
            return valueOf(name);
        } catch (final IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

}
