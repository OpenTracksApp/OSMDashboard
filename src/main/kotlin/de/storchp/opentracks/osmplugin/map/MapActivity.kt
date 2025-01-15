package de.storchp.opentracks.osmplugin.map

import android.app.PictureInPictureParams
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.util.Rational
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import de.storchp.opentracks.osmplugin.BaseActivity
import de.storchp.opentracks.osmplugin.BuildConfig
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.databinding.ActivityMapsBinding
import de.storchp.opentracks.osmplugin.map.reader.DashboardReader
import de.storchp.opentracks.osmplugin.map.reader.GeoUriReader
import de.storchp.opentracks.osmplugin.map.reader.GpxReader
import de.storchp.opentracks.osmplugin.map.reader.MapDataReader
import de.storchp.opentracks.osmplugin.map.reader.UpdateTrackStatistics
import de.storchp.opentracks.osmplugin.map.reader.UpdateTrackpointsDebug
import de.storchp.opentracks.osmplugin.map.reader.isDashboardAction
import de.storchp.opentracks.osmplugin.map.reader.isGeoIntent
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
import org.oscim.layers.Layer
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
import java.util.Collections
import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream


private val TAG: String = MapsActivity::class.java.getSimpleName()
const val EXTRA_MARKER_ID: String = "marker_id"
const val EXTRA_TRACK_ID: String = "track_id"
const val EXTRA_LOCATION: String = "location"
private const val MAP_DEFAULT_ZOOM_LEVEL = 12


open class MapsActivity : BaseActivity(), OnItemGestureListener<MarkerInterface> {
    private lateinit var binding: ActivityMapsBinding
    private lateinit var map: Map
    private lateinit var mapPreferences: MapPreferences
    private var mapData: MapData? = null
    private var renderTheme: IRenderTheme? = null
    private var mapDataReader: MapDataReader? = null
    private var fullscreenMode = false
    private var keepPositionAfterDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.map.attribution) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }

        setContentView(binding.getRoot())

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        map = binding.map.mapView.map()
        mapPreferences = MapPreferences(MapsActivity::class.java.getName(), this)

        createMapViews()
        createLayers()
        map.getMapPosition().setZoomLevel(MAP_DEFAULT_ZOOM_LEVEL)

        binding.map.fullscreenButton.setOnClickListener(View.OnClickListener { v -> switchFullscreen() })
        binding.map.settingsButton.setOnClickListener(View.OnClickListener { v -> openSettings(null) })

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
            val trackId = mapDataReader?.lastTrackId
            if (g is LongPress && trackId != null) {
                AlertDialog.Builder(this@MapsActivity)
                    .setIcon(R.drawable.ic_logo_color_24dp)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.add_marker_to_open_tracks)
                    .setPositiveButton(
                        android.R.string.ok,
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            startOpenTracksToAddNewMarker(e, trackId)
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
            mapDataReader =
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
            mapDataReader = GeoUriReader(
                intent,
                mapData!!,
                updateTrackStatistics,
                updateDebugTrackPoints,
            )
        } else {
            val documentUris = buildList {
                if (intent.data != null) {
                    add(intent.data)
                } else {
                    val intentClipData = intent.clipData
                    if (intentClipData != null && intentClipData.itemCount > 0) {
                        for (i in 0..intentClipData.itemCount - 1) {
                            add(intentClipData.getItemAt(i).uri)
                        }
                    }
                }
            }.mapNotNull { it?.let { DocumentFile.fromSingleUri(application, it) } }

            if (documentUris.isNotEmpty()) {
                mapDataReader = GpxReader(
                    documentUris,
                    contentResolver,
                    mapData!!,
                    updateTrackStatistics,
                    updateDebugTrackPoints,
                )
            }
        }

        if (mapDataReader?.isRecording == true) {
            mapDataReader?.startContentObserver()
        }
    }

    private val updateTrackStatistics: UpdateTrackStatistics =
        { trackStatistics ->
            removeStatisticElements()
            if (trackStatistics != null) {
                PreferencesUtils.getStatisticElements()
                    .sortedBy { it.ordinal }
                    .forEach { addStatisticElement(it.getText(this, trackStatistics)) }
            }
        }

    @Suppress("DEPRECATION")
    private fun showFullscreen(showFullscreen: Boolean) {
        this.fullscreenMode = showFullscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = window.insetsController
            if (windowInsetsController != null) {
                if (showFullscreen) {
                    windowInsetsController.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    windowInsetsController.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                }
                setFullscreenButton(showFullscreen)
            }
        } else {
            val decorView = window.decorView
            var uiOptions = decorView.systemUiVisibility
            if (showFullscreen) {
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_FULLSCREEN
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            } else {
                uiOptions = uiOptions and View.SYSTEM_UI_FLAG_FULLSCREEN.inv()
                uiOptions = uiOptions and View.SYSTEM_UI_FLAG_IMMERSIVE.inv()
                uiOptions = uiOptions and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv()
            }
            decorView.systemUiVisibility = uiOptions
            setFullscreenButton(showFullscreen)
        }
    }

    private fun setFullscreenButton(showFullscreen: Boolean) {
        if (showFullscreen) {
            binding.map.fullscreenButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.ic_baseline_fullscreen_exit_48)
            )
        } else {
            binding.map.fullscreenButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.ic_baseline_fullscreen_48)
            )
        }
    }

    fun navigateUp() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            && mapDataReader?.isRecording == true
            && PreferencesUtils.isPipEnabled()
        ) {
            val pipParamsBuilder = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pipParamsBuilder.setAutoEnterEnabled(true)
            }

            enterPictureInPictureMode(pipParamsBuilder.build())
        } else {
            finish()
        }
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

        @Suppress("KotlinConstantConditions")
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
        Linkify.addLinks(message, Linkify.WEB_URLS)

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
        dialog.findViewById<TextView?>(android.R.id.message)
            ?.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Android Activity life cycle method.
     */
    override fun onDestroy() {
        mapDataReader?.unregisterContentObserver()
        binding.map.mapView.onDestroy()
        super.onDestroy()
    }

    private fun resetMapData() {
        mapDataReader?.unregisterContentObserver()
        mapData?.removeLayers()

        mapDataReader = null
        mapData = MapData(
            context = this,
            map = map,
            onItemGestureListener = this,
            strokeWidth = PreferencesUtils.getStrokeWidth(),
            mapMode = PreferencesUtils.getMapMode(),
            pauseMarkerSymbol = MapUtils.createPauseMarkerSymbol(this),
            waypointMarkerSymbol = MapUtils.createWaypointMarkerSymbol(this),
            compassMarkerSymbol = MapUtils.createCompassMarkerSymbol(this),
            startMarkerSymbol = MapUtils.createStartMarkerSymbol(this),
            endMarkerSymbol = MapUtils.createEndMarkerSymbol(this),
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

    override fun onItemSingleTapUp(index: Int, item: MarkerInterface): Boolean {
        val markerItem = item as MarkerItem

        val dialogBuilder = AlertDialog.Builder(this@MapsActivity)
            .setIcon(R.drawable.ic_logo_color_24dp)
            .setTitle(markerItem.title)
            .setMessage(markerItem.description)
            .setPositiveButton(android.R.string.ok, null)

        if (markerItem.uid != null) {
            dialogBuilder.setNeutralButton(
                R.string.open_in_open_tracks,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    try {
                        val intent = Intent("de.dennisguse.opentracks.MarkerDetails")
                        intent.putExtra(EXTRA_MARKER_ID, markerItem.uid as Long)
                        startActivity(intent)
                    } catch (ex: Exception) {
                        Toast.makeText(
                            this,
                            getString(R.string.error_starting_open_tracks, ex.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }

        dialogBuilder.create().show()
        keepPositionAfterDialog = true
        return true
    }

    override fun onItemLongPress(index: Int, item: MarkerInterface): Boolean {
        return false
    }

    private val statisticRefIds = mutableListOf<Int>()

    private fun removeStatisticElements() {
        statisticRefIds
            .forEach { id ->
                binding.map.statisticsLayout.removeView(binding.map.statisticsLayout.findViewById(id))
            }
        statisticRefIds.clear()
    }

    private fun addStatisticElement(text: String?) {
        val id = View.generateViewId()
        val textView = TextView(this).apply {
            this.id = id
            this.text = Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
            setTextColor(getColor(R.color.track_statistic))
            setTextSize(TypedValue.COMPLEX_UNIT_PT, 10f)
        }
        statisticRefIds.add(id)
        binding.map.statisticsLayout.addView(textView)
        binding.map.statisticsFlow.addView(textView)
    }

    public override fun onResume() {
        super.onResume()

        mapPreferences.load(map)
        binding.map.mapView.onResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        if (keepPositionAfterDialog) {
            keepPositionAfterDialog = false
            return
        }

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
        binding.map.fullscreenButton.visibility = visibility
        binding.map.statisticsLayout.visibility = visibility
    }

    private fun isPiPMode(): Boolean {
        return isInPictureInPictureMode
    }

    override fun onPause() {
        mapPreferences.save(map)
        if (!isPiPMode()) {
            binding.map.mapView.onPause()
        }
        super.onPause()
    }

}
