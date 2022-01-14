package de.storchp.opentracks.osmplugin.dashboardapi;

import static de.storchp.opentracks.osmplugin.dashboardapi.APIConstants.LAT_LON_FACTOR;

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

    private final long id;
    private final String name;
    private final String description;
    private final String category;
    private final String icon;
    private final long trackId;
    private final LatLong latLong;
    private final String photoUrl;

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
        final var waypoints = new ArrayList<Waypoint>();
        try (final Cursor cursor = resolver.query(data, Waypoint.PROJECTION, null, null, null)) {
            while (cursor.moveToNext()) {
                final var waypointId = cursor.getLong(cursor.getColumnIndexOrThrow(Waypoint._ID));
                if (lastWaypointId > 0 && lastWaypointId >= waypointId) { // skip waypoints we already have
                    continue;
                }
                final var name = cursor.getString(cursor.getColumnIndexOrThrow(Waypoint.NAME));
                final var description = cursor.getString(cursor.getColumnIndexOrThrow(Waypoint.DESCRIPTION));
                final var category = cursor.getString(cursor.getColumnIndexOrThrow(Waypoint.CATEGORY));
                final var icon = cursor.getString(cursor.getColumnIndexOrThrow(Waypoint.ICON));
                final var trackId = cursor.getLong(cursor.getColumnIndexOrThrow(Waypoint.TRACKID));
                final var latitude = cursor.getInt(cursor.getColumnIndexOrThrow(Waypoint.LATITUDE)) / LAT_LON_FACTOR;
                final var longitude = cursor.getInt(cursor.getColumnIndexOrThrow(Waypoint.LONGITUDE)) / LAT_LON_FACTOR;
                if (MapUtils.isValid(latitude, longitude)) {
                    final var latLong = new LatLong(latitude, longitude);
                    final var photoUrl = cursor.getString(cursor.getColumnIndexOrThrow(Waypoint.PHOTOURL));
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
