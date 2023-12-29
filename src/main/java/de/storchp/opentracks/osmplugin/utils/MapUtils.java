package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.compass.Compass;
import de.storchp.opentracks.osmplugin.dashboardapi.TrackPoint;
import de.storchp.opentracks.osmplugin.dashboardapi.Waypoint;
import de.storchp.opentracks.osmplugin.maps.MovementDirection;

/**
 * Utility class for decimating tracks at a given level of precision.
 * Derived from: <a href="https://github.com/OpenTracksApp/OpenTracks/blob/23f47f10f8cd0f8b30bd6fcdccb1987008eaa07e/src/main/java/de/dennisguse/opentracks/util/LocationUtils.java">...</a>
 */
public class MapUtils {

    private static final String TAG = MapUtils.class.getSimpleName();

    private MapUtils() {
    }

    /**
     * Computes the distance on the two sphere between the point c0 and the line segment c1 to c2.
     *
     * @param c0 the first coordinate
     * @param c1 the beginning of the line segment
     * @param c2 the end of the lone segment
     * @return the distance in m (assuming spherical earth)
     */
    private static double distance(GeoPoint c0, GeoPoint c1, GeoPoint c2) {
        if (c1.equals(c2)) {
            return c2.sphericalDistance(c0);
        }

        double s0lat = c0.getLatitude() * UnitConversions.DEG_TO_RAD;
        double s0lng = c0.getLongitude() * UnitConversions.DEG_TO_RAD;
        double s1lat = c1.getLatitude() * UnitConversions.DEG_TO_RAD;
        double s1lng = c1.getLongitude() * UnitConversions.DEG_TO_RAD;
        double s2lat = c2.getLatitude() * UnitConversions.DEG_TO_RAD;
        double s2lng = c2.getLongitude() * UnitConversions.DEG_TO_RAD;

        double s2s1lat = s2lat - s1lat;
        double s2s1lng = s2lng - s1lng;
        double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
                / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);

        if (u <= 0) {
            return c0.sphericalDistance(c1);
        }

        if (u >= 1) {
            return c0.sphericalDistance(c2);
        }

        var sa = new GeoPoint(c0.getLatitude() - c1.getLatitude(), c0.getLongitude() - c1.getLongitude());
        var sb = new GeoPoint(u * (c2.getLatitude() - c1.getLatitude()), u * (c2.getLongitude() - c1.getLongitude()));

        return sa.sphericalDistance(sb);
    }

    /**
     * Decimates the given trackPoints for a given zoom level.
     * This uses a Douglas-Peucker decimation algorithm.
     *
     * @param tolerance   in meters
     * @param trackPoints input
     */
    public static List<TrackPoint> decimate(int tolerance, List<TrackPoint> trackPoints) {
        int n = trackPoints.size();
        if (n < 1) {
            return Collections.emptyList();
        }
        int idx;
        int maxIdx = 0;
        var stack = new Stack<int[]>();
        var dists = new double[n];
        dists[0] = 1;
        dists[n - 1] = 1;
        double maxDist;
        double dist;
        int[] current;

        if (n > 2) {
            stack.push(new int[]{0, (n - 1)});
            while (stack.size() > 0) {
                current = stack.pop();
                maxDist = 0;
                for (idx = current[0] + 1; idx < current[1]; ++idx) {
                    dist = MapUtils.distance(trackPoints.get(idx).getLatLong(), trackPoints.get(current[0]).getLatLong(), trackPoints.get(current[1]).getLatLong());
                    if (dist > maxDist) {
                        maxDist = dist;
                        maxIdx = idx;
                    }
                }
                if (maxDist > tolerance) {
                    dists[maxIdx] = maxDist;
                    stack.push(new int[]{current[0], maxIdx});
                    stack.push(new int[]{maxIdx, current[1]});
                }
            }
        }

        var decimated = collectTrackPoints(trackPoints, dists);
        Log.d(TAG, "Decimating " + n + " points to " + decimated.size() + " w/ tolerance = " + tolerance);

        return decimated;
    }

    @NonNull
    private static ArrayList<TrackPoint> collectTrackPoints(List<TrackPoint> trackPoints, double[] dists) {
        int idx = 0;
        var decimated = new ArrayList<TrackPoint>();
        for (var trackPoint : trackPoints) {
            if (dists[idx] != 0) {
                decimated.add(trackPoint);
            }
            idx++;
        }
        return decimated;
    }

    /**
     * Checks if a given location is a valid (i.e. physically possible) location on Earth.
     * Note: The special separator locations (which have latitude = 100) will not qualify as valid.
     * Neither will locations with lat=0 and lng=0 as these are most likely "bad" measurements which often cause trouble.
     *
     * @return true if the location is a valid location.
     */
    public static boolean isValid(double latitude, double longitude) {
        return Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180 && (latitude != 0 || longitude != 0);
    }

    public static float bearing(GeoPoint src, GeoPoint dest) {
        if (src == null || dest == null) {
            return 0;
        }
        return toLocation(src).bearingTo(toLocation(dest));
    }

    public static Location toLocation(GeoPoint latLong) {
        var location = new Location("");
        location.setLatitude(latLong.getLatitude());
        location.setLongitude(latLong.getLongitude());
        return location;
    }

    public static float bearingInDegrees(GeoPoint secondToLastPos, GeoPoint endPos) {
        return normalizeAngle(bearing(secondToLastPos, endPos));
    }

    /**
     * Converts an angle to between 0 and 360
     *
     * @param angle the angle in degrees
     * @return the normalized angle
     */
    public static float normalizeAngle(float angle) {
        float outputAngle = angle;
        while (outputAngle < 0) {
            outputAngle += 360;
        }
        return outputAngle % 360;
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static float deltaAngle(float angle1, float angle2) {
        float delta = angle2 - angle1;
        delta += 180;
        delta -= Math.floor(delta / 360) * 360;
        delta -= 180;
        if (Math.abs(Math.abs(delta) - 180) <= Float.MIN_VALUE) {
            delta = 180f;
        }
        return delta;
    }


    public static int getTrackColorBySpeed(final double average, final double averageToMaxSpeed, final TrackPoint trackPoint) {
        double speed = trackPoint.getSpeed();
        int red = 255;
        int green = 255;
        if (speed == 0.0) {
            green = 0;
        } else if (trackPoint.getSpeed() < average) {
            green = (int) (255 * speed / average);
        } else {
            red = 255 - (int) (255 * (speed - average) / averageToMaxSpeed);
        }
        return Color.argb(255, red, green, 0);
    }

    public static float rotateWith(ArrowMode arrowMode, MapMode mapMode, MovementDirection movementDirection, Compass compass) {
        if ((arrowMode == ArrowMode.COMPASS && mapMode == MapMode.COMPASS)
                || arrowMode == ArrowMode.NORTH) {
            return 0f;
        } else if (arrowMode == ArrowMode.DIRECTION && mapMode == MapMode.DIRECTION) {
            return mapMode.getHeading(movementDirection, compass);
        } else if (arrowMode == ArrowMode.DIRECTION && mapMode == MapMode.COMPASS) {
            return arrowMode.getDegrees(movementDirection, compass);
        } else {
            return arrowMode.getDegrees(movementDirection, compass) + mapMode.getHeading(movementDirection, compass) % 360;
        }
    }

    @NonNull
    public static MarkerSymbol createMarkerSymbol(Context context, int markerResource, boolean billboard, MarkerSymbol.HotspotPlace hotspot) {
        var bitmap = new AndroidBitmap(getBitmapFromVectorDrawable(context, markerResource));
        return new MarkerSymbol(bitmap, hotspot, billboard);
    }

    @NonNull
    public static MarkerSymbol createPushpinSymbol(Context context) {
        return createMarkerSymbol(context, R.drawable.ic_marker_orange_pushpin_modern, true, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
    }

    public static MarkerItem createPushpinMarker(Context context, GeoPoint latLong, Long id) {
        var symbol = createPushpinSymbol(context);
        var marker = new MarkerItem(id, latLong.toString(), "", latLong);
        marker.setMarker(symbol);
        return marker;
    }

    public static MarkerItem createPauseMarker(Context context, GeoPoint latLong) {
        var symbol = createMarkerSymbol(context, R.drawable.ic_marker_pause_34, true, MarkerSymbol.HotspotPlace.CENTER);
        var marker = new MarkerItem(latLong.toString(), "", latLong);
        marker.setMarker(symbol);
        return marker;
    }

    public static MarkerItem createTappableMarker(final Context context, Waypoint waypoint) {
        return createPushpinMarker(context, waypoint.getLatLong(), waypoint.getId());
    }
}
