package de.storchp.opentracks.osmplugin.utils

import de.storchp.opentracks.osmplugin.utils.TrackColorMode.BY_TRACK

val DEFAULT_TRACK_COLOR_MORE: TrackColorMode = BY_TRACK

enum class TrackColorMode(val supportsLiveTrack: Boolean) {
    UNI(true),
    BY_TRACK(true),
    BY_SPEED(false);
}
