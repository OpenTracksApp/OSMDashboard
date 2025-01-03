package de.storchp.opentracks.osmplugin.map

import de.storchp.opentracks.osmplugin.dashboardapi.Waypoint
import org.oscim.core.BoundingBox
import org.oscim.core.GeoPoint
import org.oscim.layers.GroupLayer
import org.oscim.layers.PathLayer
import org.oscim.layers.marker.ItemizedLayer
import org.oscim.layers.marker.ItemizedLayer.OnItemGestureListener
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol
import org.oscim.map.Map


data class MapData(
    private val map: Map,
    private val onItemGestureListener: OnItemGestureListener<MarkerInterface>,
    private val strokeWidth: Int,
    private val mapMode: MapMode,
    private val pauseMarkerSymbol: MarkerSymbol,
    private val waypointMarkerSymbol: MarkerSymbol,
    private val compassMarkerSymbol: MarkerSymbol,
) {

    private val polylinesLayer = GroupLayer(map)
    private val waypointsLayer: ItemizedLayer
    private val movementDirection = MovementDirection()
    private var endMarker: MarkerItem? = null
    private var startPos: GeoPoint? = null

    private var polyline: PathLayer? = null
    var endPos: GeoPoint? = null
        private set
    var boundingBox: BoundingBox? = null
        private set

    init {
        map.layers().add(polylinesLayer)

        waypointsLayer =
            ItemizedLayer(map, mutableListOf(), waypointMarkerSymbol, onItemGestureListener)
        map.layers().add(waypointsLayer)
    }

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
        movementDirection.updatePos(endPos)
    }

    fun resetCurrentPolyline() {
        polyline = null // reset current polyline when trackId changes
        startPos = null
        endPos = null
    }

    fun addPauseMarker(geoPoint: GeoPoint) {
        val marker = MapUtils.createMarker(latLong = geoPoint, markerSymbol = pauseMarkerSymbol)
        waypointsLayer.addItem(marker)
    }

    fun setEndMarker() {
        endPos?.let {
            synchronized(map.layers()) {
                endMarker?.let {
                    it.geoPoint = endPos
                    it.setRotation(movementDirection.currentDegrees)
                    waypointsLayer.populate()
                    map.render()
                } ?: {
                    endMarker = MarkerItem(endPos.toString(), "", endPos).apply {
                        marker = compassMarkerSymbol
                        setRotation(movementDirection.currentDegrees)
                        waypointsLayer.addItem(this)
                    }
                }
            }
        }
    }

    fun getBearing() = mapMode.getBearing(movementDirection)

    fun updateMapPositionAndRotation(myPos: GeoPoint) {
        val newPos = map.getMapPosition()
            .setPosition(myPos)
            .setBearing(getBearing())
        map.animator().animateTo(newPos)
    }

    fun updateMapPositionAndZoomLevel(myPos: GeoPoint, zoom: Int) {
        val newPos = map.getMapPosition()
            .setPosition(myPos)
            .setZoomLevel(zoom)
        map.animator().animateTo(newPos)
    }

    fun renderMap() {
        map.render()
    }

    fun addWaypointMarker(waypoint: Waypoint) {
        val marker = MapUtils.createMarker(waypoint.id, waypoint.latLong, waypointMarkerSymbol)
        waypointsLayer.addItem(marker)
    }

    fun removeLayers() {
        map.layers().remove(polylinesLayer)
        map.layers().remove(waypointsLayer)
    }

    fun createBoundingBox(latLongs: List<GeoPoint>) {
        boundingBox = BoundingBox(latLongs).extendMargin(1.2f)
    }

    fun cutPolyline() {
        polyline = null
    }

}
