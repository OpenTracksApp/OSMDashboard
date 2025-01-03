package de.storchp.opentracks.osmplugin.dashboardapi

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import de.storchp.opentracks.osmplugin.map.MapData
import de.storchp.opentracks.osmplugin.map.MapUtils
import org.oscim.core.GeoPoint
import java.net.URLDecoder
import java.util.regex.Pattern

data class Waypoint(
    val id: Long = 0,
    val name: String?,
    val description: String? = null,
    val category: String? = null,
    val icon: String? = null,
    val trackId: Long = 0,
    val latLong: GeoPoint,
    val photoUrl: String? = null,
)

object WaypointReader {
    const val ID = "_id"
    const val NAME = "name" // waypoint name
    const val DESCRIPTION = "description" // waypoint description
    const val CATEGORY = "category" // waypoint category
    const val ICON = "icon" // waypoint icon
    const val TRACKID = "trackid" // track id
    const val LONGITUDE = "longitude" // longitude
    const val LATITUDE = "latitude" // latitude
    const val PHOTOURL = "photoUrl" // url for the photo

    val PROJECTION = arrayOf(
        ID,
        NAME,
        DESCRIPTION,
        CATEGORY,
        ICON,
        TRACKID,
        LATITUDE,
        LONGITUDE,
        PHOTOURL
    )
    val NAME_PATTERN: Pattern = Pattern.compile("[+\\s]*\\((.*)\\)[+\\s]*$")
    val POSITION_PATTERN: Pattern = Pattern.compile(
        "([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)"
    )
    val QUERY_POSITION_PATTERN: Pattern =
        Pattern.compile("q=([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)")

    fun fromGeoUri(uri: String): Waypoint? {
        var schemeSpecific = uri.substring(uri.indexOf(":") + 1)

        var name: String? = null
        val nameMatcher = NAME_PATTERN.matcher(schemeSpecific)
        if (nameMatcher.find()) {
            name = URLDecoder.decode(nameMatcher.group(1), "UTF-8")
            if (name != null) {
                schemeSpecific = schemeSpecific.substring(0, nameMatcher.start())
            }
        }

        var positionPart = schemeSpecific
        var queryPart = ""
        val queryStartIndex = schemeSpecific.indexOf('?')
        if (queryStartIndex != -1) {
            positionPart = schemeSpecific.substring(0, queryStartIndex)
            queryPart = schemeSpecific.substring(queryStartIndex + 1)
        }

        val positionMatcher = POSITION_PATTERN.matcher(positionPart)
        var lat = 0.0
        var lon = 0.0
        if (positionMatcher.find()) {
            lat = positionMatcher.group(1)!!.toDouble()
            lon = positionMatcher.group(2)!!.toDouble()
        }

        val queryPositionMatcher = QUERY_POSITION_PATTERN.matcher(queryPart)
        if (queryPositionMatcher.find()) {
            lat = queryPositionMatcher.group(1)!!.toDouble()
            lon = queryPositionMatcher.group(2)!!.toDouble()
        }

        return Waypoint(latLong = GeoPoint(lat, lon), name = name)
    }

    /**
     * Reads the Waypoints from the Content Uri.
     */
    fun readWaypoints(
        resolver: ContentResolver,
        data: Uri,
        lastWaypointId: Long
    ): List<Waypoint> {
        return buildList {
            resolver.query(data, PROJECTION, null, null, null).use { cursor ->
                while (cursor!!.moveToNext()) {
                    readWaypointFromCursor(cursor, lastWaypointId)?.let(::add)
                }
            }
        }
    }

    private fun readWaypointFromCursor(cursor: Cursor, lastWaypointId: Long): Waypoint? {
        val waypointId = cursor.getLong(cursor.getColumnIndexOrThrow(ID))
        if (lastWaypointId > 0 && lastWaypointId >= waypointId) { // skip waypoints we already have
            return null
        }
        val name = cursor.getString(cursor.getColumnIndexOrThrow(NAME))
        val description =
            cursor.getString(cursor.getColumnIndexOrThrow(DESCRIPTION))
        val category = cursor.getString(cursor.getColumnIndexOrThrow(CATEGORY))
        val icon = cursor.getString(cursor.getColumnIndexOrThrow(ICON))
        val trackId = cursor.getLong(cursor.getColumnIndexOrThrow(TRACKID))
        val latitude =
            cursor.getInt(cursor.getColumnIndexOrThrow(LATITUDE)) / APIConstants.LAT_LON_FACTOR
        val longitude =
            cursor.getInt(cursor.getColumnIndexOrThrow(LONGITUDE)) / APIConstants.LAT_LON_FACTOR
        val photoUrl = cursor.getString(cursor.getColumnIndexOrThrow(PHOTOURL))

        return if (MapUtils.isValid(latitude, longitude)) {
            val latLong = GeoPoint(latitude, longitude)
            Waypoint(
                waypointId,
                name,
                description,
                category,
                icon,
                trackId,
                latLong,
                photoUrl
            )
        } else {
            null
        }
    }

    fun fromGeoIntent(intent: Intent, mapData: MapData) {
        require(intent.isGeoIntent())

        fromGeoUri(intent.data.toString())
            ?.let { waypoint ->
                mapData.addWaypointMarker(waypoint)
                mapData.updateMapPositionAndZoomLevel(waypoint.latLong, 15)
            }
    }
}

fun Intent.isGeoIntent() = "geo" == scheme && data != null
