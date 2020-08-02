package de.storchp.opentracks.osmplugin;


import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;

import com.caverock.androidsvg.BuildConfig;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.input.MapZoomControls;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.StreamRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.storchp.opentracks.osmplugin.dashboardapi.APIConstants;
import de.storchp.opentracks.osmplugin.dashboardapi.TrackPoint;
import de.storchp.opentracks.osmplugin.dashboardapi.TracksColumn;
import de.storchp.opentracks.osmplugin.dashboardapi.Waypoint;
import de.storchp.opentracks.osmplugin.maps.MapsforgeMapView;
import de.storchp.opentracks.osmplugin.maps.StyleColorCreator;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public class MapsActivity extends BaseActivity {

    private static final String TAG = MapsActivity.class.getSimpleName();

    public static final String EXTRA_MARKER_ID = "marker_id";

    private static final byte MAP_DEFAULT_ZOOM_LEVEL = (byte) 12;

    private static final String EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOULD_KEEP_SCREEN_ON";

    private MapsforgeMapView mapView;
    private Layer tileLayer;
    private final List<TileCache> tileCaches = new ArrayList<>();

    private BoundingBox boundingBox;
    private GroupLayer polylinesLayer;
    private GroupLayer waypointsLayer;

    private long lastWaypointId = 0;
    private long lastTrackPointId = 0;
    private long lastTrackId = 0;
    private int trackColor;
    private Polyline polyline;
    private FixedPixelCircle endMarker = null;

    private StyleColorCreator colorCreator = null;
    private LatLong startPos;
    private LatLong endPos;

    static Paint createPaint(final int color, final int strokeWidth, final Style style) {
        final Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(style);
        return paint;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidGraphicFactory.createInstance(this.getApplication());

        setContentView(R.layout.activity_maps_activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        final Toolbar toolbar = findViewById(R.id.maps_toolbar);
        setSupportActionBar(toolbar);

        createMapViews();
        createTileCaches();
        createLayers();
        mapView.setZoomLevel(MAP_DEFAULT_ZOOM_LEVEL);

        // Get the intent that started this activity
        final ArrayList<Uri> uris = getIntent().getParcelableArrayListExtra(APIConstants.ACTION_DASHBOARD_PAYLOAD);
        final Uri tracksUri = APIConstants.getTracksUri(uris);
        final Uri trackPointsUri = APIConstants.getTrackPointsUri(uris);
        final Uri waypointsUri = APIConstants.getWaypointsUri(uris);
        readTrackpoints(trackPointsUri, false);
        readTrack(tracksUri);
        readWaypoints(waypointsUri, false);

        getContentResolver().registerContentObserver(trackPointsUri, true, new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                super.onChange(selfChange);
                readTrackpoints(trackPointsUri, true);
                readTrack(tracksUri);
                readWaypoints(waypointsUri, true);
            }
        });

        keepScreenOn(getIntent().getBooleanExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, false));
        showOnLockScreen(getIntent().getBooleanExtra(EXTRAS_SHOW_WHEN_LOCKED, false));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        return super.onCreateOptionsMenu(menu, true);
    }

    protected void createTileCaches() {
        this.tileCaches.add(AndroidUtil.createTileCache(this, getPersistableId(),
                this.mapView.getModel().displayModel.getTileSize(), this.getScreenRatio(),
                this.mapView.getModel().frameBufferModel.getOverdrawFactor(), true));
    }

    /**
     * The persistable ID is used to store settings information, like the center of the last view
     * and the zoomlevel. By default the simple name of the class is used. The value is not user
     * visibile.
     *
     * @return the id that is used to save this mapview.
     */
    protected String getPersistableId() {
        return this.getClass().getSimpleName();
    }

    /**
     * Returns the relative size of a map view in relation to the screen size of the device. This
     * is used for cache size calculations.
     * By default this returns 1.0, for a full size map view.
     *
     * @return the screen ratio of the mapview
     */
    protected float getScreenRatio() {
        return 1.0f;
    }

    /**
     * Template method to create the map views.
     */
    protected void createMapViews() {
        mapView = findViewById(getMapViewId());
        mapView.setClickable(true);
        mapView.getMapScaleBar().setVisible(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getMapZoomControls().setAutoHide(true);
        mapView.getMapZoomControls().setZoomLevelMin(getZoomLevelMin());
        mapView.getMapZoomControls().setZoomLevelMax(getZoomLevelMax());
        mapView.getMapZoomControls().setZoomControlsOrientation(MapZoomControls.Orientation.VERTICAL_IN_OUT);
        mapView.getMapZoomControls().setZoomInResource(R.drawable.zoom_control_in);
        mapView.getMapZoomControls().setZoomOutResource(R.drawable.zoom_control_out);
        mapView.getMapZoomControls().setMarginHorizontal(getResources().getDimensionPixelOffset(R.dimen.controls_margin));
        mapView.getMapZoomControls().setMarginVertical(getResources().getDimensionPixelOffset(R.dimen.controls_margin));
    }

    protected int getMapViewId() {
        return R.id.mapView;
    }

    protected byte getZoomLevelMax() {
        return (byte) Math.min(mapView.getModel().mapViewPosition.getZoomLevelMax(), 20);
    }

    protected byte getZoomLevelMin() {
        return mapView.getModel().mapViewPosition.getZoomLevelMin();
    }

    /**
     * Hook to purge tile caches.
     * By default we purge every tile cache that has been added to the tileCaches list.
     */
    protected void purgeTileCaches() {
        for (final TileCache tileCache : tileCaches) {
            tileCache.purge();
        }
        tileCaches.clear();
    }

    protected XmlRenderTheme getRenderTheme() {
        final Uri mapTheme = PreferencesUtils.getMapThemeUri(this);
        if (mapTheme == null) {
            return InternalRenderTheme.DEFAULT;
        }
        try {
            final DocumentFile renderThemeFile = DocumentFile.fromSingleUri(getApplication(), mapTheme);
            return new StreamRenderTheme("/assets/", getContentResolver().openInputStream(renderThemeFile.getUri()));
        } catch (final Exception e) {
            Log.e(TAG, "Error loading theme " + mapTheme, e);
            return InternalRenderTheme.DEFAULT;
        }
    }

    protected MapDataStore getMapFile() {
        final MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        final Set<Uri> mapFiles = PreferencesUtils.getMapUris(this);
        if (mapFiles.isEmpty()) {
            return null;
        }
        int mapsCount = 0;
        for (final Uri mapUri: mapFiles) {
            try {
                final boolean documentUri = DocumentFile.isDocumentUri(this, mapUri);
                if (documentUri) {
                    final DocumentFile documentFile = DocumentFile.fromSingleUri(this, mapUri);
                    if (documentFile != null && documentFile.canRead()) {
                        final FileInputStream inputStream = (FileInputStream) getContentResolver().openInputStream(mapUri);
                        mapDataStore.addMapDataStore(new MapFile(inputStream, 0, null), false, false);
                        mapsCount++;
                    }
                }
            } catch (final Exception e) {
                Log.e(TAG, "Can't open mapFile", e);
            }
        }

        if (mapsCount == 0 && !mapFiles.isEmpty()) {
            Toast.makeText(this, R.string.error_loading_offline_map, Toast.LENGTH_LONG).show();
        }

        return mapsCount > 0 ? mapDataStore : null;
    }

    protected void createLayers() {
        final MapDataStore mapFile = getMapFile();

        if (mapFile != null) {
            final TileRendererLayer rendererLayer = new TileRendererLayer(this.tileCaches.get(0), mapFile,
                    this.mapView.getModel().mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
            rendererLayer.setXmlRenderTheme(getRenderTheme());
            this.tileLayer = rendererLayer;
            mapView.getLayerManager().getLayers().add(0, this.tileLayer);
        } else if (PreferencesUtils.getOnlineMapConsent(this)) {
            setOnlineTileLayer();
        } else {
            showOnlineMapConsent();
        }

        mapView.getModel().mapViewPosition.setZoomLevelMax(getZoomLevelMax());
        mapView.getModel().mapViewPosition.setZoomLevelMin(getZoomLevelMin());
    }

    private void setOnlineTileLayer() {
        final OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
        tileSource.setUserAgent(getString(R.string.app_name) + ":" + BuildConfig.APPLICATION_ID);
        this.tileLayer = new TileDownloadLayer(this.tileCaches.get(0), this.mapView.getModel().mapViewPosition,
                tileSource, AndroidGraphicFactory.INSTANCE);
        mapView.getLayerManager().getLayers().add(0, this.tileLayer);

        mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
        mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
    }

    private void showOnlineMapConsent() {
        final SpannableString message = new SpannableString(getString(R.string.online_map_consent));
        Linkify.addLinks(message, Linkify.ALL);

        final AlertDialog dialog = new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_logo_color_24dp)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.ok, (dialog1, which) -> {
                PreferencesUtils.setOnlineMapConsent(MapsActivity.this, true);
                setOnlineTileLayer();
                ((TileDownloadLayer) tileLayer).onResume();
                mapConsent.setChecked(true);
            })
            .setNegativeButton(R.string.cancel, null)
            .create();
        dialog.show();
        ((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * Android Activity life cycle method.
     */
    @Override
    protected void onDestroy() {
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        purgeTileCaches();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.map_info ) {
            final Intent intent = new Intent(MapsActivity.this, MainActivity.class);
            startActivityForResult(intent, 0);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onOnlineMapConsentChanged(final boolean consent) {
        if (consent) {
            if (this.tileLayer == null) {
                setOnlineTileLayer();
                ((TileDownloadLayer) this.tileLayer).onResume();
            }
        } else if (this.tileLayer != null && this.tileLayer instanceof TileDownloadLayer) {
            ((TileDownloadLayer) this.tileLayer).onPause();
            mapView.getLayerManager().getLayers().remove(tileLayer, true);
            this.tileLayer = null;
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == REQUEST_MAP_SELECTION || requestCode == REQUEST_THEME_SELECTION) {
            if (this.tileLayer != null) {
                if (this.tileLayer instanceof TileDownloadLayer) {
                    ((TileDownloadLayer) this.tileLayer).onPause();
                }
                mapView.getLayerManager().getLayers().remove(tileLayer, true);
                this.tileLayer = null;
            }
            this.purgeTileCaches();
            createTileCaches();
            createLayers();
        }
    }

    private void readTrackpoints(final Uri data, final boolean update) {
        Log.i(TAG, "Loading track from " + data);

        final Layers layers = mapView.getLayerManager().getLayers();
        if (!update) { // reset data
            if (polylinesLayer != null) {
                layers.remove(polylinesLayer);
            }
            polylinesLayer = new GroupLayer();
            lastTrackId = 0;
            lastTrackPointId = 0;
            colorCreator = new StyleColorCreator(StyleColorCreator.GOLDEN_RATIO_CONJUGATE / 2);
            trackColor = colorCreator.nextColor();
            polyline = null;
            startPos = null;
            endPos = null;
            endMarker = null;
            boundingBox = null;
        }

        final List<LatLong> latLongs = new ArrayList<>();
        final int tolerance = PreferencesUtils.getTrackSmoothingTolerance(this);

        try {
            final List<List<TrackPoint>> segments = TrackPoint.readTrackPointsBySegments(getContentResolver(), data, lastTrackPointId);
            for (List<TrackPoint> trackPoints : segments) {
                if (!update) {
                    polyline = null; // cut polyline on new segment
                    if (tolerance > 0) { // smooth track
                        trackPoints = de.storchp.opentracks.osmplugin.utils.LatLongUtils.decimate(tolerance, trackPoints);
                    }
                }
                for (final TrackPoint trackPoint : trackPoints) {
                    lastTrackPointId = trackPoint.getTrackPointId();

                    if (trackPoint.getTrackId() != lastTrackId) {
                        trackColor = colorCreator.nextColor();
                        lastTrackId = trackPoint.getTrackId();
                        polyline = null; // reset current polyline when trackId changes
                        startPos = null;
                    }

                    if (polyline == null) {
                        Log.d(TAG, "Continue new segment.");
                        polyline = newPolyline(trackColor);
                    }

                    polyline.addPoint(trackPoint.getLatLong());
                    endPos = trackPoint.getLatLong();

                    if (!update) {
                        latLongs.add(trackPoint.getLatLong());
                    }

                    if (startPos == null) {
                        startPos = trackPoint.getLatLong();
                    }
                }
            }
        } catch (final SecurityException e) {
            Log.w(TAG, "No permission to read trackpoints");
            return;
        } catch (final Exception e) {
            Log.e(TAG, "Reading trackpoints failed", e);
            return;
        }

        if (endPos != null) {
            if (endMarker == null) {
                endMarker = addEndPoint(endPos);
            } else {
                endMarker.setLatLong(endPos);
            }
        }

        LatLong myPos = null;
        if (update && endPos != null) {
            myPos = endPos;
        } else if (!latLongs.isEmpty()) {
            boundingBox = new BoundingBox(latLongs);
            myPos = boundingBox.getCenterPoint();
        }

        if (myPos != null) {
            mapView.setCenter(myPos);
            if (layers.indexOf(polylinesLayer) == -1 && polylinesLayer.layers.size() > 0) {
                layers.add(polylinesLayer);
            }
        } else if (!update) {
            Toast.makeText(MapsActivity.this, R.string.no_data, Toast.LENGTH_LONG).show();
        }
    }

    private FixedPixelCircle addEndPoint(final LatLong pos) {
        final FixedPixelCircle marker = new FixedPixelCircle(pos, 10, createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.RED), 0,
                Style.FILL), null);
        polylinesLayer.layers.add(marker);
        return marker;
    }

    private Polyline newPolyline(final int trackColor) {
        final Polyline polyline = new Polyline(createPaint(trackColor,
                (int) (8 * mapView.getModel().displayModel.getScaleFactor()),
                Style.STROKE), AndroidGraphicFactory.INSTANCE);
        polylinesLayer.layers.add(polyline);
        return polyline;
    }

    private void readWaypoints(final Uri data, final boolean update) {
        Log.i(TAG, "Loading waypoints from " + data);

        final Layers layers = mapView.getLayerManager().getLayers();
        if (waypointsLayer != null) {
            layers.remove(waypointsLayer);
        }
        if (!update) { // reset data
            lastWaypointId = 0;
        }

        try {
            final List<Waypoint> waypoints = Waypoint.readWaypoints(getContentResolver(), data, lastWaypointId);
            for (final Waypoint waypoint : waypoints) {
                final Marker marker = createTappableMarker(waypoint);
                lastWaypointId = waypoint.getId();
                if (waypointsLayer == null) {
                    waypointsLayer = new GroupLayer();
                }
                waypointsLayer.layers.add(marker);
            }
            if (waypointsLayer != null) {
                layers.add(waypointsLayer);
            }
        } catch (final SecurityException e) {
            Log.w(TAG, "No permission to read waypoints");
        } catch (final Exception e) {
            Log.e(TAG, "Reading waypoints failed", e);
        }

    }

    private Marker createTappableMarker(final Waypoint waypoint) {
        final Drawable drawable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? getDrawable(R.drawable.ic_marker_orange_pushpin_with_shadow) : getResources().getDrawable(R.drawable.ic_marker_orange_pushpin_with_shadow);
        final Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        bitmap.incrementRefCount();
        return new Marker(waypoint.getLatLong(), bitmap, 0, -bitmap.getHeight() / 2) {
            @Override
            public boolean onTap(final LatLong geoPoint, final Point viewPosition,
                                 final Point tapPoint) {
                if (contains(mapView.getMapViewProjection().toPixels(getPosition()), tapPoint)) {
                    final Intent intent = new Intent("de.dennisguse.opentracks.MarkerDetails");
                    intent.putExtra(EXTRA_MARKER_ID, waypoint.getId());
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        };
    }

    private void readTrack(final Uri data) {
        Log.i(TAG, "Loading track from " + data);

        // Contains only one row.
        try (final Cursor cursor = getContentResolver().query(data, TracksColumn.PROJECTION, null, null, null)) {
            if (cursor.moveToFirst()) {
                final long id = cursor.getLong(cursor.getColumnIndex(TracksColumn._ID));
                final String name = cursor.getString(cursor.getColumnIndex(TracksColumn.NAME));
                final String description = cursor.getString(cursor.getColumnIndex(TracksColumn.DESCRIPTION));
                final String category = cursor.getString(cursor.getColumnIndex(TracksColumn.CATEGORY));
                final int startTime = cursor.getInt(cursor.getColumnIndex(TracksColumn.STARTTIME));
                final int stopTime = cursor.getInt(cursor.getColumnIndex(TracksColumn.STOPTIME));
                final float totalDistance = cursor.getFloat(cursor.getColumnIndex(TracksColumn.TOTALDISTANCE));
                final int totalTime = cursor.getInt(cursor.getColumnIndex(TracksColumn.TOTALTIME));
                final int movingTime = cursor.getInt(cursor.getColumnIndex(TracksColumn.MOVINGTIME));
                final float avgSpeed = cursor.getFloat(cursor.getColumnIndex(TracksColumn.AVGSPEED));
                final float avgMovingSpeed = cursor.getFloat(cursor.getColumnIndex(TracksColumn.AVGMOVINGSPEED));
                final float maxSpeed = cursor.getFloat(cursor.getColumnIndex(TracksColumn.MAXSPEED));
                final float minElevation = cursor.getFloat(cursor.getColumnIndex(TracksColumn.MINELEVATION));
                final float maxElevation = cursor.getFloat(cursor.getColumnIndex(TracksColumn.MAXELEVATION));
                final float elevationGain = cursor.getFloat(cursor.getColumnIndex(TracksColumn.ELEVATIONGAIN));

                // TODO: show data on dashboard
                Log.d(TAG, "Track: " + name + ", start: " + startTime + ", end: " + stopTime);
            }
        } catch (final SecurityException e) {
            Log.w(TAG, "No permission to read track");
        } catch (final Exception e) {
            Log.e(TAG, "Reading track failed", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.tileLayer instanceof TileDownloadLayer) {
            ((TileDownloadLayer) this.tileLayer).onResume();
        }
    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && boundingBox != null) {
            final Dimension dimension = this.mapView.getModel().mapViewDimension.getDimension();
            this.mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(
                boundingBox.getCenterPoint(),
                (byte) Math.min(Math.min(LatLongUtils.zoomForBounds(
                        dimension, boundingBox, this.mapView.getModel().displayModel.getTileSize()),
                        getZoomLevelMax()), 16)));
            boundingBox = null; // only set the zoomlevel once
        }
    }

    @Override
    protected void onPause() {
        if (this.tileLayer instanceof TileDownloadLayer) {
            ((TileDownloadLayer) this.tileLayer).onPause();
        }
        super.onPause();
    }

}
