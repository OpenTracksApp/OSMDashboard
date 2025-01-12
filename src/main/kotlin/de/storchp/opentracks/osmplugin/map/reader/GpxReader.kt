package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import androidx.documentfile.provider.DocumentFile
import de.storchp.opentracks.osmplugin.map.MapData
import javax.xml.parsers.SAXParserFactory

class GpxReader(
    files: List<DocumentFile>,
    contentResolver: ContentResolver,
    mapData: MapData,
    updateTrackStatistics: UpdateTrackStatistics,
    updateTrackpointsDebug: UpdateTrackpointsDebug,
) : MapDataReader(mapData, updateTrackStatistics, updateTrackpointsDebug) {

    init {
        val gpxParser = GpxParser()
        files.forEach { file ->
            contentResolver.openInputStream(file.uri).use { inputStream ->
                SAXParserFactory.newInstance().newSAXParser()
                    .parse(inputStream, gpxParser)
            }
        }
        readTrackpoints(gpxParser.tracksBySegments, false, false)
        readWaypoints(gpxParser.waypoints)
        readTracks(gpxParser.tracks)
    }

}
