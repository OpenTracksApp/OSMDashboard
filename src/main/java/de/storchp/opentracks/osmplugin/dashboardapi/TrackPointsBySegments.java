package de.storchp.opentracks.osmplugin.dashboardapi;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.stream.DoubleStream;

import de.storchp.opentracks.osmplugin.utils.TrackPointsDebug;

public record TrackPointsBySegments(List<List<TrackPoint>> segments, TrackPointsDebug debug) {

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public double calcAverageSpeed() {
        return streamTrackPointsWithSpeed().average().orElse(0.0);
    }

    public double calcMaxSpeed() {
        return streamTrackPointsWithSpeed().max().orElse(0.0);
    }

    @NonNull
    private DoubleStream streamTrackPointsWithSpeed() {
        return segments.stream().flatMap(List::stream).mapToDouble(TrackPoint::getSpeed).filter(speed -> speed > 0);
    }
}
