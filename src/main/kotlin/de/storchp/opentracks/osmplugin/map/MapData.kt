package de.storchp.opentracks.osmplugin.map

import de.storchp.opentracks.osmplugin.dashboardapi.Waypoint
import org.oscim.core.BoundingBox
import org.oscim.core.GeoPoint
import org.oscim.layers.GroupLayer
import org.oscim.layers.PathLayer
import org.oscim.layers.marker.ItemizedLayer
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol
import org.oscim.map.Map


data class MapData(
    private val map: Map,
    private val polylinesLayer: GroupLayer,
    private val waypointsLayer: ItemizedLayer,
    private val strokeWidth: Int,
    val mapMode: MapMode,
    private val pauseMarkerSymbol: MarkerSymbol,
    private val waypointMarkerSymbol: MarkerSymbol,
    private val compassMarkerSymbol: MarkerSymbol,
) {

    var boundingBox: BoundingBox? = null
    val movementDirection = MovementDirection()
    var endMarker: MarkerItem? = null
    var startPos: GeoPoint? = null
    var endPos: GeoPoint? = null
    var polyline: PathLayer? = null

    fun addNewPolyline(trackColor: Int, geoPoint: GeoPoint): PathLayer? {
        polyline = PathLayer(map, trackColor, strokeWidth.toFloat())
        polylinesLayer.layers.add(polyline)

        if (endPos != null) {
            polyline!!.addPoint(endPos)
        } else if (startPos != null) {
            polyline!!.addPoint(startPos)
        }

        addPoint(geoPoint)
        return polyline
    }

    fun extendPolyline(trackColor: Int, geoPoint: GeoPoint): PathLayer? {
        if (polyline == null) {
            polyline = PathLayer(map, trackColor, strokeWidth.toFloat())
            polylinesLayer.layers.add(polyline)
        }

        addPoint(geoPoint)
        return polyline
    }

    private fun addPoint(geoPoint: GeoPoint) {
        endPos = geoPoint
        polyline!!.addPoint(endPos)
        if (startPos == null) {
            startPos = endPos
        }
    }

    fun resetCurrentPolyline() {
        polyline = null // reset current polyline when trackId changes
        startPos = null
        endPos = null
    }

    fun addPauseMarker(geoPoint: GeoPoint) {
        val marker = MapUtils.createMarker(geoPoint, pauseMarkerSymbol)
        waypointsLayer.addItem(marker)
    }

    fun setEndMarker() {
        endPos?.let {
            synchronized(map.layers()) {
                endMarker?.let {
                    it.geoPoint = endPos
                    it.setRotation(MapUtils.rotateWith(mapMode, movementDirection))
                    waypointsLayer.populate()
                    map.render()
                } ?: {
                    endMarker = MarkerItem(endPos.toString(), "", endPos).apply {
                        marker = compassMarkerSymbol
                        setRotation(MapUtils.rotateWith(mapMode, movementDirection))
                        waypointsLayer.addItem(this)
                    }
                }
            }
        }
    }

    fun updateMapPositionAndRotation(myPos: GeoPoint) {
        val newPos = map.getMapPosition().setPosition(myPos)
            .setBearing(mapMode.getHeading(movementDirection))
        map.animator().animateTo(newPos)
    }

    fun renderMap() {
        map.render()
    }

    fun addWaypointMarker(waypoint: Waypoint) {
        val marker = MapUtils.createMarker(waypoint.id, waypoint.latLong!!, waypointMarkerSymbol)
        waypointsLayer.addItem(marker)
    }

}
