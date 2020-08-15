package de.storchp.opentracks.osmplugin.utils;

import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.maps.CompassRotation;
import de.storchp.opentracks.osmplugin.maps.MovementDirection;

public enum ArrowMode {

    DIRECTION(R.string.arrow_mode_heading, false) {
        public float getDegrees(final MovementDirection movementDirection, final CompassRotation compassRotation) {
            return movementDirection.getCurrentDegrees();
        }
    },
    NORTH(R.string.arrow_mode_north, true) {
        public float getDegrees(final MovementDirection movementDirection, final CompassRotation compassRotation) {
            return compassRotation.getCurrentDegrees();
        }
    };

    private final int messageId;

    private final boolean updateable;

    ArrowMode(final int messageId, final boolean updateable) {
        this.messageId = messageId;
        this.updateable = updateable;
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

    public abstract float getDegrees(final MovementDirection movementDirection, final CompassRotation compassRotation);

    public boolean isUpdateable() {
        return updateable;
    }
}
