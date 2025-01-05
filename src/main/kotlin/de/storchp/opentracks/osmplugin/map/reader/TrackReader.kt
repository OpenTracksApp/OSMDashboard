package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import de.storchp.opentracks.osmplugin.map.model.Track
import java.lang.Exception

object TrackReader {
    private val TAG: String = Track::class.java.getSimpleName()

    const val ID = "_id"
    const val NAME = "name" // track name
    const val DESCRIPTION = "description" // track description
    const val CATEGORY = "category" // track activity type
    const val STARTTIME = "starttime" // track start time
    const val STOPTIME = "stoptime" // track stop time
    const val TOTALDISTANCE = "totaldistance" // total distance
    const val TOTALTIME = "totaltime" // total time
    const val MOVINGTIME = "movingtime" // moving time
    const val AVGSPEED = "avgspeed" // average speed
    const val AVGMOVINGSPEED = "avgmovingspeed" // average moving speed
    const val MAXSPEED = "maxspeed" // maximum speed
    const val MINELEVATION = "minelevation" // minimum elevation
    const val MAXELEVATION = "maxelevation" // maximum elevation
    const val ELEVATIONGAIN = "elevationgain" // elevation gain

    val PROJECTION = arrayOf(
        ID,
        NAME,
        DESCRIPTION,
        CATEGORY,
        STARTTIME,
        STOPTIME,
        TOTALDISTANCE,
        TOTALTIME,
        MOVINGTIME,
        AVGSPEED,
        AVGMOVINGSPEED,
        MAXSPEED,
        MINELEVATION,
        MAXELEVATION,
        ELEVATIONGAIN
    )

    /**
     * Reads the Tracks from the Content Uri
     */
    fun readTracks(resolver: ContentResolver, data: Uri): List<Track> {
        Log.i(TAG, "Loading track(s) from $data")

        return buildList {
            try {
                resolver.query(data, PROJECTION, null, null, null).use { cursor ->
                    while (cursor!!.moveToNext()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(ID))
                        val trackname = cursor.getString(cursor.getColumnIndexOrThrow(NAME))
                        val description =
                            cursor.getString(cursor.getColumnIndexOrThrow(DESCRIPTION))
                        val category = cursor.getString(cursor.getColumnIndexOrThrow(CATEGORY))
                        val startTimeEpochMillis =
                            cursor.getLong(cursor.getColumnIndexOrThrow(STARTTIME))
                        val stopTimeEpochMillis =
                            cursor.getLong(cursor.getColumnIndexOrThrow(STOPTIME))
                        val totalDistanceMeter =
                            cursor.getDouble(cursor.getColumnIndexOrThrow(TOTALDISTANCE))
                        val totalTimeMillis =
                            cursor.getLong(cursor.getColumnIndexOrThrow(TOTALTIME))
                        val movingTimeMillis =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MOVINGTIME))
                        val avgSpeedMeterPerSecond =
                            cursor.getDouble(cursor.getColumnIndexOrThrow(AVGSPEED))
                        val avgMovingSpeedMeterPerSecond =
                            cursor.getDouble(cursor.getColumnIndexOrThrow(AVGMOVINGSPEED))
                        val maxSpeedMeterPerSecond =
                            cursor.getDouble(cursor.getColumnIndexOrThrow(MAXSPEED))
                        val minElevationMeter =
                            cursor.getDouble(cursor.getColumnIndexOrThrow(MINELEVATION))
                        val maxElevationMeter =
                            cursor.getDouble(cursor.getColumnIndexOrThrow(MAXELEVATION))
                        val elevationGainMeter =
                            cursor.getDouble(cursor.getColumnIndexOrThrow(ELEVATIONGAIN))

                        add(
                            Track(
                                id = id,
                                trackname = trackname,
                                description = description,
                                category = category,
                                startTimeEpochMillis = startTimeEpochMillis,
                                stopTimeEpochMillis = stopTimeEpochMillis,
                                totalDistanceMeter = totalDistanceMeter,
                                totalTimeMillis = totalTimeMillis,
                                movingTimeMillis = movingTimeMillis,
                                avgSpeedMeterPerSecond = avgSpeedMeterPerSecond,
                                avgMovingSpeedMeterPerSecond = avgMovingSpeedMeterPerSecond,
                                maxSpeedMeterPerSecond = maxSpeedMeterPerSecond,
                                minElevationMeter = minElevationMeter,
                                maxElevationMeter = maxElevationMeter,
                                elevationGainMeter = elevationGainMeter
                            )
                        )
                    }
                }
            } catch (_: SecurityException) {
                Log.w(TAG, "No permission to read track")
            } catch (e: Exception) {
                Log.e(TAG, "Reading track failed", e)
            }
        }
    }
}