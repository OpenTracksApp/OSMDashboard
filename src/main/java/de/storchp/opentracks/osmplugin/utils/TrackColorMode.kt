package de.storchp.opentracks.osmplugin.utils

enum class TrackColorMode(supportsLiveTrack: Boolean) {
    UNI(true),
    BY_TRACK(true),
    BY_SPEED(false);

    private val supportsLiveTrack: Boolean

    init {
        this.supportsLiveTrack = supportsLiveTrack
    }

    fun isSupportsLiveTrack(): Boolean {
        return supportsLiveTrack
    }

    companion object {
        val DEFAULT: TrackColorMode = TrackColorMode.BY_TRACK
    }
}
