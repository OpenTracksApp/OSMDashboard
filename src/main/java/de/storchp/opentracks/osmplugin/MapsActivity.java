package de.storchp.opentracks.osmplugin;


import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
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
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
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
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.StreamRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.storchp.opentracks.osmplugin.dashboardapi.APIConstants;
import de.storchp.opentracks.osmplugin.dashboardapi.TrackPointsColumn;
import de.storchp.opentracks.osmplugin.dashboardapi.TracksColumn;
import de.storchp.opentracks.osmplugin.maps.MapsforgeMapView;
import de.storchp.opentracks.osmplugin.maps.StyleColorCreator;
import de.storchp.opentracks.osmplugin.maps.utils.PreferencesUtils;

public class MapsActivity extends BaseActivity {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private static final byte MAP_DEFAULT_ZOOM_LEVEL = (byte) 12;

    private static final String EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOULD_KEEP_SCREEN_ON";

    private MapsforgeMapView mapView;
    private Layer tileLayer;
    private List<TileCache> tileCaches = new ArrayList<>();

    private BoundingBox boundingBox;
    private GroupLayer groupLayer;

    private long lastTrackPointId = 0;
    private Long lastTrackId = null;
    private int trackColor;
    private Polyline polyline;
    private FixedPixelCircle endMarker = null;

    private StyleColorCreator colorCreator = null;
    private LatLong startPos;
    private LatLong endPos;

    static Paint createPaint(int color, int strokeWidth, Style style) {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(style);
        return paint;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidGraphicFactory.createInstance(this.getApplication());

        setContentView(R.layout.activity_maps_activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        Toolbar toolbar = findViewById(R.id.maps_toolbar);
        setSupportActionBar(toolbar);

        createMapViews();
        createTileCaches();
        createLayers();

        // Get the intent that started this activity
        final ArrayList<Uri> uris = getIntent().getParcelableArrayListExtra(APIConstants.ACTION_DASHBOARD_PAYLOAD);
        final Uri tracksUri = APIConstants.getTracksUri(uris);
        final Uri trackPointsUri = APIConstants.getTrackPointsUri(uris);
        readTrackpoints(trackPointsUri, false);
        readTrack(tracksUri);

        getContentResolver().registerContentObserver(trackPointsUri, true, new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                readTrackpoints(trackPointsUri, true);
                readTrack(tracksUri);
            }
        });

        if (getIntent().getBooleanExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        final boolean showOnLockScreen = getIntent().getBooleanExtra(EXTRAS_SHOW_WHEN_LOCKED, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(showOnLockScreen);
        } else if (showOnLockScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
        for (TileCache tileCache : tileCaches) {
            tileCache.purge();
        }
        tileCaches.clear();
    }

    protected XmlRenderTheme getRenderTheme() {
        Uri mapTheme = PreferencesUtils.getMapThemeUri(this);
        if (mapTheme == null) {
            return InternalRenderTheme.DEFAULT;
        }
        try {
            DocumentFile renderThemeFile = DocumentFile.fromSingleUri(getApplication(), mapTheme);
            return new StreamRenderTheme("/assets/", getContentResolver().openInputStream(renderThemeFile.getUri()));
        } catch (Exception e) {
            Log.e(TAG, "Error loading theme " + mapTheme, e);
            return InternalRenderTheme.DEFAULT;
        }
    }

    protected MapDataStore getMapFile() {
        MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        final Set<Uri> mapFiles = PreferencesUtils.getMapUris(this);
        if (mapFiles.isEmpty()) {
            return null;
        }
        int mapsCount = 0;
        for (Uri mapUri: mapFiles) {
            try {
                    if (DocumentFile.isDocumentUri(this, mapUri)) {
                        FileInputStream inputStream = (FileInputStream) getContentResolver().openInputStream(mapUri);
                        mapDataStore.addMapDataStore(new MapFile(inputStream, 0, null), false, false);
                        mapsCount++;
                    }
            } catch (FileNotFoundException ignored) {
                Log.e(TAG, "Can't open mapFile", ignored);
            }
        }

        return mapsCount > 0 ? mapDataStore : null;
    }

    protected void createLayers() {
        final MapDataStore mapFile = getMapFile();

        if (mapFile != null) {
            TileRendererLayer tileRendererLayer1 = new TileRendererLayer(this.tileCaches.get(0), mapFile,
                    this.mapView.getModel().mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
            tileRendererLayer1.setXmlRenderTheme(getRenderTheme());
            this.tileLayer = tileRendererLayer1;
            mapView.getLayerManager().getLayers().add(this.tileLayer);
        } else if (!PreferencesUtils.getOnlineMapConsent(this)) {
            showOnlineMapConsent();
        } else {
            setOnlineTileLayer();
        }
        mapView.setZoomLevel(MAP_DEFAULT_ZOOM_LEVEL);
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
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PreferencesUtils.setOnlineMapConsent(MapsActivity.this, true);
                    setOnlineTileLayer();
                    ((TileDownloadLayer) tileLayer).onResume();
                    mapConsent.setChecked(true);
                }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.map_info ) {
            Intent intent = new Intent(MapsActivity.this, MainActivity.class);
            startActivityForResult(intent, 0);
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            mapView.getLayerManager().getLayers().remove(tileLayer, true);
            this.tileLayer = null;
        }
    }

    private void readTrackpoints(Uri data, boolean update) {
        Log.i(TAG, "Loading track from " + data);

        Layers layers = mapView.getLayerManager().getLayers();
        if (!update) { // reset data
            if (groupLayer != null) {
                layers.remove(groupLayer);
            }
            groupLayer = new GroupLayer();
            lastTrackId = null;
            lastTrackPointId = 0;
            colorCreator = new StyleColorCreator(StyleColorCreator.GOLDEN_RATIO_CONJUGATE / 2);
            trackColor = colorCreator.nextColor();
            polyline = null;
            startPos = null;
            endPos = null;
            endMarker = null;
            boundingBox = null;
        }

        List<LatLong> latLongs = new ArrayList<>();

        try (Cursor cursor = getContentResolver().query(data, TrackPointsColumn.PROJECTION, null, null, null)) {
            while (cursor.moveToNext()) {
                Long trackPointId = cursor.getLong(cursor.getColumnIndex(TrackPointsColumn._ID));
                if (update && lastTrackPointId >= trackPointId) { // skip trackpoints we already have
                    continue;
                }
                lastTrackPointId = trackPointId;
                Long newTrackId = cursor.getLong(cursor.getColumnIndex(TrackPointsColumn.TRACKID));
                double latitude = cursor.getInt(cursor.getColumnIndex(TrackPointsColumn.LATITUDE)) / 1E6;
                double longitude = cursor.getInt(cursor.getColumnIndex(TrackPointsColumn.LONGITUDE)) / 1E6;

                if (TrackPointsColumn.isValidLocation(latitude, longitude)) {
                    if (!newTrackId.equals(lastTrackId)) {
                        trackColor = colorCreator.nextColor();
                        lastTrackId = newTrackId;
                        polyline = null; // reset current polyline when trackId changes
                        startPos = null;
                    }

                    if (polyline == null) {
                        Log.d(TAG, "Continue new segment after pause.");
                        polyline = newPolyline(trackColor);
                    }

                    LatLong latLong = new LatLong(latitude, longitude);
                    polyline.addPoint(latLong);

                    if (!update) {
                        latLongs.add(latLong);
                    }

                    if (startPos == null) {
                        startPos = latLong;
                    }
                    endPos = latLong;
                } else if (latitude == TrackPointsColumn.PAUSE_LATITUDE) {
                    Log.d(TAG, "Got pause trackpoint");
                    polyline = null;
                }
                // ignoring RESUME_LATITUDE that might be transferred by OpenTracks.
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read trackpoints");
        } catch (Exception e) {
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
            if (layers.indexOf(groupLayer) == -1 && groupLayer.layers.size() > 0) {
                layers.add(groupLayer);
            }
        } else if (!update) {
            Toast.makeText(MapsActivity.this, R.string.no_data, Toast.LENGTH_LONG).show();
        }
    }

    private FixedPixelCircle addEndPoint(LatLong pos) {
        FixedPixelCircle marker = new FixedPixelCircle(pos, 10, createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.RED), 0,
                Style.FILL), null);
        groupLayer.layers.add(marker);
        return marker;
    }

    private Polyline newPolyline(int trackColor) {
        Polyline polyline = new Polyline(createPaint(trackColor,
                (int) (8 * mapView.getModel().displayModel.getScaleFactor()),
                Style.STROKE), AndroidGraphicFactory.INSTANCE);
        groupLayer.layers.add(polyline);
        return polyline;
    }

    private void readTrack(Uri data) {
        Log.i(TAG, "Loading track from " + data);

        // Contains only one row.
        try (Cursor cursor = getContentResolver().query(data, TracksColumn.PROJECTION, null, null, null)) {
            if (cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndex(TracksColumn._ID));
                String name = cursor.getString(cursor.getColumnIndex(TracksColumn.NAME));
                String description = cursor.getString(cursor.getColumnIndex(TracksColumn.DESCRIPTION));
                String category = cursor.getString(cursor.getColumnIndex(TracksColumn.CATEGORY));
                int startTime = cursor.getInt(cursor.getColumnIndex(TracksColumn.STARTTIME));
                int stopTime = cursor.getInt(cursor.getColumnIndex(TracksColumn.STOPTIME));
                float totalDistance = cursor.getFloat(cursor.getColumnIndex(TracksColumn.TOTALDISTANCE));
                int totalTime = cursor.getInt(cursor.getColumnIndex(TracksColumn.TOTALTIME));
                int movingTime = cursor.getInt(cursor.getColumnIndex(TracksColumn.MOVINGTIME));
                float avgSpeed = cursor.getFloat(cursor.getColumnIndex(TracksColumn.AVGSPEED));
                float avgMovingSpeed = cursor.getFloat(cursor.getColumnIndex(TracksColumn.AVGMOVINGSPEED));
                float maxSpeed = cursor.getFloat(cursor.getColumnIndex(TracksColumn.MAXSPEED));
                float minElevation = cursor.getFloat(cursor.getColumnIndex(TracksColumn.MINELEVATION));
                float maxElevation = cursor.getFloat(cursor.getColumnIndex(TracksColumn.MAXELEVATION));
                float elevationGain = cursor.getFloat(cursor.getColumnIndex(TracksColumn.ELEVATIONGAIN));

                // TODO: show data on dashboard
                Log.d(TAG, "Track: " + name + ", start: " + startTime + ", end: " + stopTime);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read track");
        } catch (Exception e) {
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && boundingBox != null) {
            Dimension dimension = this.mapView.getModel().mapViewDimension.getDimension();
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

    @Override
    void recreateMap(boolean menuNeedsUpdate) {
        // always recreate the map
        if (menuNeedsUpdate) {
            invalidateOptionsMenu();
        } else {
            recreate();
        }
    }
}
