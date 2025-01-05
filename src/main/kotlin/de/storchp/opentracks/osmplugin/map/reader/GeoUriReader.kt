package de.storchp.opentracks.osmplugin.map.reader

import android.content.Intent
import de.storchp.opentracks.osmplugin.map.MapData
import de.storchp.opentracks.osmplugin.map.model.Waypoint
import org.oscim.core.GeoPoint
import java.net.URLDecoder
import java.util.regex.Pattern

private val NAME_PATTERN: Pattern = Pattern.compile("[+\\s]*\\((.*)\\)[+\\s]*$")
private val POSITION_PATTERN: Pattern = Pattern.compile(
    "([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)"
)
private val QUERY_POSITION_PATTERN: Pattern =
    Pattern.compile("q=([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)")

class GeoUriReader(
    intent: Intent,
    mapData: MapData,
    updateTrackStatistics: UpdateTrackStatistics,
    updateTrackpointsDebug: UpdateTrackpointsDebug,
) : MapDataReader(mapData, updateTrackStatistics, updateTrackpointsDebug) {

    init {
        require(intent.isGeoIntent())

        val waypoint = fromGeoUri(intent.data.toString())
        readWaypoints(listOf(waypoint))
        mapData.updateMapPositionAndZoomLevel(waypoint.latLong, 15)
    }

}

fun fromGeoUri(uri: String): Waypoint {
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

fun Intent.isGeoIntent() = "geo" == scheme && data != null