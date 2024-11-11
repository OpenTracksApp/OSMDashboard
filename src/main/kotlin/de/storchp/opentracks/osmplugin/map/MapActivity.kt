package de.storchp.opentracks.osmplugin.map

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
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
import de.storchp.opentracks.osmplugin.dashboardapi.DashboardReader
import de.storchp.opentracks.osmplugin.dashboardapi.UpdateTrackStatistics
import de.storchp.opentracks.osmplugin.dashboardapi.UpdateTrackpointsDebug
import de.storchp.opentracks.osmplugin.dashboardapi.WaypointReader
import de.storchp.opentracks.osmplugin.dashboardapi.isDashboardAction
import de.storchp.opentracks.osmplugin.dashboardapi.isGeoIntent
import de.storchp.opentracks.osmplugin.databinding.ActivityMapsBinding
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.oscim.android.MapPreferences
import org.oscim.backend.AssetAdapter
import org.oscim.backend.CanvasAdapter
import org.oscim.event.Gesture
import org.oscim.event.Gesture.LongPress
import org.oscim.event.GestureListener
import org.oscim.event.MotionEvent
import org.oscim.layers.GroupLayer
import org.oscim.layers.Layer
import org.oscim.layers.marker.ItemizedLayer
import org.oscim.layers.marker.ItemizedLayer.OnItemGestureListener
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem
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


open class MapsActivity : BaseActivity(), OnItemGestureListener<MarkerInterface?> {
    private lateinit var binding: ActivityMapsBinding
    private lateinit var map: Map
    private lateinit var mapPreferences: MapPreferences
    private var mapData: MapData? = null
    private var renderTheme: IRenderTheme? = null
    private var polylinesLayer: GroupLayer? = null
    private var waypointsLayer: ItemizedLayer? = null
    private var dashboardReader: DashboardReader? = null
    private var fullscreenMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

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
            if (g is LongPress && dashboardReader?.hasTrackId() == true) {
                AlertDialog.Builder(this@MapsActivity)
                    .setIcon(R.drawable.ic_logo_color_24dp)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.add_marker_to_open_tracks)
                    .setPositiveButton(
                        android.R.string.ok,
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            startOpenTracksToAddNewMarker(e, dashboardReader!!.lastTrackId)
                        })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show()
            }

            return false
        }
    }

    private fun startOpenTracksToAddNewMarker(event: MotionEvent, lastTrackId: Long) {
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

        if (intent.isDashboardAction()) {
            dashboardReader =
                DashboardReader(
                    intent,
                    contentResolver,
                    mapData!!,
                    updateTrackStatistics,
                    updateDebugTrackPoints,
                ).apply {
                    keepScreenOn(keepScreenOn)
                    showOnLockScreen(showOnLockScreen)
                    showFullscreen(showFullscreen)
                }
        } else if (intent.isGeoIntent()) {
            WaypointReader.fromGeoIntent(intent, mapData!!)
        }
    }

    private val updateTrackStatistics: UpdateTrackStatistics =
        { trackStatistics: TrackStatistics? ->
            removeStatisticElements()
            if (trackStatistics != null) {
                PreferencesUtils.getStatisticElements()
                    .sortedBy { it.ordinal }
                    .forEach { addStatisticElement(it.getText(this, trackStatistics)) }
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
            && dashboardReader?.isOpenTracksRecordingThisTrack == true
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

            val mapScaleBar = DefaultMapScaleBar(map).apply {
                setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.BOTH)
                setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE)
                setSecondaryDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE)
                setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_LEFT)
            }

            val mapScaleBarLayer = MapScaleBarLayer(map, mapScaleBar).apply {
                renderer.apply {
                    setPosition(GLViewport.Position.BOTTOM_LEFT)
                    setOffset(5 * CanvasAdapter.getScale(), 0f)
                }
            }
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
                DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                    PreferencesUtils.setOnlineMapConsent(true)
                    recreate()
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.show()
        dialog.findViewById<TextView?>(R.id.message)?.movementMethod =
            LinkMovementMethod.getInstance()
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

    private fun resetMapData() {
        dashboardReader?.unregisterContentObserver()

        val layers = map.layers()

        // polylines
        if (polylinesLayer != null) {
            layers.remove(polylinesLayer)
        }
        polylinesLayer = GroupLayer(map)
        layers.add(polylinesLayer)

        // waypoints
        if (waypointsLayer != null) {
            layers.remove(waypointsLayer)
        }
        waypointsLayer = createWaypointsLayer()
        map.layers().add(waypointsLayer)

        dashboardReader = null
        mapData = MapData(
            map = map,
            polylinesLayer = polylinesLayer!!,
            waypointsLayer = waypointsLayer!!,
            strokeWidth = PreferencesUtils.getStrokeWidth(),
            mapMode = PreferencesUtils.getMapMode(),
            pauseMarkerSymbol = MapUtils.createPauseMarkerSymbol(this),
            waypointMarkerSymbol = MapUtils.createPushpinSymbol(this),
            compassMarkerSymbol = MapUtils.createCompassMarkerSymbol(this),
        )

        mapPreferences.clear()
    }

    val updateDebugTrackPoints: UpdateTrackpointsDebug = { trackpointsDebug ->
        if (PreferencesUtils.isDebugTrackPoints()) {
            binding.map.trackpointsDebugInfo.text = getString(
                R.string.debug_trackpoints_info,
                trackpointsDebug.trackpointsReceived,
                trackpointsDebug.trackpointsInvalid,
                trackpointsDebug.trackpointsDrawn,
                trackpointsDebug.trackpointsPause,
                trackpointsDebug.segments,
                trackpointsDebug.protocolVersion,
            )
        } else {
            binding.map.trackpointsDebugInfo.text = ""
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
        if (!hasFocus) return

        mapData?.let { data ->
            data.boundingBox?.let { boundingBox ->
                val mapPos = map.getMapPosition()
                mapPos.setByBoundingBox(boundingBox, map.width, map.height)
                mapPos.setBearing(data.getBearing())
                map.animator().animateTo(mapPos)
            }
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
        dashboardReader?.startContentObserver()
    }

    override fun onStop() {
        dashboardReader?.unregisterContentObserver()
        super.onStop()
    }

}
