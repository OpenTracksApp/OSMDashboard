package de.storchp.opentracks.osmplugin;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

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
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.StreamRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.ZipRenderTheme;
import org.mapsforge.map.rendertheme.ZipXmlThemeResourceProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipInputStream;

import de.storchp.opentracks.osmplugin.compass.Compass;
import de.storchp.opentracks.osmplugin.compass.SensorListener;
import de.storchp.opentracks.osmplugin.dashboardapi.APIConstants;
import de.storchp.opentracks.osmplugin.dashboardapi.Track;
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
import de.storchp.opentracks.osmplugin.utils.StringUtils;
import de.storchp.opentracks.osmplugin.utils.TrackStatistics;

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
    private Set<Uri> mapFiles;
    private Uri mapTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        var intent = getIntent();
        if (intent != null) {
            onNewIntent(intent);
        }
    }

    private void switchFullscreen() {
        showFullscreen(!fullscreenMode);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(APIConstants.ACTION_DASHBOARD_PAYLOAD);
        protocolVersion = intent.getIntExtra(EXTRAS_PROTOCOL_VERSION, 1);
        tracksUri = APIConstants.getTracksUri(uris);
        trackPointsUri = APIConstants.getTrackPointsUri(uris);
        waypointsUri = APIConstants.getWaypointsUri(uris);
        readTrackpoints(trackPointsUri, false, protocolVersion);
        readTracks(tracksUri);
        readWaypoints(waypointsUri, false);

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
                readWaypoints(waypointsUri, true);
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

    @Override
    public void onBackPressed() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                && isOpenTracksRecordingThisTrack
                && PreferencesUtils.isPipEnabled()) {
            enterPictureInPictureMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu, true);
        menu.findItem(R.id.share).setVisible(true);
        menu.findItem(R.id.purge_tilecache).setVisible(true);
        return true;
    }

    protected void createTileCaches() {
        this.tileCache = AndroidUtil.createTileCache(this, getPersistableId(),
                this.binding.map.mapView.getModel().displayModel.getTileSize(), this.getScreenRatio() * PreferencesUtils.getTileCacheCapacityFactor(),
                this.binding.map.mapView.getModel().frameBufferModel.getOverdrawFactor(), PreferencesUtils.getPersistentTileCache());
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
        binding.map.mapView.getModel().frameBufferModel.setOverdrawFactor(PreferencesUtils.getOverdrawFactor());
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
        mapTheme = PreferencesUtils.getMapThemeUri();
        if (mapTheme == null) {
            return InternalRenderTheme.DEFAULT;
        }
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error loading theme " + mapTheme, e);
            return InternalRenderTheme.DEFAULT;
        }
    }

    protected MapDataStore getMapFile() {
        var mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        mapFiles = PreferencesUtils.getMapUris();
        if (mapFiles.isEmpty()) {
            return null;
        }
        var mapsCount = new AtomicInteger(0);
        mapFiles.stream()
                .filter(uri -> DocumentFile.isDocumentUri(this, uri))
                .map(uri -> DocumentFile.fromSingleUri(this, uri))
                .filter(documentFile -> documentFile != null && documentFile.canRead())
                .forEach(documentFile -> readMapFile(mapDataStore, mapsCount, documentFile));

        if (mapsCount.get() == 0 && !mapFiles.isEmpty()) {
            Toast.makeText(this, R.string.error_loading_offline_map, Toast.LENGTH_LONG).show();
        }

        return mapsCount.get() > 0 ? mapDataStore : null;
    }

    private void readMapFile(MultiMapDataStore mapDataStore, AtomicInteger mapsCount, DocumentFile documentFile) {
        try {
            var inputStream = (FileInputStream) getContentResolver().openInputStream(documentFile.getUri());
            mapDataStore.addMapDataStore(new MapFile(inputStream, 0, null), false, false);
            mapsCount.getAndIncrement();
        } catch (Exception e) {
            Log.e(TAG, "Can't open mapFile", e);
        }
    }

    protected void createLayers() {
        var mapFile = getMapFile();

        if (mapFile != null) {
            var rendererLayer = new TileRendererLayer(this.tileCache, mapFile,
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
        var tileSource = OpenStreetMapMapnik.INSTANCE;
        tileSource.setUserAgent(getString(R.string.app_name) + ":" + BuildConfig.APPLICATION_ID);
        this.tileLayer = new TileDownloadLayer(this.tileCache, this.binding.map.mapView.getModel().mapViewPosition,
                tileSource, AndroidGraphicFactory.INSTANCE);
        binding.map.mapView.getLayerManager().getLayers().add(0, this.tileLayer);

        binding.map.mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
        binding.map.mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
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
                    setOnlineTileLayer();
                    ((TileDownloadLayer) tileLayer).onResume();
                    mapConsent.setChecked(true);
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
        binding.map.mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        purgeTileCaches();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.map_info) {
            var intent = new Intent(this, MainActivity.class);
            if (tileCache != null) {
                intent.putExtra(MainActivity.EXTRA_MAP_INFO, "TileCache capacity=" + tileCache.getCapacity() + ", capacityFirstLevel=" + tileCache.getCapacityFirstLevel());
            }
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.share) {
            sharePicture();
            return true;
        } else if (item.getItemId() == R.id.purge_tilecache) {
            purgeTileCaches();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sharePicture() {
        // prepare rendering
        var view = binding.map.mainView;

        binding.map.sharePictureTitle.setText(R.string.share_picture_title);
        binding.map.controls.setVisibility(View.INVISIBLE);
        binding.map.attribution.setVisibility(View.INVISIBLE);

        // draw
        var canvas = new Canvas();
        var toBeCropped = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(toBeCropped);
        view.draw(canvas);

        var bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inTargetDensity = 1;
        toBeCropped.setDensity(Bitmap.DENSITY_NONE);

        int cropFromTop = (int) (70 * ((float) getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
        int fromHere = toBeCropped.getHeight() - cropFromTop;
        var croppedBitmap = Bitmap.createBitmap(toBeCropped, 0, cropFromTop, toBeCropped.getWidth(), fromHere);

        try {
            var sharedFolderPath = new File(this.getCacheDir(), "shared");
            sharedFolderPath.mkdir();
            var file = new File(sharedFolderPath, System.currentTimeMillis() + ".png");
            var out = new FileOutputStream(file);
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            var share = new Intent(Intent.ACTION_SEND);
            share.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file));
            share.setType("image/png");
            startActivity(Intent.createChooser(share, "send"));
        } catch (Exception exception) {
            Log.e(TAG, "Error sharing Bitmap", exception);
        }

        binding.map.controls.setVisibility(View.VISIBLE);
        binding.map.attribution.setVisibility(View.VISIBLE);
        binding.map.sharePictureTitle.setText("");
    }

    @Override
    protected void changeMapMode(MapMode mapMode) {
        this.mapMode = mapMode;
        rotateMap();
    }

    @Override
    protected void changeArrowMode(ArrowMode arrowMode) {
        this.arrowMode = arrowMode;
        if (endMarker != null && endMarker.rotateWith(arrowMode, mapMode, movementDirection, compass)) {
            binding.map.mapView.getLayerManager().redrawLayers();
        }
    }

    @Override
    protected void onOnlineMapConsentChanged(boolean consent) {
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

    private void readTrackpoints(Uri data, boolean update, int protocolVersion) {
        Log.i(TAG, "Loading trackpoints from " + data);

        var layers = binding.map.mapView.getLayerManager().getLayers();
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

        var latLongs = new ArrayList<LatLong>();
        int tolerance = PreferencesUtils.getTrackSmoothingTolerance();

        try {
            var segments = TrackPoint.readTrackPointsBySegments(getContentResolver(), data, lastTrackPointId, protocolVersion);
            if (segments.isEmpty()) {
                Log.d(TAG, "No new trackpoints received");
                return;
            }
            for (var trackPoints : segments) {
                if (!update) {
                    polyline = null; // cut polyline on new segment
                    if (tolerance > 0) { // smooth track
                        trackPoints = MapUtils.decimate(tolerance, trackPoints);
                    }
                }
                for (var trackPoint : trackPoints) {
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
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read trackpoints");
            return;
        } catch (Exception e) {
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

    private void setEndMarker(LatLong endPos) {
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

    private Polyline addNewPolyline(int trackColor) {
        polyline = MapUtils.createPolyline(binding.map.mapView, trackColor, strokeWidth);
        polylinesLayer.layers.add(polyline);
        return this.polyline;
    }

    private void readWaypoints(Uri data, boolean update) {
        Log.i(TAG, "Loading waypoints from " + data);

        var layers = binding.map.mapView.getLayerManager().getLayers();
        if (waypointsLayer != null) {
            layers.remove(waypointsLayer);
        }
        if (!update) { // reset data
            lastWaypointId = 0;
        }

        try {
            for (var waypoint : Waypoint.readWaypoints(getContentResolver(), data, lastWaypointId)) {
                lastWaypointId = waypoint.getId();
                if (waypointsLayer == null) {
                    waypointsLayer = new GroupLayer();
                }
                waypointsLayer.layers.add(createTappableMarker(waypoint));
            }
            if (waypointsLayer != null) {
                layers.add(waypointsLayer);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read waypoints");
        } catch (Exception e) {
            Log.e(TAG, "Reading waypoints failed", e);
        }
    }

    private Marker createTappableMarker(Waypoint waypoint) {
        var drawable = ContextCompat.getDrawable(this, R.drawable.ic_marker_orange_pushpin_with_shadow);
        assert drawable != null;
        var bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        bitmap.incrementRefCount();
        return new Marker(waypoint.getLatLong(), bitmap, 0, -bitmap.getHeight() / 2) {
            @Override
            public boolean onTap(LatLong geoPoint, Point viewPosition,
                                 Point tapPoint) {
                if (contains(binding.map.mapView.getMapViewProjection().toPixels(getPosition()), tapPoint)) {
                    var intent = new Intent("de.dennisguse.opentracks.MarkerDetails");
                    intent.putExtra(EXTRA_MARKER_ID, waypoint.getId());
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        };
    }

    private void readTracks(Uri data) {
        var tracks = Track.readTracks(getContentResolver(), data);
        if (!tracks.isEmpty()) {
            var statistics = new TrackStatistics(tracks);
            binding.map.category.setText(statistics.getCategory());
            binding.map.totalTime.setText(StringUtils.formatElapsedTimeWithHour(statistics.getTotalTimeMillis()));
            binding.map.totalDistance.setText(StringUtils.formatDistance(this, statistics.getTotalDistanceMeter()));
            binding.map.gain.setText(StringUtils.formatAltitudeChange(this, statistics.getElevationGainMeter()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // if map or theme changed, recreate layers
        if (!Objects.equals(mapTheme, PreferencesUtils.getMapThemeUri())
                || !Objects.equals(mapFiles, PreferencesUtils.getMapUris())) {
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

        if (this.tileLayer instanceof TileDownloadLayer) {
            ((TileDownloadLayer) this.tileLayer).onResume();
        }
        compass.start(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && boundingBox != null) {
            var dimension = getScaledDimension(this.binding.map.mapView.getModel());
            if (dimension != null) {
                this.binding.map.mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(
                        boundingBox.getCenterPoint(),
                        (byte) Math.min(Math.min(LatLongUtils.zoomForBounds(
                                        dimension,
                                        boundingBox, this.binding.map.mapView.getModel().displayModel.getTileSize()),
                                getZoomLevelMax()), 16)));
                boundingBox = null; // only set the zoomlevel once
            }
        }
    }

    private Dimension getScaledDimension(Model model) {
        var dimension = model.mapViewDimension.getDimension();
        if (dimension != null) {
            float scaleFactor = model.displayModel.getScaleFactor();
            return new Dimension((int) (dimension.width / scaleFactor), (int) (dimension.height / scaleFactor));
        }
        return null;
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, @NonNull Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        }
        int visibility = isInPictureInPictureMode ? View.GONE : View.VISIBLE;
        binding.toolbar.mapsToolbar.setVisibility(visibility);
        binding.map.zoomInButton.setVisibility(visibility);
        binding.map.zoomOutButton.setVisibility(visibility);
        binding.map.fullscreenButton.setVisibility(visibility);
        binding.map.statistics.setVisibility(visibility);
    }

    private boolean isPiPMode() {
        return isInPictureInPictureMode();
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
        float mapHeading = mapMode.getHeading(movementDirection, compass);
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
