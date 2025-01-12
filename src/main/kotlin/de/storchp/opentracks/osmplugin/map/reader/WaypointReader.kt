package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import de.storchp.opentracks.osmplugin.map.MapUtils
import de.storchp.opentracks.osmplugin.map.model.Waypoint
import org.oscim.core.GeoPoint

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

    /**
     * Reads the Waypoints from the Content Uri.
     */
    fun readWaypoints(
        resolver: ContentResolver,
        data: Uri,
        lastWaypointId: Long?
    ) = buildList {
        resolver.query(data, PROJECTION, null, null, null).use { cursor ->
            while (cursor!!.moveToNext()) {
                readWaypointFromCursor(cursor, lastWaypointId)?.let(::add)
            }
        }
    }

    private fun readWaypointFromCursor(cursor: Cursor, lastWaypointId: Long?): Waypoint? {
        val waypointId = cursor.getLong(cursor.getColumnIndexOrThrow(ID))
        if (lastWaypointId != null && lastWaypointId >= waypointId) { // skip waypoints we already have
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

}
