package de.storchp.opentracks.osmplugin.map.model

import org.oscim.core.GeoPoint

data class Waypoint(
    val id: Long? = null,
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val icon: String? = null,
    val trackId: Long? = null,
    val latLong: GeoPoint,
    val photoUrl: String? = null,
)
