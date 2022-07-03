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

    public Waypoint(long id, String name, String description, String category, String icon, long trackId, LatLong latLong, String photoUrl) {
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
    public static List<Waypoint> readWaypoints(ContentResolver resolver, Uri data, long lastWaypointId) {
        var waypoints = new ArrayList<Waypoint>();
        try (Cursor cursor = resolver.query(data, Waypoint.PROJECTION, null, null, null)) {
            while (cursor.moveToNext()) {
                var waypointId = cursor.getLong(cursor.getColumnIndexOrThrow(Waypoint._ID));
                if (lastWaypointId > 0 && lastWaypointId >= waypointId) { // skip waypoints we already have
                    continue;
                }
                var name = cursor.getString(cursor.getColumnIndexOrThrow(Waypoint.NAME));
                var description = cursor.getString(cursor.getColumnIndexOrThrow(Waypoint.DESCRIPTION));
                var category = cursor.getString(cursor.getColumnIndexOrThrow(Waypoint.CATEGORY));
                var icon = cursor.getString(cursor.getColumnIndexOrThrow(Waypoint.ICON));
                var trackId = cursor.getLong(cursor.getColumnIndexOrThrow(Waypoint.TRACKID));
                var latitude = cursor.getInt(cursor.getColumnIndexOrThrow(Waypoint.LATITUDE)) / LAT_LON_FACTOR;
                var longitude = cursor.getInt(cursor.getColumnIndexOrThrow(Waypoint.LONGITUDE)) / LAT_LON_FACTOR;
                if (MapUtils.isValid(latitude, longitude)) {
                    var latLong = new LatLong(latitude, longitude);
                    var photoUrl = cursor.getString(cursor.getColumnIndexOrThrow(Waypoint.PHOTOURL));
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
