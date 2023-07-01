package de.storchp.opentracks.osmplugin.utils;

import de.storchp.opentracks.osmplugin.R;

public enum TrackColorMode {

    UNI(R.string.track_color_mode_uni, true),
    BY_TRACK(R.string.track_color_mode_by_track, true),
    BY_SPEED(R.string.track_color_mode_by_speed, false);

    private final int labelResId;
    private final boolean supportsLiveTrack;

    public static final TrackColorMode DEFAULT = BY_TRACK;

    TrackColorMode(final int labelResId, final boolean supportsLiveTrack) {
        this.labelResId = labelResId;
        this.supportsLiveTrack = supportsLiveTrack;
    }

    public int getLabelResId() {
        return labelResId;
    }

    public boolean isSupportsLiveTrack() {
        return supportsLiveTrack;
    }
}
