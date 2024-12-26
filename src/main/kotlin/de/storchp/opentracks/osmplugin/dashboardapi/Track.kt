package de.storchp.opentracks.osmplugin.dashboardapi

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import java.lang.Exception

data class Track(
    val id: Long,
    val trackname: String?,
    val description: String?,
    val category: String?,
    val startTimeEpochMillis: Int,
    val stopTimeEpochMillis: Int,
    val totalDistanceMeter: Float,
    val totalTimeMillis: Int,
    val movingTimeMillis: Int,
    val avgSpeedMeterPerSecond: Float,
    val avgMovingSpeedMeterPerSecond: Float,
    val maxSpeedMeterPerSecond: Float,
    val minElevationMeter: Float,
    val maxElevationMeter: Float,
    val elevationGainMeter: Float
)

object TrackReader {
    private val TAG: String = Track::class.java.getSimpleName()

    const val _ID = "_id"
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
        _ID,
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
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(_ID))
                        val trackname = cursor.getString(cursor.getColumnIndexOrThrow(NAME))
                        val description =
                            cursor.getString(cursor.getColumnIndexOrThrow(DESCRIPTION))
                        val category = cursor.getString(cursor.getColumnIndexOrThrow(CATEGORY))
                        val startTimeEpochMillis =
                            cursor.getInt(cursor.getColumnIndexOrThrow(STARTTIME))
                        val stopTimeEpochMillis =
                            cursor.getInt(cursor.getColumnIndexOrThrow(STOPTIME))
                        val totalDistanceMeter =
                            cursor.getFloat(cursor.getColumnIndexOrThrow(TOTALDISTANCE))
                        val totalTimeMillis =
                            cursor.getInt(cursor.getColumnIndexOrThrow(TOTALTIME))
                        val movingTimeMillis =
                            cursor.getInt(cursor.getColumnIndexOrThrow(MOVINGTIME))
                        val avgSpeedMeterPerSecond =
                            cursor.getFloat(cursor.getColumnIndexOrThrow(AVGSPEED))
                        val avgMovingSpeedMeterPerSecond =
                            cursor.getFloat(cursor.getColumnIndexOrThrow(AVGMOVINGSPEED))
                        val maxSpeedMeterPerSecond =
                            cursor.getFloat(cursor.getColumnIndexOrThrow(MAXSPEED))
                        val minElevationMeter =
                            cursor.getFloat(cursor.getColumnIndexOrThrow(MINELEVATION))
                        val maxElevationMeter =
                            cursor.getFloat(cursor.getColumnIndexOrThrow(MAXELEVATION))
                        val elevationGainMeter =
                            cursor.getFloat(cursor.getColumnIndexOrThrow(ELEVATIONGAIN))

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

