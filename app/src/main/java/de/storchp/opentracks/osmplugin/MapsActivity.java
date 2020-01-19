package de.storchp.opentracks.osmplugin;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

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
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.view.Menu.NONE;

public class MapsActivity extends AppCompatActivity implements DirectoryChooserFragment.OnFragmentInteractionListener {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final String TAG_MAP_DIR = MapsActivity.class.getSimpleName() + ".MapDirChooser";
    private static final String TAG_THEME_DIR = MapsActivity.class.getSimpleName() + ".ThemeDirChooser";

    private static final int REQUEST_MAP_DIRECTORY = 1;
    private static final int REQUEST_THEME_DIRECTORY = 2;

    private BaseApplication baseApplication;
    private MapsforgeMapView mapView;
    private Layer layer;
    private List<TileCache> tileCaches = new ArrayList<>();

    private DirectoryChooserFragment mDirectoryChooser;

    private List<Polyline> polylines = new ArrayList<>();
    private FixedPixelCircle startPoint;
    private FixedPixelCircle endPoint;
    private BoundingBox boundingBox;

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

        baseApplication = (BaseApplication) getApplication();

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
        return mapView.getModel().mapViewPosition.getZoomLevelMax();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        final String mapFileName = baseApplication.getMapFileName();

        MenuItem osmMapnick = menu.findItem(R.id.osm_mapnik);
        osmMapnick.setChecked(mapFileName == null);
        osmMapnick.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, null));

        SubMenu mapSubmenu = menu.findItem(R.id.maps_submenu).getSubMenu();

        final String mapDirectory = baseApplication.getMapDirectory();
        if (mapDirectory != null) {
            final File mapDirectoryFile = new File(mapDirectory);
            if (mapDirectoryFile.canRead() && mapDirectoryFile.isDirectory()) {
                File[] maps = mapDirectoryFile.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".map");
                    }
                });
                for (File map : maps) {
                    MenuItem mapItem = mapSubmenu.add(R.id.maps_group, NONE, NONE, map.getName());
                    mapItem.setChecked(map.getAbsolutePath().equals(mapFileName));
                    mapItem.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, mapDirectoryFile));
                }
            }
        }
        mapSubmenu.setGroupCheckable(R.id.maps_group, true, true);

        MenuItem mapFolder = mapSubmenu.add(R.string.map_folder);
        mapFolder.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_MAP_DIRECTORY);
                } else {
                    openMapDirectoryChooser();
                }
                return false;
            }
        });

        final String mapTheme = baseApplication.getMapTheme();
        final String mapThemeDirectory = baseApplication.getMapThemeDirectory();

        MenuItem defaultTheme = menu.findItem(R.id.default_theme);
        defaultTheme.setChecked(mapTheme == null);
        defaultTheme.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, null));
        SubMenu themeSubmenu = menu.findItem(R.id.themes_submenu).getSubMenu();

        if (mapThemeDirectory != null) {
            final File mapThemeDirectoryFile = new File(mapThemeDirectory);
            if (mapThemeDirectoryFile.canRead() && mapThemeDirectoryFile.isDirectory()) {
                File[] themes = mapThemeDirectoryFile.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        if (file.isFile() && file.getName().endsWith(".xml")) {
                            return true;
                        }
                        if (file.isDirectory()) {
                            File theme = new File(file, file.getName() + ".xml");
                            return theme.exists() && theme.canRead();
                        }
                        return false;
                    }

                });
                for (File theme : themes) {
                    String themeName = theme.getName();
                    MenuItem themeItem = themeSubmenu.add(R.id.themes_group, NONE, NONE, themeName);
                    themeItem.setChecked(theme.getAbsolutePath().equals(mapTheme));
                    themeItem.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, mapThemeDirectoryFile));
                }
            }
        }
        themeSubmenu.setGroupCheckable(R.id.themes_group, true, true);

        MenuItem themeFolder = themeSubmenu.add(R.string.theme_folder);
        themeFolder.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_MAP_DIRECTORY);
                } else {
                    openThemeDirectoryChooser();
                }
                return false;
            }
        });

        return true;
    }

    private void openDirectoryChooser(String dir, String tag) {
        if (dir == null) {
            dir = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("")
                .allowNewDirectoryNameModification(true)
                .allowReadOnlyDirectory(true)
                .initialDirectory(dir)
                .build();
        mDirectoryChooser = DirectoryChooserFragment.newInstance(config);

        mDirectoryChooser.show(getFragmentManager(), tag);
    }

    private void openMapDirectoryChooser() {
        openDirectoryChooser(baseApplication.getMapDirectory(), TAG_MAP_DIR);
    }

    private void openThemeDirectoryChooser() {
        openDirectoryChooser(baseApplication.getMapThemeDirectory(), TAG_THEME_DIR);
    }

    @Override
    public void onSelectDirectory(@NonNull String path) {
        if (mDirectoryChooser.getTag().equals(TAG_MAP_DIR)) {
            baseApplication.setMapDirectory(path);
        } else if (mDirectoryChooser.getTag().equals(TAG_THEME_DIR)) {
            baseApplication.setMapThemeDirectory(path);
        }
        mDirectoryChooser.dismiss();
        recreate();
    }

    @Override
    public void onCancelChooser() {
        mDirectoryChooser.dismiss();
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
                MapInfoFragment mapInfoFragment = new MapInfoFragment();
                mapInfoFragment.show(getSupportFragmentManager(), "Map Info Dialog");

                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void readTrackpoints(Uri data, boolean update) {
        // A "projection" defines the columns that will be returned for each row
        String[] projection =
                {
                        Constants.Trackpoints._ID,
                        Constants.Trackpoints.LATITUDE,
                        Constants.Trackpoints.LONGITUDE,
                        Constants.Trackpoints.TIME
                };

        Log.i(TAG, "Loading track from " + data);

        // Does a query against the table and returns a Cursor object
        final Cursor cursor = getContentResolver().query(
                data,
                projection,
                null,
                null,
                null);

        Layers layers = mapView.getLayerManager().getLayers();
        for (Polyline polyline : polylines) {
            layers.remove(polyline);
        }
        polylines.clear();

        Polyline polyline = newPolyline();

        double minLat = 0;
        double maxLat = 0;
        double minLon = 0;
        double maxLon = 0;

        LatLong startPos = null;
        LatLong endPos = null;
        boolean pause = false;

        while (cursor.moveToNext()) {
            double latitude = Double.parseDouble(cursor.getString(cursor.getColumnIndex(Constants.Trackpoints.LATITUDE))) / 1E6;
            double longitude = Double.parseDouble(cursor.getString(cursor.getColumnIndex(Constants.Trackpoints.LONGITUDE))) / 1E6;
            if (!Constants.isValidLocation(latitude, longitude)) {
                pause = latitude == Constants.Trackpoints.PAUSE_LATITUDE;
                if (pause && !polyline.getLatLongs().isEmpty()) {
                    layers.add(polyline);;
                    polylines.add(polyline);
                    Log.d(TAG, "Pause Trackpoint");
                }

                boolean resume = latitude == Constants.Trackpoints.RESUME_LATITUDE;
                if (resume) {
                    pause = false;
                    polyline = newPolyline();
                    Log.d(TAG, "Resume Trackpoint");
                }

                if (!pause && !resume) {
                    Log.d(TAG, "Got invalid coordinates: " + latitude + " " + longitude);
                }
                continue;
            }

            if (pause) {
                Log.d(TAG, "Ignoring trackpoint during pause: " + latitude + " " + longitude);
                continue;
            }

            Log.d(TAG, "Adding trackpoint: " + latitude + " " + longitude);
            LatLong latLong = new LatLong(latitude, longitude);
            polyline.addPoint(latLong);
            endPos = latLong;

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
            }
        }
        layers.add(polyline);
        polylines.add(polyline);

        if (startPos != null) {
            if (startPoint == null) {
                startPoint = new FixedPixelCircle(startPos, 10, createPaint(
                        AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN), 0,
                        Style.FILL), null);
                layers.add(startPoint);
            } else {
                startPoint.setLatLong(startPos);
            }
        }
        if (endPos != null && !endPos.equals(startPos)) {
            if (endPoint == null) {
                endPoint = new FixedPixelCircle(endPos, 10, createPaint(
                        AndroidGraphicFactory.INSTANCE.createColor(Color.RED), 0,
                        Style.FILL), null);
                layers.add(endPoint);
            } else {
                endPoint.setLatLong(endPos);
            }
        }

        LatLong myPos;
        if (update && endPos != null) {
            myPos = endPos;
        } else {
            myPos = new LatLong((minLat + maxLat) / 2, (minLon + maxLon) / 2);
        }

        mapView.setCenter(myPos);
        if (!update) {
            boundingBox = new BoundingBox(minLat, minLon, maxLat, maxLon);
        } else {
            boundingBox = null;
        }
    }

    private Polyline newPolyline() {
        return new Polyline(createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE),
                (int) (8 * mapView.getModel().displayModel.getScaleFactor()),
                Style.STROKE), AndroidGraphicFactory.INSTANCE);
    }

    private void readTrack(Uri data) {
        // A "projection" defines the columns that will be returned for each row
        String[] projection =
                {
                        Constants.Track.NAME,
                        Constants.Track.DESCRIPTION,
                        Constants.Track.CATEGORY,
                        Constants.Track.STARTTIME,
                        Constants.Track.STOPTIME,
                        Constants.Track.TOTALDISTANCE,
                        Constants.Track.TOTALTIME,
                        Constants.Track.MOVINGTIME,
                        Constants.Track.AVGSPEED,
                        Constants.Track.AVGMOVINGSPEED,
                        Constants.Track.MAXSPEED,
                        Constants.Track.MINELEVATION,
                        Constants.Track.MAXELEVATION,
                        Constants.Track.ELEVATIONGAIN

                };

        Log.i(TAG, "Loading track from " + data);

        // Does a query against the table and returns a Cursor object
        final Cursor cursor = getContentResolver().query(
                data,
                projection,
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
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
    }

    @Override
    protected void onStop() {
        super.onStop();
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
                    LatLongUtils.zoomForBounds(
                            dimension,
                            boundingBox,
                            this.mapView.getModel().displayModel.getTileSize())));
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_MAP_DIRECTORY) {
            Log.i(TAG, "Received response for external file permission request.");

            // Check if the required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission has been granted
                openMapDirectoryChooser();
            } else {
                //Permission not granted
                Toast.makeText(MapsActivity.this, R.string.grant_external_storage, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_THEME_DIRECTORY) {
            Log.i(TAG, "Received response for external file permission request.");

            // Check if the required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission has been granted
                openThemeDirectoryChooser();
            } else {
                //Permission not granted
                Toast.makeText(MapsActivity.this, R.string.grant_external_storage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static class MapMenuListener implements MenuItem.OnMenuItemClickListener {

        private WeakReference<MapsActivity> mapsActivityRef;

        private BaseApplication baseApplication;

        private File mapDirectory;

        private MapMenuListener(final MapsActivity mapsActivity, final BaseApplication baseApplication, final File mapDirectory) {
            this.mapsActivityRef = new WeakReference<>(mapsActivity);
            this.baseApplication = baseApplication;
            this.mapDirectory = mapDirectory;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.osm_mapnik) { // default Mapnik online tiles
                baseApplication.setMapFileName(null);
            } else {
                baseApplication.setMapFileName(new File(mapDirectory, item.getTitle().toString()).getAbsolutePath());
            }

            MapsActivity mapsActivity = mapsActivityRef.get();
            if (mapsActivity != null) {
                mapsActivity.recreate();
            }
            return false;
        }
    }

    private static class MapThemeMenuListener implements MenuItem.OnMenuItemClickListener {

        private WeakReference<MapsActivity> mapsActivityRef;

        private BaseApplication baseApplication;

        private File mapThemeDirectory;

        private MapThemeMenuListener(final MapsActivity mapsActivity, final BaseApplication baseApplication, final File mapThemeDirectory) {
            this.mapsActivityRef = new WeakReference<>(mapsActivity);
            this.baseApplication = baseApplication;
            this.mapThemeDirectory = mapThemeDirectory;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.default_theme) { // default theme
                baseApplication.setMapTheme(null);
            } else {
                baseApplication.setMapTheme(new File(mapThemeDirectory, item.getTitle().toString()).getAbsolutePath());
            }

            MapsActivity mapsActivity = mapsActivityRef.get();
            if (mapsActivity != null) {
                mapsActivity.recreate();
            }
            return false;
        }
    }

}

