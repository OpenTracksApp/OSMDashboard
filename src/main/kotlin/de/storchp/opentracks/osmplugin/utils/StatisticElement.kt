package de.storchp.opentracks.osmplugin.utils

import android.content.Context
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.map.TrackStatistics
import java.lang.IllegalArgumentException

enum class StatisticElement {
    CATEGORY {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return context.getString(R.string.stat_category, statistics.category)
        }
    },
    TOTAL_TIME {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatElapsedTimeHoursMinutes(
                context,
                statistics.totalTimeMillis
            )
        }
    },
    MOVING_TIME {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatElapsedTimeHoursMinutes(
                context,
                statistics.movingTimeMillis
            )
        }
    },
    DISTANCE_KM {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatDistanceInKm(context, statistics.totalDistanceMeter)
        }
    },
    DISTANCE_MI {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatDistanceInMi(context, statistics.totalDistanceMeter)
        }
    },
    SPEED_KM_H {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatSpeedInKmPerHour(
                context,
                statistics.avgMovingSpeedMeterPerSecond
            )
        }
    },
    PACE_MIN_KM {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatPaceMinPerKm(
                context,
                statistics.avgMovingSpeedMeterPerSecond
            )
        }
    },
    SPEED_MI_H {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatSpeedInMiPerHour(
                context,
                statistics.avgMovingSpeedMeterPerSecond
            )
        }
    },
    PACE_MIN_MI {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatPaceMinPerMi(
                context,
                statistics.avgMovingSpeedMeterPerSecond
            )
        }
    },
    ELEVATION_GAIN_METER {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatAltitudeChangeInMeter(
                context,
                statistics.elevationGainMeter
            )
        }
    },
    ELEVATION_GAIN_FEET {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatAltitudeChangeInFeet(
                context,
                statistics.elevationGainMeter
            )
        }
    };

    abstract fun getText(context: Context, statistics: TrackStatistics): String?

}

fun String.toStatisticElement() =
    try {
        StatisticElement.valueOf(this)
    } catch (_: IllegalArgumentException) {
        null
    }