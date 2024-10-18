package de.storchp.opentracks.osmplugin.map

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.documentfile.provider.DocumentFile
import de.storchp.opentracks.osmplugin.BaseActivity
import de.storchp.opentracks.osmplugin.BuildConfig
import de.storchp.opentracks.osmplugin.MainActivity
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.dashboardapi.APIConstants
import de.storchp.opentracks.osmplugin.dashboardapi.TrackReader
import de.storchp.opentracks.osmplugin.dashboardapi.TrackpointReader
import de.storchp.opentracks.osmplugin.dashboardapi.TrackpointsBySegments
import de.storchp.opentracks.osmplugin.dashboardapi.WaypointReader
import de.storchp.opentracks.osmplugin.databinding.ActivityMapsBinding
import de.storchp.opentracks.osmplugin.map.MapsActivity.OpenTracksContentObserver
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils
import de.storchp.opentracks.osmplugin.utils.TrackpointsDebug
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.oscim.android.MapPreferences
import org.oscim.backend.AssetAdapter
import org.oscim.backend.CanvasAdapter
import org.oscim.core.BoundingBox
import org.oscim.core.GeoPoint
import org.oscim.event.Gesture
import org.oscim.event.Gesture.LongPress
import org.oscim.event.GestureListener
import org.oscim.event.MotionEvent
import org.oscim.layers.GroupLayer
import org.oscim.layers.Layer
import org.oscim.layers.PathLayer
import org.oscim.layers.marker.ItemizedLayer
import org.oscim.layers.marker.ItemizedLayer.OnItemGestureListener
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol.HotspotPlace
import org.oscim.layers.tile.bitmap.BitmapTileLayer
import org.oscim.layers.tile.buildings.BuildingLayer
import org.oscim.layers.tile.vector.labeling.LabelLayer
import org.oscim.map.Map
import org.oscim.renderer.GLViewport
import org.oscim.scalebar.DefaultMapScaleBar
import org.oscim.scalebar.ImperialUnitAdapter
import org.oscim.scalebar.MapScaleBar
import org.oscim.scalebar.MapScaleBarLayer
import org.oscim.scalebar.MetricUnitAdapter
import org.oscim.theme.IRenderTheme
import org.oscim.theme.StreamRenderTheme
import org.oscim.theme.ThemeFile
import org.oscim.theme.ZipRenderTheme
import org.oscim.theme.ZipXmlThemeResourceProvider
import org.oscim.tiling.source.OkHttpEngine.OkHttpFactory
import org.oscim.tiling.source.bitmap.DefaultSources
import org.oscim.tiling.source.mapfile.MapFileTileSource
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.lang.RuntimeException
import java.util.ArrayList
import java.util.Collections
import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream


private val TAG: String = MapsActivity::class.java.getSimpleName()
const val EXTRA_MARKER_ID: String = "marker_id"
const val EXTRA_TRACK_ID: String = "track_id"
const val EXTRA_LOCATION: String = "location"
private const val MAP_DEFAULT_ZOOM_LEVEL = 12
private const val EXTRAS_PROTOCOL_VERSION = "PROTOCOL_VERSION"
private const val EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK =
    "EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK"
private const val EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON"
private const val EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOULD_KEEP_SCREEN_ON"
private const val EXTRAS_SHOW_FULLSCREEN = "EXTRAS_SHOULD_FULLSCREEN"

open class MapsActivity : BaseActivity(), OnItemGestureListener<MarkerInterface?> {
    private var isOpenTracksRecordingThisTrack = false
    private lateinit var binding: ActivityMapsBinding
    private lateinit var map: Map
    private lateinit var mapPreferences: MapPreferences
    private var renderTheme: IRenderTheme? = null
    private var boundingBox: BoundingBox? = null
    private var polylinesLayer: GroupLayer? = null
    private var waypointsLayer: ItemizedLayer? = null
    private var lastWaypointId: Long = 0
    private var lastTrackPointId: Long = 0
    private var lastTrackId: Long = 0
    private var trackColor = 0
    private var polyline: PathLayer? = null
    private var endMarker: MarkerItem? = null
    private var colorCreator: StyleColorCreator? = null
    private var startPos: GeoPoint? = null
    private var endPos: GeoPoint? = null
    private var fullscreenMode = false
    private var movementDirection = MovementDirection()
    private var mapMode: MapMode = MapMode.NORTH
    private var contentObserver: OpenTracksContentObserver? = null
    private var tracksUri: Uri? = null
    private var trackpointsUri: Uri? = null
    private var waypointsUri: Uri? = null
    private var strokeWidth = 0
    private var protocolVersion = 1
    private var trackpointsDebug: TrackpointsDebug? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        strokeWidth = PreferencesUtils.getStrokeWidth()
        mapMode = PreferencesUtils.getMapMode()

        map = binding.map.mapView.map()
        mapPreferences = MapPreferences(MapsActivity::class.java.getName(), this)

        setSupportActionBar(binding.toolbar.mapsToolbar)

        createMapViews()
        createLayers()
        map.getMapPosition().setZoomLevel(MAP_DEFAULT_ZOOM_LEVEL)

        binding.map.fullscreenButton.setOnClickListener(View.OnClickListener { v: View? -> switchFullscreen() })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateUp()
            }
        })

        // Get the intent that started this activity
        val intent = getIntent()
        if (intent != null) {
            onNewIntent(intent)
        }
    }

    private inner class MapEventsReceiver(map: Map?) : Layer(map), GestureListener {
        override fun onGesture(g: Gesture?, e: MotionEvent): Boolean {
            if (g is LongPress && lastTrackId > 0) {
                AlertDialog.Builder(this@MapsActivity)
                    .setIcon(R.drawable.ic_logo_color_24dp)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.add_marker_to_open_tracks)
                    .setPositiveButton(
                        android.R.string.ok,
                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                            startOpenTracksToAddNewMarker(e)
                        })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show()
            }

            return false
        }
    }

    private fun startOpenTracksToAddNewMarker(event: MotionEvent) {
        try {
            val geoPoint = map.viewport().fromScreenPoint(event.x, event.y)
            val intent = Intent("de.dennisguse.opentracks.CreateMarker")
            intent.putExtra(EXTRA_TRACK_ID, lastTrackId)
            intent.putExtra(EXTRA_LOCATION, MapUtils.toLocation(geoPoint))
            startActivity(intent)
        } catch (ex: Exception) {
            Log.e(TAG, "Can't send trackpoint to OpenTracks", ex)
        }
    }

    private fun switchFullscreen() {
        showFullscreen(!fullscreenMode)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        resetMapData()

        if (APIConstants.ACTION_DASHBOARD == intent.action) {
            val uris =
                intent.getParcelableArrayListExtra<Uri>(APIConstants.ACTION_DASHBOARD_PAYLOAD)!!
            protocolVersion = intent.getIntExtra(EXTRAS_PROTOCOL_VERSION, 1)
            tracksUri = APIConstants.getTracksUri(uris)
            trackpointsUri = APIConstants.getTrackpointsUri(uris)
            waypointsUri = APIConstants.getWaypointsUri(uris)
            keepScreenOn(intent.getBooleanExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, false))
            showOnLockScreen(intent.getBooleanExtra(EXTRAS_SHOW_WHEN_LOCKED, false))
            showFullscreen(intent.getBooleanExtra(EXTRAS_SHOW_FULLSCREEN, false))
            isOpenTracksRecordingThisTrack =
                intent.getBooleanExtra(EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, false)

            trackpointsUri?.let { readTrackpoints(it, false, protocolVersion) }
            readTracks(tracksUri!!)
            waypointsUri?.let { readWaypoints(it) }
        } else if ("geo" == intent.scheme && intent.data != null) {
            WaypointReader.fromGeoUri(intent.data.toString())
                ?.let { waypoint ->
                    val marker = MapUtils.createTappableMarker(this, waypoint)
                    waypointsLayer!!.addItem(marker)
                    val pos = map.getMapPosition()
                        .setPosition(waypoint.latLong)
                        .setZoomLevel(15)
                    map.animator().animateTo(pos)
                }
        }
    }

    private inner class OpenTracksContentObserver(
        private val tracksUri: Uri,
        private val trackpointsUri: Uri,
        private val waypointsUri: Uri,
        private val protocolVersion: Int
    ) : ContentObserver(Handler()) {

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (uri == null) {
                return  // nothing can be done without an uri
            }
            if (tracksUri.toString().startsWith(uri.toString())) {
                readTracks(tracksUri)
            } else if (trackpointsUri.toString().startsWith(uri.toString())) {
                readTrackpoints(trackpointsUri, true, protocolVersion)
            } else if (waypointsUri.toString().startsWith(uri.toString())) {
                readWaypoints(waypointsUri)
            }
        }
    }

    private fun showFullscreen(showFullscreen: Boolean) {
        this.fullscreenMode = showFullscreen
        val decorView = window.decorView
        var uiOptions = decorView.systemUiVisibility
        if (showFullscreen) {
            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_FULLSCREEN
            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE
            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            binding.map.fullscreenButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_baseline_fullscreen_exit_48
                )
            )
        } else {
            uiOptions = uiOptions and View.SYSTEM_UI_FLAG_FULLSCREEN.inv()
            uiOptions = uiOptions and View.SYSTEM_UI_FLAG_IMMERSIVE.inv()
            uiOptions = uiOptions and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv()
            binding.map.fullscreenButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_baseline_fullscreen_48
                )
            )
        }
        binding.toolbar.mapsToolbar.visibility = if (showFullscreen) View.GONE else View.VISIBLE
        decorView.systemUiVisibility = uiOptions
    }

    fun navigateUp() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            && isOpenTracksRecordingThisTrack
            && PreferencesUtils.isPipEnabled()
        ) {
            enterPictureInPictureMode()
        } else {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu, true)
        return true
    }

    /**
     * Template method to create the map views.
     */
    protected fun createMapViews() {
        binding.map.mapView.isClickable = true
    }

    protected fun getRenderTheme(): ThemeFile? {
        val mapTheme = PreferencesUtils.getMapThemeUri()
        if (mapTheme == null) {
            return null
        }
        try {
            if ("file" == mapTheme.scheme) {
                val themeFile = File(mapTheme.path!!)
                if (themeFile.exists() && themeFile.getName().endsWith(".zip")) {
                    var themeFileUri = Uri.fromFile(themeFile)
                    val fragment = mapTheme.fragment
                    if (fragment != null) {
                        themeFileUri = themeFileUri.buildUpon().fragment(null).build()
                    } else {
                        throw RuntimeException("Fragment missing, which indicates the theme inside the zip file")
                    }
                    return ZipRenderTheme(
                        fragment,
                        ZipXmlThemeResourceProvider(
                            ZipInputStream(
                                BufferedInputStream(
                                    contentResolver.openInputStream(themeFileUri)
                                )
                            )
                        )
                    )
                }
                return StreamRenderTheme("/assets/", FileInputStream(themeFile))
            } else {
                val renderThemeFile: DocumentFile? =
                    checkNotNull(DocumentFile.fromSingleUri(application, mapTheme))
                var themeFileUri = renderThemeFile!!.uri
                if (Objects.requireNonNull<String?>(
                        renderThemeFile.name,
                        "Theme files must have a name"
                    ).endsWith(".zip")
                ) {
                    val fragment = themeFileUri.fragment
                    if (fragment != null) {
                        themeFileUri = themeFileUri.buildUpon().fragment(null).build()
                    } else {
                        throw RuntimeException("Fragment missing, which indicates the theme inside the zip file")
                    }
                    return ZipRenderTheme(
                        fragment,
                        ZipXmlThemeResourceProvider(
                            ZipInputStream(
                                BufferedInputStream(
                                    contentResolver.openInputStream(themeFileUri)
                                )
                            )
                        )
                    )
                }
                return StreamRenderTheme(
                    "/assets/",
                    contentResolver.openInputStream(themeFileUri)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading theme $mapTheme", e)
        }
        return null
    }

    protected fun getMapFile(): MultiMapFileTileSource? {
        val tileSource = MultiMapFileTileSource()
        val mapFiles = PreferencesUtils.getMapUris()
        if (mapFiles.isEmpty()) {
            return null
        }
        val mapsCount = AtomicInteger(0)
        mapFiles
            .filter { uri -> DocumentFile.isDocumentUri(this, uri) }
            .mapNotNull { uri -> DocumentFile.fromSingleUri(this, uri) }
            .filter { documentFile -> documentFile.canRead() }
            .forEach { documentFile ->
                readMapFile(
                    tileSource,
                    mapsCount,
                    documentFile
                )
            }

        mapFiles
            .filter { uri -> "file" == uri.scheme }
            .map { uri -> File(uri.path!!) }
            .filter { file -> file.exists() }
            .forEach { file: File -> readMapFile(tileSource, mapsCount, file) }

        if (mapsCount.get() == 0 && mapFiles.isNotEmpty()) {
            Toast.makeText(
                this,
                R.string.error_loading_offline_map,
                Toast.LENGTH_LONG
            ).show()
        }

        return if (mapsCount.get() > 0) tileSource else null
    }

    private fun readMapFile(
        mapDataStore: MultiMapFileTileSource,
        mapsCount: AtomicInteger,
        documentFile: DocumentFile
    ) {
        try {
            val inputStream =
                contentResolver.openInputStream(documentFile.uri) as FileInputStream?
            val tileSource = MapFileTileSource()
            tileSource.setMapFileInputStream(inputStream)
            mapDataStore.add(tileSource)
            mapsCount.getAndIncrement()
        } catch (e: Exception) {
            Log.e(TAG, "Can't open mapFile", e)
        }
    }

    private fun readMapFile(
        mapDataStore: MultiMapFileTileSource,
        mapsCount: AtomicInteger,
        file: File
    ) {
        try {
            val tileSource = MapFileTileSource()
            tileSource.setMapFile(file.path)
            mapDataStore.add(tileSource)
            mapsCount.getAndIncrement()
        } catch (e: Exception) {
            Log.e(TAG, "Can't open mapFile", e)
        }
    }

    protected fun loadTheme() {
        if (renderTheme != null) {
            renderTheme!!.dispose()
        }
        renderTheme =
            map.setTheme(StreamRenderTheme("", AssetAdapter.readFileAsStream("vtm/vtmstyle.xml")))
    }

    protected fun createLayers() {
        val mapFile = getMapFile()
        map.layers().add(MapEventsReceiver(map))

        if (mapFile != null) {
            val tileLayer = map.setBaseMap(mapFile)
            loadTheme()

            map.layers().add(BuildingLayer(map, tileLayer))
            map.layers().add(LabelLayer(map, tileLayer))

            val mapScaleBar = DefaultMapScaleBar(map)
            mapScaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.BOTH)
            mapScaleBar.setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE)
            mapScaleBar.setSecondaryDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE)
            mapScaleBar.setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_LEFT)

            val mapScaleBarLayer = MapScaleBarLayer(map, mapScaleBar)
            val renderer = mapScaleBarLayer.renderer
            renderer.setPosition(GLViewport.Position.BOTTOM_LEFT)
            renderer.setOffset(5 * CanvasAdapter.getScale(), 0f)
            map.layers().add(mapScaleBarLayer)

            val themeFile = getRenderTheme()
            if (themeFile != null) {
                map.setTheme(themeFile)
            }
        } else if (BuildConfig.offline) {
            AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setMessage(R.string.no_map_configured)
                .setPositiveButton(android.R.string.ok, null)
                .create().show()
        } else if (PreferencesUtils.getOnlineMapConsent()) {
            setOnlineTileLayer()
        } else {
            showOnlineMapConsent()
        }
    }

    private fun setOnlineTileLayer() {
        val tileSource = DefaultSources.OPENSTREETMAP.build()
        val builder = OkHttpClient.Builder()
        val cacheDirectory = File(externalCacheDir, "tiles")
        val cacheSize = 10 * 1024 * 1024 // 10 MB
        val cache = Cache(cacheDirectory, cacheSize.toLong())
        builder.cache(cache)

        tileSource.setHttpEngine(OkHttpFactory(builder))
        tileSource.setHttpRequestHeaders(
            Collections.singletonMap<String?, String?>(
                "User-Agent",
                getString(R.string.app_name) + ":" + BuildConfig.APPLICATION_ID
            )
        )

        val bitmapLayer = BitmapTileLayer(map, tileSource)
        map.layers().add(bitmapLayer)
    }

    private fun showOnlineMapConsent() {
        val message =
            SpannableString(getString(R.string.online_map_consent))
        Linkify.addLinks(message, Linkify.ALL)

        val dialog = AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_logo_color_24dp)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(
                android.R.string.ok,
                DialogInterface.OnClickListener { dialog1: DialogInterface?, which: Int ->
                    PreferencesUtils.setOnlineMapConsent(true)
                    recreate()
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.show()
        (Objects.requireNonNull<Any?>(
            dialog.findViewById<View?>(R.id.message),
            "An AlertDialog must have a TextView with id.message"
        ) as TextView).movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Android Activity life cycle method.
     */
    override fun onDestroy() {
        binding.map.mapView.onDestroy()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.map_info) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun readTrackpoints(data: Uri, update: Boolean, protocolVersion: Int) {
        Log.i(TAG, "Loading trackpoints from $data")

        synchronized(map.layers()) {
            val showPauseMarkers = PreferencesUtils.isShowPauseMarkers()
            val latLongs = mutableListOf<GeoPoint>()
            val tolerance = PreferencesUtils.getTrackSmoothingTolerance()

            try {
                val trackpointsBySegments: TrackpointsBySegments =
                    TrackpointReader.readTrackpointsBySegments(
                        contentResolver,
                        data,
                        lastTrackPointId,
                        protocolVersion
                    )
                if (trackpointsBySegments.isEmpty()) {
                    Log.d(TAG, "No new trackpoints received")
                    return
                }

                val average = trackpointsBySegments.calcAverageSpeed()
                val maxSpeed = trackpointsBySegments.calcMaxSpeed()
                val averageToMaxSpeed = maxSpeed - average
                var trackColorMode = PreferencesUtils.getTrackColorMode()
                if (isOpenTracksRecordingThisTrack && !trackColorMode.supportsLiveTrack) {
                    trackColorMode = DEFAULT_TRACK_COLOR_MORE
                }

                trackpointsBySegments.segments.map { trackpoints ->
                    if (!update) {
                        polyline = null // cut polyline on new segment
                        if (tolerance > 0) { // smooth track
                            return@map MapUtils.decimate(tolerance, trackpoints)
                        }
                    }
                    return@map trackpoints
                }.forEach { trackpoints ->
                    trackpoints.forEach { trackpoint ->
                        lastTrackPointId = trackpoint.id

                        if (trackpoint.trackId != lastTrackId) {
                            if (trackColorMode == TrackColorMode.BY_TRACK) {
                                trackColor = colorCreator!!.nextColor()
                            }
                            lastTrackId = trackpoint.trackId
                            polyline = null // reset current polyline when trackId changes
                            startPos = null
                            endPos = null
                        }

                        if (trackColorMode == TrackColorMode.BY_SPEED) {
                            trackColor = MapUtils.getTrackColorBySpeed(
                                average,
                                averageToMaxSpeed,
                                trackpoint
                            )
                            polyline = addNewPolyline(trackColor)
                            if (endPos != null) {
                                polyline!!.addPoint(endPos)
                            } else if (startPos != null) {
                                polyline!!.addPoint(startPos)
                            }
                        } else {
                            if (polyline == null) {
                                Log.d(TAG, "Continue new segment.")
                                polyline = addNewPolyline(trackColor)
                            }
                        }

                        endPos = trackpoint.latLong
                        polyline!!.addPoint(endPos)
                        movementDirection.updatePos(endPos)

                        if (trackpoint.isPause && showPauseMarkers) {
                            val marker = MapUtils.createPauseMarker(this, trackpoint.latLong)
                            waypointsLayer!!.addItem(marker)
                        }

                        if (!update) {
                            latLongs.add(endPos!!)
                        }

                        if (startPos == null) {
                            startPos = endPos
                        }
                    }
                    trackpointsBySegments.debug.trackpointsDrawn += trackpoints.size
                }
                trackpointsDebug!!.add(trackpointsBySegments.debug)
            } catch (e: SecurityException) {
                Toast.makeText(
                    this,
                    getString(
                        R.string.error_reading_trackpoints,
                        e.message
                    ),
                    Toast.LENGTH_LONG
                ).show()
                return
            } catch (e: Exception) {
                throw RuntimeException("Error reading trackpoints", e)
            }

            Log.d(TAG, "Last trackpointId=$lastTrackPointId")

            if (endPos != null) {
                setEndMarker(endPos!!)
            }

            var myPos: GeoPoint? = null
            if (update && endPos != null) {
                myPos = endPos
                map.render()
            } else if (latLongs.isNotEmpty()) {
                boundingBox = BoundingBox(latLongs).extendMargin(1.2f).also {
                    myPos = it.getCenterPoint()
                }
            }

            if (myPos != null) {
                updateMapPositionAndRotation(myPos)
            }
            updateDebugTrackPoints()
        }
    }

    private fun resetMapData() {
        unregisterContentObserver()

        tracksUri = null
        trackpointsUri = null
        waypointsUri = null

        val layers = map.layers()

        // polylines
        if (polylinesLayer != null) {
            layers.remove(polylinesLayer)
        }
        polylinesLayer = GroupLayer(map)
        layers.add(polylinesLayer)

        // tracks
        lastTrackId = 0
        lastTrackPointId = 0
        colorCreator = StyleColorCreator()
        trackColor = colorCreator!!.nextColor()
        polyline = null
        startPos = null
        endPos = null
        endMarker = null
        boundingBox = null
        movementDirection = MovementDirection()
        trackpointsDebug = TrackpointsDebug()

        // waypoints
        if (waypointsLayer != null) {
            layers.remove(waypointsLayer)
        }

        waypointsLayer = createWaypointsLayer()
        map.layers().add(waypointsLayer)
        lastWaypointId = 0
        mapPreferences.clear()
    }

    fun updateDebugTrackPoints() {
        if (PreferencesUtils.isDebugTrackPoints()) {
            binding.map.trackpointsDebugInfo.text = getString(
                R.string.debug_trackpoints_info,
                trackpointsDebug!!.trackpointsReceived,
                trackpointsDebug!!.trackpointsInvalid,
                trackpointsDebug!!.trackpointsDrawn,
                trackpointsDebug!!.trackpointsPause,
                trackpointsDebug!!.segments,
                protocolVersion
            )
        } else {
            binding.map.trackpointsDebugInfo.text = ""
        }
    }

    private fun setEndMarker(endPos: GeoPoint) {
        synchronized(map.layers()) {
            endMarker?.let {
                it.geoPoint = endPos
                it.setRotation(MapUtils.rotateWith(mapMode, movementDirection))
                waypointsLayer!!.populate()
                map.render()
            } ?: {
                endMarker = MarkerItem(endPos.toString(), "", endPos)
                val symbol = MapUtils.createMarkerSymbol(
                    this,
                    R.drawable.ic_compass,
                    false,
                    HotspotPlace.CENTER
                )
                endMarker!!.marker = symbol
                endMarker!!.setRotation(MapUtils.rotateWith(mapMode, movementDirection))
                waypointsLayer!!.addItem(endMarker)
            }
        }
    }

    private fun addNewPolyline(trackColor: Int): PathLayer? {
        polyline = PathLayer(map, trackColor, strokeWidth.toFloat())
        polylinesLayer!!.layers.add(polyline)
        return this.polyline
    }

    private fun readWaypoints(data: Uri) {
        Log.i(TAG, "Loading waypoints from $data")

        try {
            WaypointReader.readWaypoints(contentResolver, data, lastWaypointId).forEach {
                lastWaypointId = it.id
                val marker = MapUtils.createTappableMarker(this, it)
                waypointsLayer!!.addItem(marker)
            }
        } catch (_: SecurityException) {
            Log.w(TAG, "No permission to read waypoints")
        } catch (e: Exception) {
            Log.e(TAG, "Reading waypoints failed", e)
        }
    }

    private fun createWaypointsLayer(): ItemizedLayer {
        val symbol = MapUtils.createPushpinSymbol(this)
        return ItemizedLayer(map, ArrayList<MarkerInterface?>(), symbol, this)
    }

    override fun onItemSingleTapUp(index: Int, item: MarkerInterface?): Boolean {
        val markerItem = item as MarkerItem
        if (markerItem.uid != null) {
            try {
                val intent = Intent("de.dennisguse.opentracks.MarkerDetails")
                intent.putExtra(EXTRA_MARKER_ID, markerItem.getUid() as Long?)
                startActivity(intent)
            } catch (ex: Exception) {
                Log.e(
                    TAG,
                    "Can't open OpenTracks MarkerDetails for marker " + markerItem.uid,
                    ex
                )
            }
        }
        return true
    }

    override fun onItemLongPress(index: Int, item: MarkerInterface?): Boolean {
        return false
    }

    private fun readTracks(data: Uri) {
        val tracks = TrackReader.readTracks(contentResolver, data)
        if (tracks.isNotEmpty()) {
            val statistics = TrackStatistics(tracks)
            removeStatisticElements()
            PreferencesUtils.getStatisticElements()
                .sortedBy { it.ordinal }
                .forEach { addStatisticElement(it.getText(this, statistics)) }
        }
    }

    private fun removeStatisticElements() {
        binding.map.statisticsLayout.children
            .filterIsInstance<TextView>()
            .forEach { view ->
                binding.map.statisticsLayout.removeView(view)
                binding.map.statistics.removeView(view)
            }
    }

    private fun addStatisticElement(text: String?) {
        val textView = TextView(this).apply {
            setId(View.generateViewId())
            this.text = Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
            setTextColor(getColor(R.color.track_statistic))
            setTextSize(TypedValue.COMPLEX_UNIT_PT, 10f)
        }
        binding.map.statisticsLayout.addView(textView)
        binding.map.statistics.addView(textView)
    }

    public override fun onResume() {
        super.onResume()

        mapPreferences.load(map)
        binding.map.mapView.onResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && boundingBox != null) {
            val mapPos = map.getMapPosition()
            mapPos.setByBoundingBox(boundingBox, map.width, map.height)
            mapPos.setBearing(mapMode.getHeading(movementDirection))
            map.animator().animateTo(mapPos)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        val visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
        binding.toolbar.mapsToolbar.visibility = visibility
        binding.map.fullscreenButton.setVisibility(visibility)
        binding.map.statistics.setVisibility(visibility)
    }

    private fun isPiPMode(): Boolean {
        return isInPictureInPictureMode
    }

    override fun onPause() {
        if (!isPiPMode()) {
            mapPreferences.save(map)
            binding.map.mapView.onPause()
        }
        super.onPause()
    }

    override fun onStart() {
        super.onStart()

        if (tracksUri != null && trackpointsUri != null && waypointsUri != null) {
            contentObserver = OpenTracksContentObserver(
                tracksUri!!,
                trackpointsUri!!,
                waypointsUri!!,
                protocolVersion
            )
            try {
                contentResolver.registerContentObserver(tracksUri!!, false, contentObserver!!)
                contentResolver.registerContentObserver(
                    trackpointsUri!!,
                    false,
                    contentObserver!!
                )
                if (waypointsUri != null) {
                    contentResolver.registerContentObserver(
                        waypointsUri!!,
                        false,
                        contentObserver!!
                    )
                }
            } catch (se: SecurityException) {
                Log.e(TAG, "Error on registering OpenTracksContentObserver", se)
                Toast.makeText(
                    this,
                    R.string.error_reg_content_observer,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onStop() {
        unregisterContentObserver()
        super.onStop()
    }


    private fun unregisterContentObserver() {
        if (contentObserver != null) {
            Log.d(TAG, "unregister content observer")
            contentResolver.unregisterContentObserver(contentObserver!!)
            contentObserver = null
        }
    }

    private fun updateMapPositionAndRotation(myPos: GeoPoint?) {
        val newPos = map.getMapPosition().setPosition(myPos)
            .setBearing(mapMode.getHeading(movementDirection))
        map.animator().animateTo(newPos)
    }

}
