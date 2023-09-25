package de.storchp.opentracks.osmplugin.utils;

public enum TrackColorMode {

    UNI(true),
    BY_TRACK(true),
    BY_SPEED(false);

    private final boolean supportsLiveTrack;

    public static final TrackColorMode DEFAULT = BY_TRACK;

    TrackColorMode(final boolean supportsLiveTrack) {
        this.supportsLiveTrack = supportsLiveTrack;
    }

    public boolean isSupportsLiveTrack() {
        return supportsLiveTrack;
    }
}
