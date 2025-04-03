package de.storchp.opentracks.osmplugin.map

import android.content.Context
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.map.model.Trackpoint
import de.storchp.opentracks.osmplugin.map.model.Waypoint
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale


data class MapData(
    private val context: Context,
    private val map: Map,
    private val onItemGestureListener: OnItemGestureListener<MarkerInterface>,
    private val strokeWidth: Int,
    private val mapMode: MapMode,
    private val pauseMarkerSymbol: MarkerSymbol,
    private val waypointMarkerSymbol: MarkerSymbol,
    private val compassMarkerSymbol: MarkerSymbol,
    private val startMarkerSymbol: MarkerSymbol,
    private val endMarkerSymbol: MarkerSymbol,
) {

    private val polylinesLayer = GroupLayer(map)
    private val waypointsLayer: ItemizedLayer
    private val movementDirection = MovementDirection()
    private var endMarker: MarkerItem? = null
    private var startPoint: Trackpoint? = null

    private var polyline: PathLayer? = null
    var endPoint: Trackpoint? = null
        private set
    var boundingBox: BoundingBox? = null
        private set

    init {
        map.layers().add(polylinesLayer)

        waypointsLayer =
            ItemizedLayer(map, mutableListOf(), waypointMarkerSymbol, onItemGestureListener)
        map.layers().add(waypointsLayer)
    }

    fun addNewPolyline(trackColor: Int, trackpoint: Trackpoint) {
        polyline = PathLayer(map, trackColor, strokeWidth.toFloat())
        polylinesLayer.layers.add(polyline)

        if (endPoint != null) {
            polyline!!.addPoint(endPoint!!.latLong)
        } else if (startPoint != null) {
            polyline!!.addPoint(startPoint!!.latLong)
        }

        addPoint(trackpoint)
    }

    fun extendPolyline(trackColor: Int, trackpoint: Trackpoint) {
        if (polyline == null) {
            polyline = PathLayer(map, trackColor, strokeWidth.toFloat())
            polylinesLayer.layers.add(polyline)
        }

        addPoint(trackpoint)
    }

    private fun addPoint(trackpoint: Trackpoint) {
        endPoint = trackpoint
        polyline!!.addPoint(trackpoint.latLong)
        if (startPoint == null) {
            startPoint = endPoint
            MarkerItem(
                context.getString(R.string.start),
                createDescription(trackpoint),
                startPoint!!.latLong
            ).apply {
                marker = startMarkerSymbol
                waypointsLayer.addItem(this)
            }
        }
        movementDirection.updatePos(trackpoint.latLong)
        map.render()
    }

    fun resetCurrentPolyline() {
        polyline = null // reset current polyline when trackId changes
        startPoint = null
        endPoint = null
    }

    private val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(context.resources.configuration.locales[0] ?: Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    fun addPauseMarker(trackpoint: Trackpoint) {
        val marker = MapUtils.createMarker(
            latLong = trackpoint.latLong,
            title = context.getString(R.string.pause_title),
            description = createDescription(trackpoint),
            markerSymbol = pauseMarkerSymbol
        )
        waypointsLayer.addItem(marker)
    }

    private fun createDescription(trackpoint: Trackpoint): String {
        val dateTime = trackpoint.time?.let { formatter.format(it) } ?: "?"
        return context.getString(
            R.string.marker_where_when_description,
            dateTime,
            trackpoint.latLong.latitude,
            trackpoint.latLong.longitude
        )
    }

    fun setEndMarker(isRecording: Boolean) {
        endPoint?.let { endPoint ->
            synchronized(map.layers()) {
                var rotation = 0f
                var markerSymbol = endMarkerSymbol
                var title = context.getString(R.string.finish)
                val description = createDescription(endPoint)
                if (isRecording) {
                    rotation = movementDirection.currentDegrees
                    markerSymbol = compassMarkerSymbol
                    title = context.getString(R.string.current_pos)
                }
                if (endMarker == null) {
                    endMarker = MarkerItem(title, description, endPoint.latLong).apply {
                        marker = markerSymbol
                        setRotation(rotation)
                        waypointsLayer.addItem(this)
                    }
                } else {
                    endMarker?.let {
                        it.geoPoint = endPoint.latLong
                        it.setRotation(rotation)
                        it.description = description
                        waypointsLayer.populate()
                        map.render()
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
        val marker = MapUtils.createMarker(
            waypoint.id,
            waypoint.name,
            waypoint.description,
            waypoint.latLong,
            waypointMarkerSymbol
        )
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
