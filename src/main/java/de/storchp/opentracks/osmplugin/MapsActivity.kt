package de.storchp.opentracks.osmplugin;


import static android.util.TypedValue.COMPLEX_UNIT_PT;
import static java.util.Comparator.comparingInt;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import org.oscim.android.MapPreferences;
import org.oscim.backend.AssetAdapter;
import org.oscim.backend.CanvasAdapter;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.GroupLayer;
import org.oscim.layers.Layer;
import org.oscim.layers.PathLayer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.ImperialUnitAdapter;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.scalebar.MetricUnitAdapter;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.StreamRenderTheme;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.ZipRenderTheme;
import org.oscim.theme.ZipXmlThemeResourceProvider;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.bitmap.DefaultSources;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipInputStream;

import de.storchp.opentracks.osmplugin.dashboardapi.APIConstants;
import de.storchp.opentracks.osmplugin.dashboardapi.Track;
import de.storchp.opentracks.osmplugin.dashboardapi.TrackPoint;
import de.storchp.opentracks.osmplugin.dashboardapi.Waypoint;
import de.storchp.opentracks.osmplugin.databinding.ActivityMapsBinding;
import de.storchp.opentracks.osmplugin.maps.MovementDirection;
import de.storchp.opentracks.osmplugin.maps.StyleColorCreator;
import de.storchp.opentracks.osmplugin.utils.MapMode;
import de.storchp.opentracks.osmplugin.utils.MapUtils;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;
import de.storchp.opentracks.osmplugin.utils.StatisticElement;
import de.storchp.opentracks.osmplugin.utils.TrackColorMode;
import de.storchp.opentracks.osmplugin.utils.TrackPointsDebug;
import de.storchp.opentracks.osmplugin.utils.TrackStatistics;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class MapsActivity extends BaseActivity implements ItemizedLayer.OnItemGestureListener<MarkerInterface> {

    private static final String TAG = MapsActivity.class.getSimpleName();
    public static final String EXTRA_MARKER_ID = "marker_id";
    public static final String EXTRA_TRACK_ID = "track_id";
    public static final String EXTRA_LOCATION = "location";
    private static final int MAP_DEFAULT_ZOOM_LEVEL = 12;
    private static final String EXTRAS_PROTOCOL_VERSION = "PROTOCOL_VERSION";
    private static final String EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK = "EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK";
    private static final String EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_FULLSCREEN = "EXTRAS_SHOULD_FULLSCREEN";
    private boolean isOpenTracksRecordingThisTrack;
    private ActivityMapsBinding binding;
    private Map map;
    private MapPreferences mapPreferences;
    private IRenderTheme renderTheme;
    private BoundingBox boundingBox;
    private GroupLayer polylinesLayer;
    private ItemizedLayer waypointsLayer;
    private long lastWaypointId = 0;
    private long lastTrackPointId = 0;
    private long lastTrackId = 0;
    private int trackColor;
    private PathLayer polyline;
    private MarkerItem endMarker = null;
    private StyleColorCreator colorCreator = null;
    private GeoPoint startPos;
    private GeoPoint endPos;
    private boolean fullscreenMode = false;
    private MovementDirection movementDirection = new MovementDirection();
    private MapMode mapMode;
    private OpenTracksContentObserver contentObserver;
    private Uri tracksUri;
    private Uri trackPointsUri;
    private Uri waypointsUri;
    private int strokeWidth;
    private int protocolVersion = 1;
    private TrackPointsDebug trackPointsDebug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        strokeWidth = PreferencesUtils.getStrokeWidth();
        mapMode = PreferencesUtils.getMapMode();

        map = binding.map.mapView.map();
        mapPreferences = new MapPreferences(MapsActivity.class.getName(), this);

        setSupportActionBar(binding.toolbar.mapsToolbar);

        createMapViews();
        createLayers();
        map.getMapPosition().setZoomLevel(MAP_DEFAULT_ZOOM_LEVEL);

        binding.map.fullscreenButton.setOnClickListener(v -> switchFullscreen());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            public void handleOnBackPressed() {
                navigateUp();
            }
        });

        // Get the intent that started this activity
        var intent = getIntent();
        if (intent != null) {
            onNewIntent(intent);
        }
    }

    private class MapEventsReceiver extends Layer implements GestureListener {
        MapEventsReceiver(Map map) {
            super(map);
        }

        @Override
        public boolean onGesture(Gesture g, MotionEvent e) {
            if (g instanceof Gesture.LongPress && lastTrackId > 0) {
                new AlertDialog.Builder(MapsActivity.this)
                        .setIcon(R.drawable.ic_logo_color_24dp)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.add_marker_to_open_tracks)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> startOpenTracksToAddNewMarker(e))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show();
            }

            return false;
        }
    }

    private void startOpenTracksToAddNewMarker(MotionEvent event) {
        try {
            var geoPoint = map.viewport().fromScreenPoint(event.getX(), event.getY());
            var intent = new Intent("de.dennisguse.opentracks.CreateMarker");
            intent.putExtra(EXTRA_TRACK_ID, lastTrackId);
            intent.putExtra(EXTRA_LOCATION, MapUtils.toLocation(geoPoint));
            startActivity(intent);
        } catch (Exception ex) {
            Log.e(TAG, "Can't send trackpoint to OpenTracks", ex);
        }
    }

    private void switchFullscreen() {
        showFullscreen(!fullscreenMode);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        resetMapData();

        if (APIConstants.ACTION_DASHBOARD.equals(intent.getAction())) {
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(APIConstants.ACTION_DASHBOARD_PAYLOAD);
            protocolVersion = intent.getIntExtra(EXTRAS_PROTOCOL_VERSION, 1);
            tracksUri = APIConstants.getTracksUri(uris);
            trackPointsUri = APIConstants.getTrackPointsUri(uris);
            waypointsUri = APIConstants.getWaypointsUri(uris);
            keepScreenOn(intent.getBooleanExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, false));
            showOnLockScreen(intent.getBooleanExtra(EXTRAS_SHOW_WHEN_LOCKED, false));
            showFullscreen(intent.getBooleanExtra(EXTRAS_SHOW_FULLSCREEN, false));
            isOpenTracksRecordingThisTrack = intent.getBooleanExtra(EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, false);

            readTrackpoints(trackPointsUri, false, protocolVersion);
            readTracks(tracksUri);
            readWaypoints(waypointsUri);
        } else if ("geo".equals(intent.getScheme()) && intent.getData() != null) {
            Waypoint.fromGeoUri(intent.getData().toString()).ifPresent(waypoint -> {
                var marker = MapUtils.createTappableMarker(this, waypoint);
                waypointsLayer.addItem(marker);
                var pos = map.getMapPosition()
                        .setPosition(waypoint.getLatLong())
                        .setZoomLevel(15);
                map.animator().animateTo(pos);
            });
        }
    }

    private class OpenTracksContentObserver extends ContentObserver {

        private final Uri tracksUri;
        private final Uri trackpointsUri;
        private final Uri waypointsUri;
        private final int protocolVersion;

        public OpenTracksContentObserver(Uri tracksUri, Uri trackpointsUri, Uri waypointsUri, int protocolVersion) {
            super(new Handler());
            this.tracksUri = tracksUri;
            this.trackpointsUri = trackpointsUri;
            this.waypointsUri = waypointsUri;
            this.protocolVersion = protocolVersion;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                return; // nothing can be done without an uri
            }
            if (tracksUri.toString().startsWith(uri.toString())) {
                readTracks(tracksUri);
            } else if (trackpointsUri.toString().startsWith(uri.toString())) {
                readTrackpoints(trackpointsUri, true, protocolVersion);
            } else if (waypointsUri.toString().startsWith(uri.toString())) {
                readWaypoints(waypointsUri);
            }
        }
    }

    private void showFullscreen(boolean showFullscreen) {
        this.fullscreenMode = showFullscreen;
        var decorView = getWindow().getDecorView();
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

    public void navigateUp() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                && isOpenTracksRecordingThisTrack
                && PreferencesUtils.isPipEnabled()) {
            enterPictureInPictureMode();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu, true);
        return true;
    }

    /**
     * Template method to create the map views.
     */
    protected void createMapViews() {
        binding.map.mapView.setClickable(true);
    }

    protected ThemeFile getRenderTheme() {
        Uri mapTheme = PreferencesUtils.getMapThemeUri();
        if (mapTheme == null) {
            return null;
        }
        try {
            if ("file".equals(mapTheme.getScheme())) {
                var themeFile = new File(mapTheme.getPath());
                if (themeFile.exists() && themeFile.getName().endsWith(".zip")) {
                    var themeFileUri = Uri.fromFile(themeFile);
                    var fragment = mapTheme.getFragment();
                    if (fragment != null) {
                        themeFileUri = themeFileUri.buildUpon().fragment(null).build();
                    } else {
                        throw new RuntimeException("Fragment missing, which indicates the theme inside the zip file");
                    }
                    return new ZipRenderTheme(fragment, new ZipXmlThemeResourceProvider(new ZipInputStream(new BufferedInputStream(getContentResolver().openInputStream(themeFileUri)))));
                }
                return new StreamRenderTheme("/assets/", new FileInputStream(themeFile));
            } else {
                var renderThemeFile = DocumentFile.fromSingleUri(getApplication(), mapTheme);
                assert renderThemeFile != null;
                var themeFileUri = renderThemeFile.getUri();
                if (Objects.requireNonNull(renderThemeFile.getName(), "Theme files must have a name").endsWith(".zip")) {
                    var fragment = themeFileUri.getFragment();
                    if (fragment != null) {
                        themeFileUri = themeFileUri.buildUpon().fragment(null).build();
                    } else {
                        throw new RuntimeException("Fragment missing, which indicates the theme inside the zip file");
                    }
                    return new ZipRenderTheme(fragment, new ZipXmlThemeResourceProvider(new ZipInputStream(new BufferedInputStream(getContentResolver().openInputStream(themeFileUri)))));
                }
                return new StreamRenderTheme("/assets/", getContentResolver().openInputStream(themeFileUri));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading theme " + mapTheme, e);
        }
        return null;
    }

    protected MultiMapFileTileSource getMapFile() {
        var tileSource = new MultiMapFileTileSource();
        var mapFiles = PreferencesUtils.getMapUris();
        if (mapFiles.isEmpty()) {
            return null;
        }
        var mapsCount = new AtomicInteger(0);
        mapFiles.stream()
                .filter(uri -> DocumentFile.isDocumentUri(this, uri))
                .map(uri -> DocumentFile.fromSingleUri(this, uri))
                .filter(documentFile -> documentFile != null && documentFile.canRead())
                .forEach(documentFile -> readMapFile(tileSource, mapsCount, documentFile));

        mapFiles.stream()
                .filter(uri -> "file".equals(uri.getScheme()))
                .map(uri -> new File(uri.getPath()))
                .filter(File::exists)
                .forEach(file -> readMapFile(tileSource, mapsCount, file));

        if (mapsCount.get() == 0 && !mapFiles.isEmpty()) {
            Toast.makeText(this, R.string.error_loading_offline_map, Toast.LENGTH_LONG).show();
        }

        return mapsCount.get() > 0 ? tileSource : null;
    }

    private void readMapFile(MultiMapFileTileSource mapDataStore, AtomicInteger mapsCount, DocumentFile documentFile) {
        try {
            var inputStream = (FileInputStream) getContentResolver().openInputStream(documentFile.getUri());
            var tileSource = new MapFileTileSource();
            tileSource.setMapFileInputStream(inputStream);
            mapDataStore.add(tileSource);
            mapsCount.getAndIncrement();
        } catch (Exception e) {
            Log.e(TAG, "Can't open mapFile", e);
        }
    }

    private void readMapFile(MultiMapFileTileSource mapDataStore, AtomicInteger mapsCount, File file) {
        try {
            var tileSource = new MapFileTileSource();
            tileSource.setMapFile(file.getPath());
            mapDataStore.add(tileSource);
            mapsCount.getAndIncrement();
        } catch (Exception e) {
            Log.e(TAG, "Can't open mapFile", e);
        }
    }

    protected void loadTheme() {
        if (renderTheme != null) {
            renderTheme.dispose();
        }
        renderTheme = map.setTheme(new StreamRenderTheme("", AssetAdapter.readFileAsStream("vtm/vtmstyle.xml")));
    }

    protected void createLayers() {
        var mapFile = getMapFile();
        map.layers().add(new MapEventsReceiver(map));

        if (mapFile != null) {
            var tileLayer = map.setBaseMap(mapFile);
            loadTheme();

            map.layers().add(new BuildingLayer(map, tileLayer));
            map.layers().add(new LabelLayer(map, tileLayer));

            var mapScaleBar = new DefaultMapScaleBar(map);
            mapScaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.BOTH);
            mapScaleBar.setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE);
            mapScaleBar.setSecondaryDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE);
            mapScaleBar.setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_LEFT);

            var mapScaleBarLayer = new MapScaleBarLayer(map, mapScaleBar);
            var renderer = mapScaleBarLayer.getRenderer();
            renderer.setPosition(GLViewport.Position.BOTTOM_LEFT);
            renderer.setOffset(5 * CanvasAdapter.getScale(), 0);
            map.layers().add(mapScaleBarLayer);

            var themeFile = getRenderTheme();
            if (themeFile != null) {
                map.setTheme(themeFile);
            }
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
    }

    private void setOnlineTileLayer() {
        var tileSource = DefaultSources.OPENSTREETMAP.build();
        var builder = new OkHttpClient.Builder();
        var cacheDirectory = new File(getExternalCacheDir(), "tiles");
        int cacheSize = 10 * 1024 * 1024; // 10 MB
        var cache = new Cache(cacheDirectory, cacheSize);
        builder.cache(cache);

        tileSource.setHttpEngine(new OkHttpEngine.OkHttpFactory(builder));
        tileSource.setHttpRequestHeaders(Collections.singletonMap("User-Agent", getString(R.string.app_name) + ":" + BuildConfig.APPLICATION_ID));

        BitmapTileLayer bitmapLayer = new BitmapTileLayer(map, tileSource);
        map.layers().add(bitmapLayer);
    }

    private void showOnlineMapConsent() {
        var message = new SpannableString(getString(R.string.online_map_consent));
        Linkify.addLinks(message, Linkify.ALL);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    PreferencesUtils.setOnlineMapConsent(true);
                    MapsActivity.this.recreate();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
        ((TextView) Objects.requireNonNull(dialog.findViewById(android.R.id.message),
                "An AlertDialog must have a TextView with id.message"))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * Android Activity life cycle method.
     */
    @Override
    protected void onDestroy() {
        binding.map.mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.map_info) {
            var intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void readTrackpoints(Uri data, boolean update, int protocolVersion) {
        Log.i(TAG, "Loading trackpoints from " + data);

        synchronized (map.layers()) {
            var showPauseMarkers = PreferencesUtils.isShowPauseMarkers();
            var latLongs = new ArrayList<GeoPoint>();
            int tolerance = PreferencesUtils.getTrackSmoothingTolerance();

            try {
                var trackpointsBySegments = TrackPoint.readTrackPointsBySegments(getContentResolver(), data, lastTrackPointId, protocolVersion);
                if (trackpointsBySegments.isEmpty()) {
                    Log.d(TAG, "No new trackpoints received");
                    return;
                }

                double average = trackpointsBySegments.calcAverageSpeed();
                double maxSpeed = trackpointsBySegments.calcMaxSpeed();
                double averageToMaxSpeed = maxSpeed - average;
                var trackColorMode = PreferencesUtils.getTrackColorMode();
                if (isOpenTracksRecordingThisTrack && !trackColorMode.isSupportsLiveTrack()) {
                    trackColorMode = TrackColorMode.DEFAULT;
                }

                for (var trackPoints : trackpointsBySegments.segments()) {
                    if (!update) {
                        polyline = null; // cut polyline on new segment
                        if (tolerance > 0) { // smooth track
                            trackPoints = MapUtils.decimate(tolerance, trackPoints);
                        }
                    }
                    for (var trackPoint : trackPoints) {
                        lastTrackPointId = trackPoint.getTrackPointId();

                        if (trackPoint.getTrackId() != lastTrackId) {
                            if (trackColorMode == TrackColorMode.BY_TRACK) {
                                trackColor = colorCreator.nextColor();
                            }
                            lastTrackId = trackPoint.getTrackId();
                            polyline = null; // reset current polyline when trackId changes
                            startPos = null;
                            endPos = null;
                        }

                        if (trackColorMode == TrackColorMode.BY_SPEED) {
                            trackColor = MapUtils.getTrackColorBySpeed(average, averageToMaxSpeed, trackPoint);
                            polyline = addNewPolyline(trackColor);
                            if (endPos != null) {
                                polyline.addPoint(endPos);
                            } else if (startPos != null) {
                                polyline.addPoint(startPos);
                            }
                        } else {
                            if (polyline == null) {
                                Log.d(TAG, "Continue new segment.");
                                polyline = addNewPolyline(trackColor);
                            }
                        }

                        endPos = trackPoint.getLatLong();
                        polyline.addPoint(endPos);
                        movementDirection.updatePos(endPos);

                        if (trackPoint.isPause() && showPauseMarkers) {
                            var marker = MapUtils.createPauseMarker(this, trackPoint.getLatLong());
                            waypointsLayer.addItem(marker);
                        }

                        if (!update) {
                            latLongs.add(endPos);
                        }

                        if (startPos == null) {
                            startPos = endPos;
                        }
                    }
                    trackpointsBySegments.debug().trackpointsDrawn += trackPoints.size();
                }
                trackPointsDebug.add(trackpointsBySegments.debug());
            } catch (SecurityException e) {
                Toast.makeText(MapsActivity.this, getString(R.string.error_reading_trackpoints, e.getMessage()), Toast.LENGTH_LONG).show();
                return;
            } catch (Exception e) {
                throw new RuntimeException("Error reading trackpoints", e);
            }

            Log.d(TAG, "Last trackpointId=" + lastTrackPointId);

            if (endPos != null) {
                setEndMarker(endPos);
            }

            GeoPoint myPos = null;
            if (update && endPos != null) {
                myPos = endPos;
                map.render();
            } else if (!latLongs.isEmpty()) {
                boundingBox = new BoundingBox(latLongs).extendMargin(1.2f);
                myPos = boundingBox.getCenterPoint();
            }

            if (myPos != null) {
                updateMapPositionAndRotation(myPos);
            }
            updateDebugTrackPoints();
        }
    }

    private void resetMapData() {
        unregisterContentObserver();

        tracksUri = null;
        trackPointsUri = null;
        waypointsUri = null;

        var layers = map.layers();

        // polylines
        if (polylinesLayer != null) {
            layers.remove(polylinesLayer);
        }
        polylinesLayer = new GroupLayer(map);
        layers.add(polylinesLayer);

        // tracks
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
        trackPointsDebug = new TrackPointsDebug();

        // waypoints
        if (waypointsLayer != null) {
            layers.remove(waypointsLayer);
        }
        waypointsLayer = createWaypointsLayer();
        map.layers().add(waypointsLayer);
        lastWaypointId = 0;
        mapPreferences.clear();
    }

    public void updateDebugTrackPoints() {
        if (PreferencesUtils.isDebugTrackPoints()) {
            binding.map.trackpointsDebugInfo.setText(
                    getString(R.string.debug_trackpoints_info,
                            trackPointsDebug.trackpointsReceived,
                            trackPointsDebug.trackpointsInvalid,
                            trackPointsDebug.trackpointsDrawn,
                            trackPointsDebug.trackpointsPause,
                            trackPointsDebug.segments,
                            protocolVersion
                    ));
        } else {
            binding.map.trackpointsDebugInfo.setText("");
        }
    }

    private void setEndMarker(GeoPoint endPos) {
        synchronized (map.layers()) {
            if (endMarker != null) {
                endMarker.geoPoint = endPos;
                endMarker.setRotation(MapUtils.rotateWith(mapMode, movementDirection));
                waypointsLayer.populate();
                map.render();
            } else {
                endMarker = new MarkerItem(endPos.toString(), "", endPos);
                var symbol = MapUtils.createMarkerSymbol(this, R.drawable.ic_compass, false, MarkerSymbol.HotspotPlace.CENTER);
                endMarker.setMarker(symbol);
                endMarker.setRotation(MapUtils.rotateWith(mapMode, movementDirection));
                waypointsLayer.addItem(endMarker);
            }
        }
    }

    private PathLayer addNewPolyline(int trackColor) {
        polyline = new PathLayer(map, trackColor, strokeWidth);
        polylinesLayer.layers.add(polyline);
        return this.polyline;
    }

    private void readWaypoints(Uri data) {
        Log.i(TAG, "Loading waypoints from " + data);

        try {
            for (var waypoint : Waypoint.readWaypoints(getContentResolver(), data, lastWaypointId)) {
                lastWaypointId = waypoint.getId();
                final MarkerItem marker = MapUtils.createTappableMarker(this, waypoint);
                waypointsLayer.addItem(marker);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read waypoints");
        } catch (Exception e) {
            Log.e(TAG, "Reading waypoints failed", e);
        }
    }

    private ItemizedLayer createWaypointsLayer() {
        var symbol = MapUtils.createPushpinSymbol(this);
        return new ItemizedLayer(map, new ArrayList<>(), symbol, this);
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerInterface item) {
        MarkerItem markerItem = (MarkerItem) item;
        if (markerItem.uid != null) {
            try {
                var intent = new Intent("de.dennisguse.opentracks.MarkerDetails");
                intent.putExtra(EXTRA_MARKER_ID, (Long) markerItem.getUid());
                startActivity(intent);
            } catch (Exception ex) {
                Log.e(TAG, "Can't open OpenTracks MarkerDetails for marker " + markerItem.uid, ex);
            }
        }
        return true;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerInterface item) {
        return false;
    }

    private void readTracks(Uri data) {
        var tracks = Track.readTracks(getContentResolver(), data);
        if (!tracks.isEmpty()) {
            var statistics = new TrackStatistics(tracks);
            removeStatisticElements();
            PreferencesUtils.getStatisticElements()
                    .stream()
                    .sorted(comparingInt(StatisticElement::ordinal))
                    .forEach(se -> addStatisticElement(se.getText(this, statistics)));
        }
    }

    private void removeStatisticElements() {
        var childsToRemove = new ArrayList<View>();
        for (int i = 0; i < binding.map.statisticsLayout.getChildCount(); i++) {
            var childView = binding.map.statisticsLayout.getChildAt(i);
            if (childView instanceof TextView) {
                childsToRemove.add(childView);
            }
        }
        childsToRemove.forEach((view -> {
            binding.map.statisticsLayout.removeView(view);
            binding.map.statistics.removeView(view);
        }));
    }

    private void addStatisticElement(String text) {
        var textView = new TextView(this);
        textView.setId(View.generateViewId());
        textView.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
        textView.setTextColor(getColor(R.color.track_statistic));
        textView.setTextSize(COMPLEX_UNIT_PT, 10);
        binding.map.statisticsLayout.addView(textView);
        binding.map.statistics.addView(textView);
    }

    @Override
    public void onResume() {
        super.onResume();

        mapPreferences.load(map);
        binding.map.mapView.onResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && boundingBox != null) {
            var mapPos = map.getMapPosition();
            mapPos.setByBoundingBox(boundingBox, map.getWidth(), map.getHeight());
            mapPos.setBearing(mapMode.getHeading(movementDirection));
            map.animator().animateTo(mapPos);
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        int visibility = isInPictureInPictureMode ? View.GONE : View.VISIBLE;
        binding.toolbar.mapsToolbar.setVisibility(visibility);
        binding.map.fullscreenButton.setVisibility(visibility);
        binding.map.statistics.setVisibility(visibility);
    }

    private boolean isPiPMode() {
        return isInPictureInPictureMode();
    }

    @Override
    protected void onPause() {
        if (!isPiPMode()) {
            mapPreferences.save(map);
            binding.map.mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "register content observer");
        if (tracksUri != null && trackPointsUri != null && waypointsUri != null) {
            contentObserver = new OpenTracksContentObserver(tracksUri, trackPointsUri, waypointsUri, protocolVersion);
            try {
                getContentResolver().registerContentObserver(tracksUri, false, contentObserver);
                getContentResolver().registerContentObserver(trackPointsUri, false, contentObserver);
                if (waypointsUri != null) {
                    getContentResolver().registerContentObserver(waypointsUri, false, contentObserver);
                }
            } catch (SecurityException se) {
                Log.e(TAG, "Error on registering OpenTracksContentObserver", se);
                Toast.makeText(this, R.string.error_reg_content_observer, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStop() {
        unregisterContentObserver();
        super.onStop();
    }


    private void unregisterContentObserver() {
        if (contentObserver != null) {
            Log.d(TAG, "unregister content observer");
            getContentResolver().unregisterContentObserver(contentObserver);
            contentObserver = null;
        }
    }

    private void updateMapPositionAndRotation(final GeoPoint myPos) {
        var newPos = map.getMapPosition().setPosition(myPos).setBearing(mapMode.getHeading(movementDirection));
        map.animator().animateTo(newPos);
    }

}
