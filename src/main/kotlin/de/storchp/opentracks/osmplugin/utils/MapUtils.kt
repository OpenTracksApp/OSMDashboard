package de.storchp.opentracks.osmplugin.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.dashboardapi.Trackpoint
import de.storchp.opentracks.osmplugin.dashboardapi.Waypoint
import de.storchp.opentracks.osmplugin.maps.MovementDirection
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol
import org.oscim.layers.marker.MarkerSymbol.HotspotPlace
import java.util.Stack
import kotlin.math.abs

/**
 * Utility class for decimating tracks at a given level of precision.
 * Derived from: [...](https://github.com/OpenTracksApp/OpenTracks/blob/23f47f10f8cd0f8b30bd6fcdccb1987008eaa07e/src/main/java/de/dennisguse/opentracks/util/LocationUtils.java)
 */
object MapUtils {
    private val TAG: String = MapUtils::class.java.getSimpleName()

    /**
     * Computes the distance on the two sphere between the point c0 and the line segment c1 to c2.
     *
     * @param c0 the first coordinate
     * @param c1 the beginning of the line segment
     * @param c2 the end of the lone segment
     * @return the distance in m (assuming spherical earth)
     */
    private fun distance(c0: GeoPoint, c1: GeoPoint, c2: GeoPoint): Double {
        if (c1 == c2) {
            return c2.sphericalDistance(c0)
        }

        val u: Double = calcU(c0, c1, c2)

        if (u <= 0) {
            return c0.sphericalDistance(c1)
        }

        if (u >= 1) {
            return c0.sphericalDistance(c2)
        }

        val sa =
            GeoPoint(c0.getLatitude() - c1.getLatitude(), c0.getLongitude() - c1.getLongitude())
        val sb = GeoPoint(
            u * (c2.getLatitude() - c1.getLatitude()),
            u * (c2.getLongitude() - c1.getLongitude())
        )

        return sa.sphericalDistance(sb)
    }

    private fun calcU(c0: GeoPoint, c1: GeoPoint, c2: GeoPoint): Double {
        val s0lat = c0.getLatitude() * UnitConversions.DEG_TO_RAD
        val s0lng = c0.getLongitude() * UnitConversions.DEG_TO_RAD
        val s1lat = c1.getLatitude() * UnitConversions.DEG_TO_RAD
        val s1lng = c1.getLongitude() * UnitConversions.DEG_TO_RAD
        val s2lat = c2.getLatitude() * UnitConversions.DEG_TO_RAD
        val s2lng = c2.getLongitude() * UnitConversions.DEG_TO_RAD

        val s2s1lat = s2lat - s1lat
        val s2s1lng = s2lng - s1lng
        return (((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
                / (s2s1lat * s2s1lat + s2s1lng * s2s1lng))
    }

    /**
     * Decimates the given trackpoints for a given zoom level.
     * This uses a Douglas-Peucker decimation algorithm.
     *
     * @param tolerance   in meters
     * @param trackpoints input
     */
    fun decimate(tolerance: Int, trackpoints: List<Trackpoint>): List<Trackpoint> {
        val n = trackpoints.size
        if (n < 1) {
            return emptyList()
        }
        var idx: Int
        var maxIdx = 0
        val stack = Stack<IntArray>()
        val dists = DoubleArray(n)
        dists[0] = 1.0
        dists[n - 1] = 1.0
        var maxDist: Double
        var dist: Double
        var current: IntArray

        if (n > 2) {
            stack.push(intArrayOf(0, (n - 1)))
            while (stack.isNotEmpty()) {
                current = stack.pop()
                maxDist = 0.0
                idx = current[0] + 1
                while (idx < current[1]) {
                    dist = distance(
                        trackpoints[idx].latLong!!,
                        trackpoints[current[0]].latLong!!,
                        trackpoints[current[1]].latLong!!
                    )
                    if (dist > maxDist) {
                        maxDist = dist
                        maxIdx = idx
                    }
                    ++idx
                }
                if (maxDist > tolerance) {
                    dists[maxIdx] = maxDist
                    stack.push(intArrayOf(current[0], maxIdx))
                    stack.push(intArrayOf(maxIdx, current[1]))
                }
            }
        }

        val decimated: List<Trackpoint> = collectTrackpoints(trackpoints, dists)
        Log.d(
            TAG,
            "Decimating $n points to ${decimated.size} w/ tolerance = $tolerance"
        )

        return decimated
    }

    private fun collectTrackpoints(trackpoints: List<Trackpoint>, dists: DoubleArray) =
        trackpoints.mapIndexedNotNull { index, trackpoint ->
            if (dists[index] != 0.0) {
                trackpoint
            } else {
                null
            }
        }

    /**
     * Checks if a given location is a valid (i.e. physically possible) location on Earth.
     * Note: The special separator locations (which have latitude = 100) will not qualify as valid.
     * Neither will locations with lat=0 and lng=0 as these are most likely "bad" measurements which often cause trouble.
     *
     * @return true if the location is a valid location.
     */
    fun isValid(latitude: Double, longitude: Double) =
        abs(latitude) <= 90 && abs(longitude) <= 180 && (latitude != 0.0 || longitude != 0.0)

    fun bearing(src: GeoPoint?, dest: GeoPoint?) =
        if (src == null || dest == null) {
            0f
        } else {
            toLocation(src).bearingTo(toLocation(dest))
        }

    fun toLocation(latLong: GeoPoint) =
        Location("").apply {
            latitude = latLong.latitude
            longitude = latLong.longitude
        }

    fun bearingInDegrees(secondToLastPos: GeoPoint?, endPos: GeoPoint?) =
        normalizeAngle(bearing(secondToLastPos, endPos))

    /**
     * Converts an angle to between 0 and 360
     *
     * @param angle the angle in degrees
     * @return the normalized angle
     */
    fun normalizeAngle(angle: Float): Float {
        var outputAngle = angle
        while (outputAngle < 0) {
            outputAngle += 360f
        }
        return outputAngle % 360
    }

    fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId)
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }


    fun getTrackColorBySpeed(
        average: Double,
        averageToMaxSpeed: Double,
        trackpoint: Trackpoint
    ): Int {
        var red = 255
        var green = 255
        if (trackpoint.speed == 0.0) {
            green = 0
        } else if (trackpoint.speed < average) {
            green = (255 * trackpoint.speed / average).toInt()
        } else {
            red = 255 - (255 * (trackpoint.speed - average) / averageToMaxSpeed).toInt()
        }
        return Color.argb(255, red, green, 0)
    }

    fun rotateWith(mapMode: MapMode, movementDirection: MovementDirection) =
        if (mapMode === MapMode.DIRECTION) {
            -1 * mapMode.getHeading(movementDirection)
        } else {
            movementDirection.currentDegrees + mapMode.getHeading(movementDirection) % 360
        }

    fun createMarkerSymbol(
        context: Context,
        markerResource: Int,
        billboard: Boolean,
        hotspot: HotspotPlace
    ) = MarkerSymbol(
        AndroidBitmap(getBitmapFromVectorDrawable(context, markerResource)),
        hotspot,
        billboard
    )

    fun createPushpinSymbol(context: Context) =
        createMarkerSymbol(
            context = context,
            markerResource = R.drawable.ic_marker_orange_pushpin_modern,
            billboard = true,
            hotspot = HotspotPlace.BOTTOM_CENTER
        )

    fun createPushpinMarker(context: Context, latLong: GeoPoint?, id: Long?) =
        MarkerItem(id, latLong.toString(), "", latLong).apply {
            marker = createPushpinSymbol(context)
        }

    fun createPauseMarker(context: Context, latLong: GeoPoint?) =
        MarkerItem(latLong.toString(), "", latLong).apply {
            marker = createMarkerSymbol(
                context = context,
                markerResource = R.drawable.ic_marker_pause_34,
                billboard = true,
                hotspot = HotspotPlace.CENTER
            )
        }

    fun createTappableMarker(context: Context, waypoint: Waypoint) =
        createPushpinMarker(context, waypoint.latLong, waypoint.id)
}
