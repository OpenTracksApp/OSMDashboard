package de.storchp.opentracks.osmplugin.utils;

public class TrackPointsDebug {
    public int trackpointsReceived = 0;
    public int trackpointsInvalid = 0;
    public int trackpointsDrawn = 0;
    public int trackpointsPause = 0;
    public int segments = 0;

    public void add(final TrackPointsDebug other) {
        this.trackpointsReceived += other.trackpointsReceived;
        this.trackpointsInvalid += other.trackpointsInvalid;
        this.trackpointsDrawn += other.trackpointsDrawn;
        this.trackpointsPause += other.trackpointsPause;
        this.segments += other.segments;
    }
}
