package de.storchp.opentracks.osmplugin;


import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

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
import org.mapsforge.map.android.util.ExternalRenderThemeUsingJarResources;
import org.mapsforge.map.datastore.MapDataStore;
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
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends BaseActivity {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private MapsforgeMapView mapView;
    private Layer layer;
    private List<TileCache> tileCaches = new ArrayList<>();

    private BoundingBox boundingBox;
    private GroupLayer groupLayer;

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
        Intent intent = getIntent();
        final ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Constants.ACTION_DASHBOARD_PAYLOAD);
        final Uri tracksUri = Constants.getTracksUri(uris);
        final Uri trackPointsUri = Constants.getTrackPointsUri(uris);
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
        return (byte)Math.min(mapView.getModel().mapViewPosition.getZoomLevelMax(), 20);
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
        String mapTheme = baseApplication.getMapTheme();
        if (mapTheme == null) {
            return InternalRenderTheme.DEFAULT;
        }
        try {
            File renderThemeFile = new File(mapTheme);
            if (renderThemeFile.isDirectory()) {
                renderThemeFile = new File(renderThemeFile, renderThemeFile.getName() + ".xml");
            }
            return new ExternalRenderThemeUsingJarResources(renderThemeFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error loading theme " + mapTheme, e);
            return InternalRenderTheme.DEFAULT;
        }
    }

    protected MapDataStore getMapFile() {
        if (baseApplication.getMapFileName() == null) {
            return null;
        }
        final File mapFile = new File(baseApplication.getMapFileName());
        return mapFile.canRead() ? new MapFile(mapFile) : null;
    }

    protected void createLayers() {
        final MapDataStore mapFile = getMapFile();

        if (mapFile != null) {
            TileRendererLayer tileRendererLayer1 = new TileRendererLayer(this.tileCaches.get(0), mapFile,
                    this.mapView.getModel().mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
            tileRendererLayer1.setXmlRenderTheme(getRenderTheme());
            this.layer = tileRendererLayer1;
            mapView.getLayerManager().getLayers().add(this.layer);
        } else {
            OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
            tileSource.setUserAgent("OpenTracks-OsmPlugIn");
            this.layer = new TileDownloadLayer(this.tileCaches.get(0), this.mapView.getModel().mapViewPosition,
                    tileSource, AndroidGraphicFactory.INSTANCE);
            mapView.getLayerManager().getLayers().add(this.layer);

            mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
            mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
        }
        mapView.setZoomLevel(baseApplication.getZoomLevelDefault());
        mapView.getModel().mapViewPosition.setZoomLevelMax(getZoomLevelMax());
        mapView.getModel().mapViewPosition.setZoomLevelMin(getZoomLevelMin());
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
        switch (item.getItemId()) {
            case R.id.map_info:
                Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                startActivityForResult(intent, 0);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void readTrackpoints(Uri data, boolean update) {
        Log.i(TAG, "Loading track from " + data);

        Layers layers = mapView.getLayerManager().getLayers();
        if (groupLayer != null) {
            layers.remove(groupLayer);
        }
        groupLayer = new GroupLayer();
        StyleColorCreator colorCreator = new StyleColorCreator(StyleColorCreator.GOLDEN_RATIO_CONJUGATE/2);

        double minLat = 0;
        double maxLat = 0;
        double minLon = 0;
        double maxLon = 0;

        LatLong startPos = null;
        LatLong endPos = null;
        Long trackId = null;
        int trackColor = colorCreator.nextColor();

        try (Cursor cursor = getContentResolver().query(data, Constants.Trackpoints.PROJECTION, null, null, null)) {
            Polyline polyline = null;

            while (cursor.moveToNext()) {
                Long newTrackId = cursor.getLong(cursor.getColumnIndex(Constants.Trackpoints.TRACKID));
                double latitude = cursor.getInt(cursor.getColumnIndex(Constants.Trackpoints.LATITUDE)) / 1E6;
                double longitude = cursor.getInt(cursor.getColumnIndex(Constants.Trackpoints.LONGITUDE)) / 1E6;

                if (Constants.isValidLocation(latitude, longitude)) {
                    if (!newTrackId.equals(trackId)) {
                        trackColor = colorCreator.nextColor();
                        trackId = newTrackId;
                        polyline = null; // reset current polyline when trackId changes
                        if (endPos != null) {
                            addEndPoint(endPos);
                        }
                        startPos = null;
                    }

                    if (polyline == null) {
                        Log.d(TAG, "Continue new segment after pause.");
                        polyline = newPolyline(trackColor);
                        groupLayer.layers.add(polyline);
                    }

                    LatLong latLong = new LatLong(latitude, longitude);
                    polyline.addPoint(latLong);

                    if (minLat == 0.0) {
                        minLat = latLong.latitude;
                        maxLat = latLong.latitude;
                        minLon = latLong.longitude;
                        maxLon = latLong.longitude;
                    } else {
                        minLat = Math.min(minLat, latLong.latitude);
                        maxLat = Math.max(maxLat, latLong.latitude);
                        minLon = Math.min(minLon, latLong.longitude);
                        maxLon = Math.max(maxLon, latLong.longitude);
                    }

                    if (startPos == null) {
                        startPos = latLong;
                        addStartPoint(startPos);
                    }
                    endPos = latLong;
                } else if (latitude == Constants.Trackpoints.PAUSE_LATITUDE) {
                    Log.d(TAG, "Got pause trackpoint");
                    polyline = null;
                }
                // ingoring RESUME_LATITUDE that might be transferred by OpenTracks.
            }
        } catch (Exception e) {
            Log.e(TAG, "Reading trackpoints failed", e);
            return;
        }

        if (endPos != null && !endPos.equals(startPos)) {
            addEndPoint(endPos);
        }

        LatLong myPos = null;
        if (update && endPos != null) {
            myPos = endPos;
        } else if (startPos != null) {
            myPos = new LatLong((minLat + maxLat) / 2, (minLon + maxLon) / 2);
        }

        boundingBox = null;
        if (myPos != null) {
            layers.add(groupLayer);
            mapView.setCenter(myPos);
            if (!update) {
                boundingBox = new BoundingBox(minLat, minLon, maxLat, maxLon);
            }
        } else {
            Toast.makeText(MapsActivity.this, R.string.no_data, Toast.LENGTH_LONG).show();
        }
    }

    private void addStartPoint(LatLong pos) {
        addPoint(pos, Color.GREEN);
    }

    private void addEndPoint(LatLong pos) {
        addPoint(pos, Color.RED);
    }

    private void addPoint(LatLong pos, Color color) {
        groupLayer.layers.add(new FixedPixelCircle(pos, 10, createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(color), 0,
                Style.FILL), null));
    }

    private Polyline newPolyline(int trackColor) {
        return new Polyline(createPaint(trackColor,
                (int) (8 * mapView.getModel().displayModel.getScaleFactor()),
                Style.STROKE), AndroidGraphicFactory.INSTANCE);
    }

    private void readTrack(Uri data) {
        Log.i(TAG, "Loading track from " + data);

        // Contains only one row.
        try (Cursor cursor = getContentResolver().query(data, Constants.Track.PROJECTION, null, null, null)) {
            if (cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndex(Constants.Track._ID));
                String name = cursor.getString(cursor.getColumnIndex(Constants.Track.NAME));
                String description = cursor.getString(cursor.getColumnIndex(Constants.Track.DESCRIPTION));
                String category = cursor.getString(cursor.getColumnIndex(Constants.Track.CATEGORY));
                int startTime = cursor.getInt(cursor.getColumnIndex(Constants.Track.STARTTIME));
                int stopTime = cursor.getInt(cursor.getColumnIndex(Constants.Track.STOPTIME));
                float totalDistance = cursor.getFloat(cursor.getColumnIndex(Constants.Track.TOTALDISTANCE));
                int totalTime = cursor.getInt(cursor.getColumnIndex(Constants.Track.TOTALTIME));
                int movingTime = cursor.getInt(cursor.getColumnIndex(Constants.Track.MOVINGTIME));
                float avgSpeed = cursor.getFloat(cursor.getColumnIndex(Constants.Track.AVGSPEED));
                float avgMovingSpeed = cursor.getFloat(cursor.getColumnIndex(Constants.Track.AVGMOVINGSPEED));
                float maxSpeed = cursor.getFloat(cursor.getColumnIndex(Constants.Track.MAXSPEED));
                float minElevation = cursor.getFloat(cursor.getColumnIndex(Constants.Track.MINELEVATION));
                float maxElevation = cursor.getFloat(cursor.getColumnIndex(Constants.Track.MAXELEVATION));
                float elevationGain = cursor.getFloat(cursor.getColumnIndex(Constants.Track.ELEVATIONGAIN));

                // TODO: show data on dashboard
                Log.d(TAG, "Track: " + name + ", start: " + startTime + ", end: " + stopTime);
            }
        } catch (Exception e) {
            Log.e(TAG, "Reading track failed", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.layer instanceof TileDownloadLayer) {
            ((TileDownloadLayer) this.layer).onResume();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && boundingBox != null) {
            Dimension dimension = this.mapView.getModel().mapViewDimension.getDimension();
            this.mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(
                boundingBox.getCenterPoint(),
                (byte)Math.min(Math.min(LatLongUtils.zoomForBounds(
                        dimension, boundingBox, this.mapView.getModel().displayModel.getTileSize()),
                    getZoomLevelMax()), 16)));
            boundingBox = null; // only set the zoomlevel once
        }
    }

    @Override
    protected void onPause() {
        if (this.layer instanceof TileDownloadLayer) {
            ((TileDownloadLayer) this.layer).onPause();
        }
        super.onPause();
    }

}
