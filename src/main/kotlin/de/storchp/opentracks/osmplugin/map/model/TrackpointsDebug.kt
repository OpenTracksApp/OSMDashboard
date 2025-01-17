package de.storchp.opentracks.osmplugin.map.model

data class TrackpointsDebug(
    var trackpointsReceived: Int = 0,
    var trackpointsInvalid: Int = 0,
    var trackpointsDrawn: Int = 0,
    var trackpointsPause: Int = 0,
    var segments: Int = 0,
    var protocolVersion: Int = 0,
) {
    fun add(other: TrackpointsDebug) {
        this.trackpointsReceived += other.trackpointsReceived
        this.trackpointsInvalid += other.trackpointsInvalid
        this.trackpointsDrawn += other.trackpointsDrawn
        this.trackpointsPause += other.trackpointsPause
        this.segments += other.segments
    }
}
