package de.storchp.opentracks.osmplugin.utils;

import java.util.List;

import de.storchp.opentracks.osmplugin.dashboardapi.Track;

public class TrackStatistics {

    private String category = "unknown";
    private int startTimeEpochMillis;
    private int stopTimeEpochMillis;
    private float totalDistanceMeter;
    private int totalTimeMillis;
    private int movingTimeMillis;
    private float avgSpeedMeterPerSecond;
    private float avgMovingSpeedMeterPerSecond;
    private float maxSpeedMeterPerSecond;
    private float minElevationMeter;
    private float maxElevationMeter;
    private float elevationGainMeter;

    public TrackStatistics(List<Track> tracks) {
        if (tracks.isEmpty()) {
            return;
        }
        var first = tracks.get(0);
        category = first.category();
        startTimeEpochMillis = first.startTimeEpochMillis();
        stopTimeEpochMillis = first.stopTimeEpochMillis();
        totalDistanceMeter = first.totalDistanceMeter();
        totalTimeMillis = first.totalTimeMillis();
        movingTimeMillis = first.movingTimeMillis();
        avgSpeedMeterPerSecond = first.avgSpeedMeterPerSecond();
        avgMovingSpeedMeterPerSecond = first.avgMovingSpeedMeterPerSecond();
        maxSpeedMeterPerSecond = first.maxSpeedMeterPerSecond();
        minElevationMeter = first.minElevationMeter();
        maxElevationMeter = first.maxElevationMeter();
        elevationGainMeter = first.elevationGainMeter();

        if (tracks.size() > 1) {
            float totalAvgSpeedMeterPerSecond = avgSpeedMeterPerSecond;
            float totalAvgMovingSpeedMeterPerSecond = avgMovingSpeedMeterPerSecond;
            for (var track : tracks.subList(1, tracks.size())) {
                if (!category.equals(track.category())) {
                    category = "mixed";
                }
                startTimeEpochMillis = Math.min(startTimeEpochMillis, track.startTimeEpochMillis());
                stopTimeEpochMillis = Math.max(stopTimeEpochMillis, track.stopTimeEpochMillis());
                totalDistanceMeter += track.totalDistanceMeter();
                totalTimeMillis += track.totalTimeMillis();
                movingTimeMillis += track.movingTimeMillis();
                totalAvgSpeedMeterPerSecond += track.avgSpeedMeterPerSecond();
                totalAvgMovingSpeedMeterPerSecond += track.avgMovingSpeedMeterPerSecond();
                maxSpeedMeterPerSecond = Math.max(maxSpeedMeterPerSecond, track.maxSpeedMeterPerSecond());
                minElevationMeter = Math.min(minElevationMeter, track.minElevationMeter());
                maxElevationMeter = Math.max(maxElevationMeter, track.maxElevationMeter());
                elevationGainMeter += track.elevationGainMeter();
            }

            avgSpeedMeterPerSecond = totalAvgSpeedMeterPerSecond / tracks.size();
            avgMovingSpeedMeterPerSecond = totalAvgMovingSpeedMeterPerSecond / tracks.size();
        }
    }

    public String getCategory() {
        return category;
    }

    public int getStartTimeEpochMillis() {
        return startTimeEpochMillis;
    }

    public int getStopTimeEpochMillis() {
        return stopTimeEpochMillis;
    }

    public float getTotalDistanceMeter() {
        return totalDistanceMeter;
    }

    public int getTotalTimeMillis() {
        return totalTimeMillis;
    }

    public int getMovingTimeMillis() {
        return movingTimeMillis;
    }

    public float getAvgSpeedMeterPerSecond() {
        return avgSpeedMeterPerSecond;
    }

    public float getAvgMovingSpeedMeterPerSecond() {
        return avgMovingSpeedMeterPerSecond;
    }

    public float getMaxSpeedMeterPerSecond() {
        return maxSpeedMeterPerSecond;
    }

    public float getMinElevationMeter() {
        return minElevationMeter;
    }

    public float getMaxElevationMeter() {
        return maxElevationMeter;
    }

    public float getElevationGainMeter() {
        return elevationGainMeter;
    }
}
