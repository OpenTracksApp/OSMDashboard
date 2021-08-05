package de.storchp.opentracks.osmplugin;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.StreamRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.ZipRenderTheme;
import org.mapsforge.map.rendertheme.ZipXmlThemeResourceProvider;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

import de.storchp.opentracks.osmplugin.compass.Compass;
import de.storchp.opentracks.osmplugin.compass.SensorListener;
import de.storchp.opentracks.osmplugin.dashboardapi.APIConstants;
import de.storchp.opentracks.osmplugin.dashboardapi.TrackPoint;
import de.storchp.opentracks.osmplugin.dashboardapi.Waypoint;
import de.storchp.opentracks.osmplugin.databinding.ActivityMapsBinding;
import de.storchp.opentracks.osmplugin.maps.MovementDirection;
import de.storchp.opentracks.osmplugin.maps.RotatableMarker;
import de.storchp.opentracks.osmplugin.maps.StyleColorCreator;
import de.storchp.opentracks.osmplugin.utils.ArrowMode;
import de.storchp.opentracks.osmplugin.utils.MapMode;
import de.storchp.opentracks.osmplugin.utils.MapUtils;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public class MapsActivity extends BaseActivity implements SensorListener {

    private static final String TAG = MapsActivity.class.getSimpleName();

    public static final String EXTRA_MARKER_ID = "marker_id";

    private static final byte MAP_DEFAULT_ZOOM_LEVEL = (byte) 12;

    private static final String EXTRAS_PROTOCOL_VERSION = "PROTOCOL_VERSION";
    private static final String EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK = "EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK";
    private static final String EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_FULLSCREEN = "EXTRAS_SHOULD_FULLSCREEN";

    private boolean isOpenTracksRecordingThisTrack;

    private ActivityMapsBinding binding;

    private Layer tileLayer;
    private TileCache tileCache;

    private BoundingBox boundingBox;
    private GroupLayer polylinesLayer;
    private GroupLayer waypointsLayer;

    private long lastWaypointId = 0;
    private long lastTrackPointId = 0;
    private long lastTrackId = 0;
    private int trackColor;
    private Polyline polyline;
    private RotatableMarker endMarker = null;

    private StyleColorCreator colorCreator = null;
    private LatLong startPos;
    private LatLong endPos;
    private boolean fullscreenMode = false;

    private Compass compass;
    private MovementDirection movementDirection = new MovementDirection();
    private ArrowMode arrowMode;
    private MapMode mapMode;
    private OpenTracksContentObserver contentObserver;
    private Uri tracksUri;
    private Uri trackPointsUri;
    private Uri waypointsUri;
    private float currentMapHeading = 0;
    private int strokeWidth;
    private int protocolVersion = 1;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidGraphicFactory.createInstance(this.getApplication());

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (PreferencesUtils.getMultiThreadMapRendering()) {
            Parameters.NUMBER_OF_THREADS = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
        } else {
            Parameters.NUMBER_OF_THREADS = 1;
        }

        strokeWidth = PreferencesUtils.getStrokeWidth();
        arrowMode = PreferencesUtils.getArrowMode();
        mapMode = PreferencesUtils.getMapMode();

        compass = new Compass(this);

        setSupportActionBar(binding.toolbar.mapsToolbar);

        createMapViews();
        createTileCaches();
        createLayers();
        binding.map.mapView.setZoomLevel(MAP_DEFAULT_ZOOM_LEVEL);

        binding.map.zoomInButton.setOnClickListener(v -> binding.map.mapView.getModel().mapViewPosition.zoomIn());
        binding.map.zoomOutButton.setOnClickListener(v -> binding.map.mapView.getModel().mapViewPosition.zoomOut());
        binding.map.fullscreenButton.setOnClickListener(v -> switchFullscreen());

        // Get the intent that started this activity
        final Intent intent = getIntent();
        if (intent != null) {
            onNewIntent(intent);
        }
    }

    private void switchFullscreen() {
        showFullscreen(!fullscreenMode);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        final ArrayList<Uri> uris = intent.getParcelableArrayListExtra(APIConstants.ACTION_DASHBOARD_PAYLOAD);
        protocolVersion = intent.getIntExtra(EXTRAS_PROTOCOL_VERSION, 1);
        tracksUri = APIConstants.getTracksUri(uris);
        trackPointsUri = APIConstants.getTrackPointsUri(uris);
        waypointsUri = APIConstants.getWaypointsUri(uris);
        readTrackpoints(trackPointsUri, false, protocolVersion);
        readTracks(tracksUri, protocolVersion);
        readWaypoints(waypointsUri, false, protocolVersion);

        keepScreenOn(intent.getBooleanExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, false));
        showOnLockScreen(intent.getBooleanExtra(EXTRAS_SHOW_WHEN_LOCKED, false));
        showFullscreen(intent.getBooleanExtra(EXTRAS_SHOW_FULLSCREEN, false));
        isOpenTracksRecordingThisTrack = intent.getBooleanExtra(EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, false);
    }

    private class OpenTracksContentObserver extends ContentObserver {

        private final Uri tracksUri;
        private final Uri trackpointsUri;
        private final Uri waypointsUri;
        private final int protocolVersion;

        public OpenTracksContentObserver(final Uri tracksUri, final Uri trackpointsUri, final Uri waypointsUri, final int protocolVersion) {
            super(new Handler());
            this.tracksUri = tracksUri;
            this.trackpointsUri = trackpointsUri;
            this.waypointsUri = waypointsUri;
            this.protocolVersion = protocolVersion;
        }

        @Override
        public void onChange(final boolean selfChange, final Uri uri) {
            if (uri == null) {
                return; // nothing can be done without an uri
            }
            if (tracksUri.toString().startsWith(uri.toString())) {
                readTracks(tracksUri, protocolVersion);
            } else if (trackpointsUri.toString().startsWith(uri.toString())) {
                readTrackpoints(trackpointsUri, true, protocolVersion);
            } else if (waypointsUri.toString().startsWith(uri.toString())) {
                readWaypoints(waypointsUri, true, protocolVersion);
            }
        }
    }

    private void showFullscreen(final boolean showFullscreen) {
        this.fullscreenMode = showFullscreen;
        final View decorView = getWindow().getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();
        if (showFullscreen) {
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
            uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            binding.map.fullscreenButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_fullscreen_exit_48));
        } else {
            uiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE;
            uiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            binding.map.fullscreenButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_fullscreen_48));
        }
        binding.toolbar.mapsToolbar.setVisibility(showFullscreen ? View.GONE : View.VISIBLE);
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                && isOpenTracksRecordingThisTrack
                && PreferencesUtils.isPipEnabled()) {
            enterPictureInPictureMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        return super.onCreateOptionsMenu(menu, true);
    }

    protected void createTileCaches() {
        this.tileCache = AndroidUtil.createTileCache(this, getPersistableId(),
                this.binding.map.mapView.getModel().displayModel.getTileSize(), this.getScreenRatio(),
                this.binding.map.mapView.getModel().frameBufferModel.getOverdrawFactor(), true);
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
        binding.map.mapView.setClickable(true);
        binding.map.mapView.getModel().frameBufferModel.setOverdrawFactor(1.0d);
        binding.map.mapView.getMapScaleBar().setVisible(false);
        binding.map.mapView.setBuiltInZoomControls(false);
        binding.map.mapView.getMapZoomControls().setZoomLevelMin(getZoomLevelMin());
        binding.map.mapView.getMapZoomControls().setZoomLevelMax(getZoomLevelMax());
    }

    protected byte getZoomLevelMax() {
        return (byte) Math.min(binding.map.mapView.getModel().mapViewPosition.getZoomLevelMax(), 20);
    }

    protected byte getZoomLevelMin() {
        return binding.map.mapView.getModel().mapViewPosition.getZoomLevelMin();
    }

    /**
     * Hook to purge tile caches.
     * By default we purge every tile cache that has been added to the tileCaches list.
     */
    protected void purgeTileCaches() {
        if (tileCache != null) {
            tileCache.purge();
        }
    }

    protected XmlRenderTheme getRenderTheme() {
        final Uri mapTheme = PreferencesUtils.getMapThemeUri();
        if (mapTheme == null) {
            return InternalRenderTheme.DEFAULT;
        }
        try {
            final DocumentFile renderThemeFile = DocumentFile.fromSingleUri(getApplication(), mapTheme);
            Uri themeFileUri = renderThemeFile.getUri();
            if (renderThemeFile.getName().endsWith(".zip")) {
                final String fragment = themeFileUri.getFragment();
                if (fragment != null) {
                    themeFileUri = themeFileUri.buildUpon().fragment(null).build();
                } else {
                    throw new RuntimeException("Fragment missing, which indicates the theme inside the zip file");
                }
                return new ZipRenderTheme(fragment, new ZipXmlThemeResourceProvider(new ZipInputStream(new BufferedInputStream(getContentResolver().openInputStream(themeFileUri)))));
            }
            return new StreamRenderTheme("/assets/", getContentResolver().openInputStream(themeFileUri));
        } catch (final Exception e) {
            Log.e(TAG, "Error loading theme " + mapTheme, e);
            return InternalRenderTheme.DEFAULT;
        }
    }

    protected MapDataStore getMapFile() {
        final MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        final Set<Uri> mapFiles = PreferencesUtils.getMapUris();
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
            final TileRendererLayer rendererLayer = new TileRendererLayer(this.tileCache, mapFile,
                    this.binding.map.mapView.getModel().mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
            rendererLayer.setXmlRenderTheme(getRenderTheme());
            this.tileLayer = rendererLayer;
            binding.map.mapView.getLayerManager().getLayers().add(0, this.tileLayer);
        } else if (BuildConfig.offline) {
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_logo_color_24dp)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.no_map_configured)
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
        } else if (PreferencesUtils.getOnlineMapConsent()) {
            setOnlineTileLayer();
        } else {
            showOnlineMapConsent();
        }

        binding.map.mapView.getModel().mapViewPosition.setZoomLevelMax(getZoomLevelMax());
        binding.map.mapView.getModel().mapViewPosition.setZoomLevelMin(getZoomLevelMin());
    }

    private void setOnlineTileLayer() {
        final OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
        tileSource.setUserAgent(getString(R.string.app_name) + ":" + BuildConfig.APPLICATION_ID);
        this.tileLayer = new TileDownloadLayer(this.tileCache, this.binding.map.mapView.getModel().mapViewPosition,
                tileSource, AndroidGraphicFactory.INSTANCE);
        binding.map.mapView.getLayerManager().getLayers().add(0, this.tileLayer);

        binding.map.mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
        binding.map.mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
    }

    private void showOnlineMapConsent() {
        final SpannableString message = new SpannableString(getString(R.string.online_map_consent));
        Linkify.addLinks(message, Linkify.ALL);

        final AlertDialog dialog = new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_logo_color_24dp)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                PreferencesUtils.setOnlineMapConsent(true);
                setOnlineTileLayer();
                ((TileDownloadLayer) tileLayer).onResume();
                mapConsent.setChecked(true);
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        dialog.show();
        ((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * Android Activity life cycle method.
     */
    @Override
    protected void onDestroy() {
        binding.map.mapView.destroyAll();
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
    protected void changeMapMode(final MapMode mapMode) {
        this.mapMode = mapMode;
        rotateMap();
    }

    @Override
    protected void changeArrowMode(final ArrowMode arrowMode) {
        this.arrowMode = arrowMode;
        if (endMarker.rotateWith(arrowMode, mapMode, movementDirection, compass)) {
            binding.map.mapView.getLayerManager().redrawLayers();
        }
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
            binding.map.mapView.getLayerManager().getLayers().remove(tileLayer, true);
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
                binding.map.mapView.getLayerManager().getLayers().remove(tileLayer, true);
                this.tileLayer = null;
            }
            this.purgeTileCaches();
            createTileCaches();
            createLayers();
        }
    }

    private void readTrackpoints(final Uri data, final boolean update, final int protocolVersion) {
        Log.i(TAG, "Loading trackpoints from " + data);

        final Layers layers = binding.map.mapView.getLayerManager().getLayers();
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
            movementDirection = new MovementDirection();
        }

        final List<LatLong> latLongs = new ArrayList<>();
        final int tolerance = PreferencesUtils.getTrackSmoothingTolerance();

        try {
            final List<List<TrackPoint>> segments = TrackPoint.readTrackPointsBySegments(getContentResolver(), data, lastTrackPointId, protocolVersion);
            if (segments.isEmpty()) {
                Log.d(TAG, "No new trackpoints received");
                return;
            }
            for (List<TrackPoint> trackPoints : segments) {
                if (!update) {
                    polyline = null; // cut polyline on new segment
                    if (tolerance > 0) { // smooth track
                        trackPoints = MapUtils.decimate(tolerance, trackPoints);
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
                        polyline = addNewPolyline(trackColor);
                    }

                    endPos = trackPoint.getLatLong();
                    polyline.addPoint(endPos);
                    movementDirection.updatePos(endPos);

                    if (!update) {
                        latLongs.add(endPos);
                    }

                    if (startPos == null) {
                        startPos = endPos;
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

        Log.d(TAG, "Last trackpointId=" + lastTrackPointId);

        if (endPos != null) {
            setEndMarker(endPos);
        }

        LatLong myPos = null;
        if (update && endPos != null) {
            myPos = endPos;
        } else if (!latLongs.isEmpty()) {
            boundingBox = new BoundingBox(latLongs);
            myPos = boundingBox.getCenterPoint();
        }

        if (myPos != null) {
            if (update) {
                binding.map.mapView.getModel().mapViewPosition.animateTo(myPos);
            } else {
                binding.map.mapView.setCenter(myPos);
            }
            if (layers.indexOf(polylinesLayer) == -1 && polylinesLayer.layers.size() > 0) {
                layers.add(polylinesLayer);
            }
        } else if (!update) {
            Toast.makeText(MapsActivity.this, R.string.no_data, Toast.LENGTH_LONG).show();
        }
    }

    private void setEndMarker(final LatLong endPos) {
        synchronized (binding.map.mapView.getLayerManager().getLayers()) {
            if (endMarker != null) {
                endMarker.rotateWith(arrowMode, mapMode, movementDirection, compass);
                endMarker.setLatLong(endPos);
            } else {
                endMarker = new RotatableMarker(endPos, RotatableMarker.getBitmapFromVectorDrawable(this, R.drawable.ic_compass));
                endMarker.rotateWith(arrowMode, mapMode, movementDirection, compass);
                polylinesLayer.layers.add(endMarker);
            }
        }
        rotateMap();
    }

    private Polyline addNewPolyline(final int trackColor) {
        polyline = MapUtils.createPolyline(binding.map.mapView, trackColor, strokeWidth);
        polylinesLayer.layers.add(polyline);
        return this.polyline;
    }

    private void readWaypoints(final Uri data, final boolean update, final int protocolVersion) {
        Log.i(TAG, "Loading waypoints from " + data);

        final Layers layers = binding.map.mapView.getLayerManager().getLayers();
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
        final Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_marker_orange_pushpin_with_shadow);
        final Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        bitmap.incrementRefCount();
        return new Marker(waypoint.getLatLong(), bitmap, 0, -bitmap.getHeight() / 2) {
            @Override
            public boolean onTap(final LatLong geoPoint, final Point viewPosition,
                                 final Point tapPoint) {
                if (contains(binding.map.mapView.getMapViewProjection().toPixels(getPosition()), tapPoint)) {
                    final Intent intent = new Intent("de.dennisguse.opentracks.MarkerDetails");
                    intent.putExtra(EXTRA_MARKER_ID, waypoint.getId());
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        };
    }

    private void readTracks(final Uri data, final int protocolVersion) {
        Log.i(TAG, "Loading track from " + data);

        /* not used at the moment
        try (final Cursor cursor = getContentResolver().query(data, TracksColumn.PROJECTION, null, null, null)) {
            while (cursor.moveToNext()) {
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
        */
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.tileLayer instanceof TileDownloadLayer) {
            ((TileDownloadLayer) this.tileLayer).onResume();
        }
        compass.start(this);
    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && boundingBox != null) {
            final Dimension dimension = this.binding.map.mapView.getModel().mapViewDimension.getDimension();
            this.binding.map.mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(
                boundingBox.getCenterPoint(),
                (byte) Math.min(Math.min(LatLongUtils.zoomForBounds(
                        dimension, boundingBox, this.binding.map.mapView.getModel().displayModel.getTileSize()),
                        getZoomLevelMax()), 16)));
            boundingBox = null; // only set the zoomlevel once
        }
    }

    @Override
    public void onPictureInPictureModeChanged (final boolean isInPictureInPictureMode, final Configuration newConfig) {
        final int visibility = isInPictureInPictureMode ? View.GONE : View.VISIBLE;
        binding.toolbar.mapsToolbar.setVisibility(visibility);
        binding.map.zoomInButton.setVisibility(visibility);
        binding.map.zoomOutButton.setVisibility(visibility);
        binding.map.fullscreenButton.setVisibility(visibility);
    }

    private boolean isPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return isInPictureInPictureMode();
        }
        return false;
    }
    @Override
    protected void onPause() {
        if (!isPiPMode()) {
            if (tileLayer instanceof TileDownloadLayer) {
                ((TileDownloadLayer) tileLayer).onPause();
            }
        }
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "register content observer");
        contentObserver = new OpenTracksContentObserver(tracksUri, trackPointsUri, waypointsUri, protocolVersion);
        getContentResolver().registerContentObserver(tracksUri, false, contentObserver);
        getContentResolver().registerContentObserver(trackPointsUri, false, contentObserver);
        if (waypointsUri != null) {
            getContentResolver().registerContentObserver(waypointsUri, false, contentObserver);
        }
    }

    @Override
    protected void onStop() {
        compass.stop(this);
        if (contentObserver != null) {
            Log.d(TAG, "unregister content observer");
            getContentResolver().unregisterContentObserver(contentObserver);
            contentObserver = null;
        }
        super.onStop();
    }

    private void rotateMap() {
        final float mapHeading = mapMode.getHeading(movementDirection, compass);
        if (Math.abs(currentMapHeading - mapHeading) > 1) {
            // only rotate map if it is at lease on degree different than before
            Log.d(TAG, "CurrentMapHeading=" + currentMapHeading + ", mapHeading=" + mapHeading);
            binding.map.rotateView.setHeading(mapHeading);
            binding.map.rotateView.postInvalidate();
            currentMapHeading = mapHeading;
        }
    }

    @Override
    public boolean updateSensor() {
        if (endMarker != null) {
            if (endMarker.rotateWith(arrowMode, mapMode, movementDirection, compass)) {
                binding.map.mapView.getLayerManager().redrawLayers();
            }
        }

        rotateMap();

        return true;
    }

}
