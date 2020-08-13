package de.storchp.opentracks.osmplugin.utils;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.storchp.opentracks.osmplugin.dashboardapi.TrackPoint;

/**
 * Utility class for decimating tracks at a given level of precision.
 *
 * Derived from: https://github.com/OpenTracksApp/OpenTracks/blob/23f47f10f8cd0f8b30bd6fcdccb1987008eaa07e/src/main/java/de/dennisguse/opentracks/util/LocationUtils.java
 */
public class LatLongUtils {

    private static final String TAG = LatLongUtils.class.getSimpleName();

    private LatLongUtils() {
    }

    /**
     * Computes the distance on the two sphere between the point c0 and the line segment c1 to c2.
     *
     * @param c0 the first coordinate
     * @param c1 the beginning of the line segment
     * @param c2 the end of the lone segment
     * @return the distance in m (assuming spherical earth)
     */
    private static double distance(final LatLong c0, final LatLong c1, final LatLong c2) {
        if (c1.equals(c2)) {
            return c2.sphericalDistance(c0);
        }

        final double s0lat = c0.getLatitude() * UnitConversions.DEG_TO_RAD;
        final double s0lng = c0.getLongitude() * UnitConversions.DEG_TO_RAD;
        final double s1lat = c1.getLatitude() * UnitConversions.DEG_TO_RAD;
        final double s1lng = c1.getLongitude() * UnitConversions.DEG_TO_RAD;
        final double s2lat = c2.getLatitude() * UnitConversions.DEG_TO_RAD;
        final double s2lng = c2.getLongitude() * UnitConversions.DEG_TO_RAD;

        final double s2s1lat = s2lat - s1lat;
        final double s2s1lng = s2lng - s1lng;
        final double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
                / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);

        if (u <= 0) {
            return c0.sphericalDistance(c1);
        }

        if (u >= 1) {
            return c0.sphericalDistance(c2);
        }

        final LatLong sa = new LatLong(c0.getLatitude() - c1.getLatitude(), c0.getLongitude() - c1.getLongitude());
        final LatLong sb = new LatLong(u * (c2.getLatitude() - c1.getLatitude()), u * (c2.getLongitude() - c1.getLongitude()));

        return sa.sphericalDistance(sb);
    }

    /**
     * Decimates the given trackPoints for a given zoom level.
     * This uses a Douglas-Peucker decimation algorithm.
     *
     * @param tolerance in meters
     * @param trackPoints input
     */
    public static List<TrackPoint> decimate(final int tolerance, final List<TrackPoint> trackPoints) {
        final List<TrackPoint> decimated = new ArrayList<>();
        final int n = trackPoints.size();
        if (n < 1) {
            return null;
        }
        int idx;
        int maxIdx = 0;
        final Stack<int[]> stack = new Stack<>();
        final double[] dists = new double[n];
        dists[0] = 1;
        dists[n - 1] = 1;
        double maxDist;
        double dist;
        int[] current;

        if (n > 2) {
            final int[] stackVal = new int[]{0, (n - 1)};
            stack.push(stackVal);
            while (stack.size() > 0) {
                current = stack.pop();
                maxDist = 0;
                for (idx = current[0] + 1; idx < current[1]; ++idx) {
                    dist = LatLongUtils.distance(trackPoints.get(idx).getLatLong(), trackPoints.get(current[0]).getLatLong(), trackPoints.get(current[1]).getLatLong());
                    if (dist > maxDist) {
                        maxDist = dist;
                        maxIdx = idx;
                    }
                }
                if (maxDist > tolerance) {
                    dists[maxIdx] = maxDist;
                    final int[] stackValCurMax = {current[0], maxIdx};
                    stack.push(stackValCurMax);
                    final int[] stackValMaxCur = {maxIdx, current[1]};
                    stack.push(stackValMaxCur);
                }
            }
        }

        int i = 0;
        idx = 0;
        decimated.clear();
        for (final TrackPoint trackPoint : trackPoints) {
            if (dists[idx] != 0) {
                decimated.add(trackPoint);
                i++;
            }
            idx++;
        }
        Log.d(TAG, "Decimating " + n + " points to " + i + " w/ tolerance = " + tolerance);

        return decimated;
    }

    /**
     * Checks if a given location is a valid (i.e. physically possible) location on Earth.
     * Note: The special separator locations (which have latitude = 100) will not qualify as valid.
     * Neither will locations with lat=0 and lng=0 as these are most likely "bad" measurements which often cause trouble.
     *
     * @return true if the location is a valid location.
     */
    public static boolean isValid(final double latitude, final double longitude) {
        return Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180;
    }

    public static float bearing(final LatLong src, final LatLong dest) {
        if (src == null || dest == null) {
            return 0;
        }
        return toLocation(src).bearingTo(toLocation(dest));
    }

    public static Location toLocation(final LatLong latLong) {
        final Location location = new Location("");
        location.setLatitude(latLong.latitude);
        location.setLongitude(latLong.longitude);
        return location;
    }

}
