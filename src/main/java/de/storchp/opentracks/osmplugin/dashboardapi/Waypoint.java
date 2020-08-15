package de.storchp.opentracks.osmplugin.dashboardapi;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;
import java.util.List;

import de.storchp.opentracks.osmplugin.utils.MapUtils;

public class Waypoint {

    public static final String _ID = "_id";
    public static final String NAME = "name"; // waypoint name
    public static final String DESCRIPTION = "description"; // waypoint description
    public static final String CATEGORY = "category"; // waypoint category
    public static final String ICON = "icon"; // waypoint icon
    public static final String TRACKID = "trackid"; // track id
    public static final String LONGITUDE = "longitude"; // longitude
    public static final String LATITUDE = "latitude"; // latitude
    public static final String PHOTOURL = "photoUrl"; // url for the photo

    public static final String[] PROJECTION = {
            _ID,
            NAME,
            DESCRIPTION,
            CATEGORY,
            ICON,
            TRACKID,
            LATITUDE,
            LONGITUDE,
            PHOTOURL
    };

    private long id = -1L;
    private String name = "";
    private String description = "";
    private String category = "";
    private String icon = "";
    private long trackId = -1L;
    private LatLong latLong;
    private String photoUrl = "";

    public Waypoint(final long id, final String name, final String description, final String category, final String icon, final long trackId, final LatLong latLong, final String photoUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.trackId = trackId;
        this.latLong = latLong;
        this.photoUrl = photoUrl;
    }

    /**
     * Reads the Waypoints from the Content Uri.
     */
    public static List<Waypoint> readWaypoints(final ContentResolver resolver, final Uri data, final long lastWaypointId) {
        final List<Waypoint> waypoints = new ArrayList<>();
        try (final Cursor cursor = resolver.query(data, Waypoint.PROJECTION, null, null, null)) {
            while (cursor.moveToNext()) {
                final long waypointId = cursor.getLong(cursor.getColumnIndex(Waypoint._ID));
                if (lastWaypointId > 0 && lastWaypointId >= waypointId) { // skip waypoints we already have
                    continue;
                }
                final String name = cursor.getString(cursor.getColumnIndex(Waypoint.NAME));
                final String description = cursor.getString(cursor.getColumnIndex(Waypoint.DESCRIPTION));
                final String category = cursor.getString(cursor.getColumnIndex(Waypoint.CATEGORY));
                final String icon = cursor.getString(cursor.getColumnIndex(Waypoint.ICON));
                final long trackId = cursor.getLong(cursor.getColumnIndex(Waypoint.TRACKID));
                final double latitude = cursor.getInt(cursor.getColumnIndex(Waypoint.LATITUDE)) / 1E6;
                final double longitude = cursor.getInt(cursor.getColumnIndex(Waypoint.LONGITUDE)) / 1E6;
                if (MapUtils.isValid(latitude, longitude)) {
                    final LatLong latLong = new LatLong(latitude, longitude);
                    final String photoUrl = cursor.getString(cursor.getColumnIndex(Waypoint.PHOTOURL));
                    waypoints.add(new Waypoint(waypointId, name, description, category, icon, trackId, latLong, photoUrl));
                }
            }
        }
        return waypoints;
    }


    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getIcon() {
        return icon;
    }

    public long getTrackId() {
        return trackId;
    }

    public LatLong getLatLong() {
        return latLong;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}
