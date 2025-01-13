package de.storchp.opentracks.osmplugin.map

val DEFAULT_TRACK_COLOR_MODE: TrackColorMode = TrackColorMode.BY_TRACK

enum class TrackColorMode(val supportsLiveTrack: Boolean) {
    UNI(true),
    BY_TRACK(true),
    BY_SPEED(false);
}
