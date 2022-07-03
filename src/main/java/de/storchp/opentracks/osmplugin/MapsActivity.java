package de.storchp.opentracks.osmplugin;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
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
        final var intent = getIntent();
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
        final var decorView = getWindow().getDecorView();
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
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu, true);
        menu.findItem(R.id.share).setVisible(true);
        menu.findItem(R.id.purge_tilecache).setVisible(true);
        return true;
    }

    protected void createTileCaches() {
        this.tileCache = AndroidUtil.createTileCache(this, getPersistableId(),
                this.binding.map.mapView.getModel().displayModel.getTileSize(), this.getScreenRatio(),
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
        binding.map.mapView.getModel().frameBufferModel.setOverdrawFactor(1.4d);
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
            final var renderThemeFile = DocumentFile.fromSingleUri(getApplication(), mapTheme);
            assert renderThemeFile != null;
            var themeFileUri = renderThemeFile.getUri();
            if (Objects.requireNonNull(renderThemeFile.getName(), "Theme files must have a name").endsWith(".zip")) {
                final var fragment = themeFileUri.getFragment();
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
        final var mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        mapFiles = PreferencesUtils.getMapUris();
        if (mapFiles.isEmpty()) {
            return null;
        }
        final var mapsCount = new AtomicInteger(0);
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

    private void readMapFile(final MultiMapDataStore mapDataStore, final AtomicInteger mapsCount, final DocumentFile documentFile) {
        try {
            final var inputStream = (FileInputStream) getContentResolver().openInputStream(documentFile.getUri());
            mapDataStore.addMapDataStore(new MapFile(inputStream, 0, null), false, false);
            mapsCount.getAndIncrement();
        } catch (final Exception e) {
            Log.e(TAG, "Can't open mapFile", e);
        }
    }

    protected void createLayers() {
        final var mapFile = getMapFile();

        if (mapFile != null) {
            final var rendererLayer = new TileRendererLayer(this.tileCache, mapFile,
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
        final var tileSource = OpenStreetMapMapnik.INSTANCE;
        tileSource.setUserAgent(getString(R.string.app_name) + ":" + BuildConfig.APPLICATION_ID);
        this.tileLayer = new TileDownloadLayer(this.tileCache, this.binding.map.mapView.getModel().mapViewPosition,
                tileSource, AndroidGraphicFactory.INSTANCE);
        binding.map.mapView.getLayerManager().getLayers().add(0, this.tileLayer);

        binding.map.mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
        binding.map.mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
    }

    private void showOnlineMapConsent() {
        final var message = new SpannableString(getString(R.string.online_map_consent));
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.map_info ) {
            startActivity(new Intent(this, MainActivity.class));
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
        final var view = binding.map.mainView;

        binding.map.sharePictureTitle.setText(R.string.share_picture_title);
        binding.map.controls.setVisibility(View.INVISIBLE);
        binding.map.attribution.setVisibility(View.INVISIBLE);

        // draw
        final var canvas = new Canvas();
        final var toBeCropped = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(toBeCropped);
        view.draw(canvas);

        final var bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inTargetDensity = 1;
        toBeCropped.setDensity(Bitmap.DENSITY_NONE);

        final int cropFromTop = (int)(70 * ((float) getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
        final int fromHere = toBeCropped.getHeight() - cropFromTop;
        final var croppedBitmap = Bitmap.createBitmap(toBeCropped, 0, cropFromTop, toBeCropped.getWidth(), fromHere);

        try {
            final var sharedFolderPath = new File(this.getCacheDir(), "shared");
            sharedFolderPath.mkdir();
            final var file = new File(sharedFolderPath, System.currentTimeMillis() + ".png");
            final var out = new FileOutputStream(file);
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            final var share = new Intent(Intent.ACTION_SEND);
            share.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file));
            share.setType("image/png");
            startActivity(Intent.createChooser(share, "send"));
        } catch (final Exception exception) {
            Log.e(TAG, "Error sharing Bitmap", exception);
        }

        binding.map.controls.setVisibility(View.VISIBLE);
        binding.map.attribution.setVisibility(View.VISIBLE);
        binding.map.sharePictureTitle.setText("");
    }

    @Override
    protected void changeMapMode(final MapMode mapMode) {
        this.mapMode = mapMode;
        rotateMap();
    }

    @Override
    protected void changeArrowMode(final ArrowMode arrowMode) {
        this.arrowMode = arrowMode;
        if (endMarker != null && endMarker.rotateWith(arrowMode, mapMode, movementDirection, compass)) {
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

    private void readTrackpoints(final Uri data, final boolean update, final int protocolVersion) {
        Log.i(TAG, "Loading trackpoints from " + data);

        final var layers = binding.map.mapView.getLayerManager().getLayers();
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

        final var latLongs = new ArrayList<LatLong>();
        final int tolerance = PreferencesUtils.getTrackSmoothingTolerance();

        try {
            final var segments = TrackPoint.readTrackPointsBySegments(getContentResolver(), data, lastTrackPointId, protocolVersion);
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
                for (final var trackPoint : trackPoints) {
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

        final var layers = binding.map.mapView.getLayerManager().getLayers();
        if (waypointsLayer != null) {
            layers.remove(waypointsLayer);
        }
        if (!update) { // reset data
            lastWaypointId = 0;
        }

        try {
            for (final var waypoint : Waypoint.readWaypoints(getContentResolver(), data, lastWaypointId)) {
                lastWaypointId = waypoint.getId();
                if (waypointsLayer == null) {
                    waypointsLayer = new GroupLayer();
                }
                waypointsLayer.layers.add(createTappableMarker(waypoint));
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
        final var drawable = ContextCompat.getDrawable(this, R.drawable.ic_marker_orange_pushpin_with_shadow);
        assert drawable != null;
        final var bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        bitmap.incrementRefCount();
        return new Marker(waypoint.getLatLong(), bitmap, 0, -bitmap.getHeight() / 2) {
            @Override
            public boolean onTap(final LatLong geoPoint, final Point viewPosition,
                                 final Point tapPoint) {
                if (contains(binding.map.mapView.getMapViewProjection().toPixels(getPosition()), tapPoint)) {
                    final var intent = new Intent("de.dennisguse.opentracks.MarkerDetails");
                    intent.putExtra(EXTRA_MARKER_ID, waypoint.getId());
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        };
    }

    private void readTracks(final Uri data, final int protocolVersion) {
        final var tracks = Track.readTracks(getContentResolver(), data, protocolVersion);
        if (!tracks.isEmpty()) {
            final var statistics = new TrackStatistics(tracks);
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
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && boundingBox != null) {
            final var dimension = getScaledDimension(this.binding.map.mapView.getModel());
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

    private Dimension getScaledDimension(final Model model) {
        final var dimension = model.mapViewDimension.getDimension();
        if (dimension != null) {
            final float scaleFactor = model.displayModel.getScaleFactor();
            return new Dimension((int)(dimension.width / scaleFactor), (int)(dimension.height / scaleFactor));
        }
        return null;
    }

    @Override
    public void onPictureInPictureModeChanged (final boolean isInPictureInPictureMode, final Configuration newConfig) {
        final int visibility = isInPictureInPictureMode ? View.GONE : View.VISIBLE;
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
        } catch (final SecurityException se) {
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
