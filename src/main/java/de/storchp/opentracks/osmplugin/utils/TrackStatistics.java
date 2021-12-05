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

    public TrackStatistics(final List<Track> tracks) {
        if (tracks.isEmpty()) {
            return;
        }
        final Track first = tracks.get(0);
        category = first.getCategory();
        startTimeEpochMillis = first.getStartTimeEpochMillis();
        stopTimeEpochMillis = first.getStopTimeEpochMillis();
        totalDistanceMeter = first.getTotalDistanceMeter();
        totalTimeMillis = first.getTotalTimeMillis();
        movingTimeMillis = first.getMovingTimeMillis();
        avgSpeedMeterPerSecond = first.getAvgSpeedMeterPerSecond();
        avgMovingSpeedMeterPerSecond = first.getAvgMovingSpeedMeterPerSecond();
        maxSpeedMeterPerSecond = first.getMaxSpeedMeterPerSecond();
        minElevationMeter = first.getMinElevationMeter();
        maxElevationMeter = first.getMaxElevationMeter();
        elevationGainMeter = first.getElevationGainMeter();

        if (tracks.size() > 1) {
            float totalAvgSpeedMeterPerSecond = avgSpeedMeterPerSecond;
            float totalAvgMovingSpeedMeterPerSecond = avgMovingSpeedMeterPerSecond;
            for (final Track track : tracks.subList(1, tracks.size())) {
                if (!category.equals(track.getCategory())) {
                    category = "mixed";
                }
                startTimeEpochMillis = Math.min(startTimeEpochMillis, track.getStartTimeEpochMillis());
                stopTimeEpochMillis = Math.max(stopTimeEpochMillis, track.getStopTimeEpochMillis());
                totalDistanceMeter += track.getTotalDistanceMeter();
                totalTimeMillis += track.getTotalTimeMillis();
                movingTimeMillis += track.getMovingTimeMillis();
                totalAvgSpeedMeterPerSecond += track.getAvgSpeedMeterPerSecond();
                totalAvgMovingSpeedMeterPerSecond += track.getAvgMovingSpeedMeterPerSecond();
                maxSpeedMeterPerSecond = Math.max(maxSpeedMeterPerSecond, track.getMaxSpeedMeterPerSecond());
                minElevationMeter = Math.min(minElevationMeter, track.getMinElevationMeter());
                maxElevationMeter = Math.max(maxElevationMeter, track.getMaxElevationMeter());
                elevationGainMeter += track.getElevationGainMeter();
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
