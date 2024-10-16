package de.storchp.opentracks.osmplugin.dashboardapi;

import static de.storchp.opentracks.osmplugin.dashboardapi.APIConstants.LAT_LON_FACTOR;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.oscim.core.GeoPoint;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import de.storchp.opentracks.osmplugin.utils.MapUtils;

/**
 * @noinspection unused
 */
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
    public static final Pattern NAME_PATTERN = Pattern.compile("[+\\s]*\\((.*)\\)[+\\s]*$");
    public static final Pattern POSITION_PATTERN = Pattern.compile(
            "([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)");
    public static final Pattern QUERY_POSITION_PATTERN = Pattern.compile("q=([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)");

    private long id;
    private final String name;
    private String description;
    private String category;
    private String icon;
    private long trackId;
    private final GeoPoint latLong;
    private String photoUrl;

    public Waypoint(long id, String name, String description, String category, String icon, long trackId, GeoPoint latLong, String photoUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.trackId = trackId;
        this.latLong = latLong;
        this.photoUrl = photoUrl;
    }

    public Waypoint(final GeoPoint latLong, final String name) {
        this.latLong = latLong;
        this.name = name;
    }

    public static Optional<Waypoint> fromGeoUri(String uri) {
        if (uri == null) {
            return Optional.empty();
        }

        var schemeSpecific = uri.substring(uri.indexOf(":") + 1);

        String name = null;
        var nameMatcher = NAME_PATTERN.matcher(schemeSpecific);
        if (nameMatcher.find()) {
            name = URLDecoder.decode(nameMatcher.group(1));
            if (name != null) {
                schemeSpecific = schemeSpecific.substring(0, nameMatcher.start());
            }
        }

        var positionPart = schemeSpecific;
        var queryPart = "";
        int queryStartIndex = schemeSpecific.indexOf('?');
        if (queryStartIndex != -1) {
            positionPart = schemeSpecific.substring(0, queryStartIndex);
            queryPart = schemeSpecific.substring(queryStartIndex + 1);
        }

        var positionMatcher = POSITION_PATTERN.matcher(positionPart);
        double lat = 0.0;
        double lon = 0.0;
        if (positionMatcher.find()) {
            lat = Double.parseDouble(positionMatcher.group(1));
            lon = Double.parseDouble(positionMatcher.group(2));
        }

        var queryPositionMatcher = QUERY_POSITION_PATTERN.matcher(queryPart);
        if (queryPositionMatcher.find()) {
            lat = Double.parseDouble(queryPositionMatcher.group(1));
            lon = Double.parseDouble(queryPositionMatcher.group(2));
        }

        return Optional.of(new Waypoint(new GeoPoint(lat, lon), name));
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
                    var latLong = new GeoPoint(latitude, longitude);
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

    public GeoPoint getLatLong() {
        return latLong;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}
