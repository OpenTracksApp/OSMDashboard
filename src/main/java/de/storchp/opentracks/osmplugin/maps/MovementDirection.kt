package de.storchp.opentracks.osmplugin.maps;

import org.oscim.core.GeoPoint;

import de.storchp.opentracks.osmplugin.utils.MapUtils;

public class MovementDirection {

    private GeoPoint secondToLastPos;
    private float currentDegrees = 0;

    public void updatePos(GeoPoint endPos) {
        if (endPos != null && !endPos.equals(secondToLastPos)) {
            currentDegrees = MapUtils.bearingInDegrees(secondToLastPos, endPos);
            secondToLastPos = endPos;
        }
    }

    public float getCurrentDegrees() {
        return currentDegrees;
    }

}
