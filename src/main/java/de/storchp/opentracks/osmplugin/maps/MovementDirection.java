package de.storchp.opentracks.osmplugin.maps;

import org.mapsforge.core.model.LatLong;

import de.storchp.opentracks.osmplugin.utils.MapUtils;

public class MovementDirection {

    private LatLong secondToLastPos;
    private float currentDegrees = 0;

    public float updatePos(final LatLong endPos) {
        if (endPos != null && !endPos.equals(secondToLastPos)) {
            currentDegrees = MapUtils.bearingInDegrees(secondToLastPos, endPos);
            secondToLastPos = endPos;
        }

        return currentDegrees;
    }

    public float getCurrentDegrees() {
        return currentDegrees;
    }

}
