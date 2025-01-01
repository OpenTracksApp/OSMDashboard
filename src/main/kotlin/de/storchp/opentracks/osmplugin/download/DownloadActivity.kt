package de.storchp.opentracks.osmplugin.download

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import de.storchp.opentracks.osmplugin.BaseActivity
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.databinding.ActivityDownloadBinding
import de.storchp.opentracks.osmplugin.download.DownloadActivity.DownloadBroadcastReceiver
import de.storchp.opentracks.osmplugin.download.DownloadActivity.DownloadType
import de.storchp.opentracks.osmplugin.settings.DirectoryChooserActivity
import de.storchp.opentracks.osmplugin.settings.DirectoryChooserActivity.MapDirectoryChooserActivity
import de.storchp.opentracks.osmplugin.settings.DirectoryChooserActivity.ThemeDirectoryChooserActivity
import de.storchp.opentracks.osmplugin.utils.FileUtil
import de.storchp.opentracks.osmplugin.utils.FileUtil.createBinaryDocumentFile
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.lang.RuntimeException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

private val TAG: String = DownloadActivity::class.java.getSimpleName()
private const val UPDATE_DOWNLOAD_PROGRESS = 1

private const val OPENANDROMAPS_MAP_HOST = "ftp.gwdg.de"
private const val OPENANDROMAPS_THEME_HOST = "www.openandromaps.org"
private const val OPENANDROMAPS_MAP_DOWNLOAD_URL =
    "https://$OPENANDROMAPS_MAP_HOST/pub/misc/openstreetmap/openandromaps/mapsV4/"
private const val OPENANDROMAPS_THEME_DOWNLOAD_URL =
    "https://$OPENANDROMAPS_THEME_HOST/wp-content/users/tobias/"
private const val FREIZEITKARTE_HOST = "download.freizeitkarte-osm.de"

private const val MF_V4_MAP_SCHEME = "mf-v4-map"
private const val MF_THEME_SCHEME = "mf-theme"

class DownloadActivity : BaseActivity() {
    private var downloadUri: Uri? = null
    private var downloadType = DownloadType.MAP
    private lateinit var binding: ActivityDownloadBinding
    private var downloadId: Long? = null
    private var downloadManager: DownloadManager? = null
    private var downloadBroadcastReceiver: DownloadBroadcastReceiver? = null
    private val executor: ExecutorService = Executors.newFixedThreadPool(1)

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot()) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.getRoot())

        binding.toolbar.mapsToolbar.setTitle(R.string.download_map)
        setSupportActionBar(binding.toolbar.mapsToolbar)

        val uri = intent.data
        if (uri != null) {
            val scheme = uri.scheme
            val host = uri.host
            val path = uri.path
            Log.i(
                TAG,
                "scheme=" + scheme + ",host=" + host + ", path=" + path + ", lastPathSegment=" + uri.lastPathSegment
            )
            downloadUri = uri

            if (MF_V4_MAP_SCHEME == scheme) {
                if (host == "download.openandromaps.org" && path!!.startsWith("/mapsV4/") && path.endsWith(
                        ".zip"
                    )
                ) {
                    // OpenAndroMaps URIs need to be remapped - mf-v4-map://download.openandromaps.org/mapsV4/Germany/bayern.zip
                    downloadUri = Uri.parse(
                        OPENANDROMAPS_MAP_DOWNLOAD_URL + path.substring(
                            8
                        )
                    )
                    downloadType = DownloadType.MAP_ZIP
                } else {
                    // try to replace MF_V4_MAP_SCHEME with https for unknown sources
                    downloadUri = uri.buildUpon().scheme("https").build()
                    downloadType =
                        if (path!!.endsWith(".zip")) DownloadType.MAP_ZIP else DownloadType.MAP
                }
            } else if (MF_THEME_SCHEME == scheme) {
                downloadUri =
                    if (host == "download.openandromaps.org" && path!!.startsWith("/themes/")
                        && path.endsWith(".zip")
                    ) {
                        // no remapping, as they have themes only on their homepage, not on their ftp site
                        Uri.parse(
                            OPENANDROMAPS_THEME_DOWNLOAD_URL + path.substring(
                                8
                            )
                        )
                    } else {
                        // try to replace MF_THEME_SCHEME with https for unknown sources
                        uri.buildUpon().scheme("https").build()
                    }
                downloadType = DownloadType.THEME
                binding.toolbar.mapsToolbar.setTitle(R.string.download_theme)
            } else if (FREIZEITKARTE_HOST == host) {
                if (path!!.endsWith(".map.zip")) {
                    downloadType = DownloadType.MAP_ZIP
                } else if (path.endsWith(".zip")) {
                    downloadType = DownloadType.THEME
                    binding.toolbar.mapsToolbar.setTitle(R.string.download_theme)
                }
            } else if (OPENANDROMAPS_MAP_HOST == host && path!!.endsWith(".zip")) {
                downloadType = DownloadType.MAP_ZIP
            } else if (OPENANDROMAPS_THEME_HOST == host && path!!.endsWith(".zip")) {
                downloadType = DownloadType.THEME
            }

            Log.i(TAG, "downloadUri=$downloadUri, downloadType=$downloadType")

            binding.downloadInfo.text = downloadUri.toString()

            downloadBroadcastReceiver = DownloadBroadcastReceiver()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    downloadBroadcastReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    RECEIVER_EXPORTED
                )
            } else {
                registerReceiver(
                    downloadBroadcastReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

            startDownload()
        } else {
            binding.downloadInfo.setText(R.string.no_download_uri_found)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateUp()
            }
        })
    }

    val directoryIntentLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult<Intent, ActivityResult>(StartActivityForResult(),
            ActivityResultCallback { result ->
                if (result.resultCode == RESULT_OK) {
                    startDownload()
                }
            })

    fun startDownload() {
        val directoryUri = downloadType.getDirectoryUri()
        if (directoryUri != null) {
            val directoryFile = FileUtil.getDocumentFileFromTreeUri(this, directoryUri)
            if (directoryFile != null && !directoryFile.canWrite()) {
                directoryIntentLauncher.launch(Intent(this, downloadType.getDirectoryChooser()))
                return
            }
        }

        val filesDir = getExternalFilesDir(null)!!.toPath()
        val downloadDir = filesDir.resolve(downloadType.subdir)
        downloadDir.toFile().mkdir()
        val filename = downloadUri!!.lastPathSegment
        val file = downloadDir.resolve(filename)
        if (file.toFile().exists()) {
            AlertDialog.Builder(this@DownloadActivity)
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setMessage(getString(downloadType.overwriteMessageId, filename))
                .setPositiveButton(
                    android.R.string.ok,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        file.toFile().delete()
                        dialog!!.dismiss()
                        startDownload()
                    })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show()
            return
        }

        val request = DownloadManager.Request(downloadUri)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(this, null, downloadType.subdir + "/" + filename)
            .setTitle(filename)
            .setDescription(getString(downloadType.downloadMessageId))
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager!!.enqueue(request)
        observeDownload()
    }

    private fun observeDownload() {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.isIndeterminate = false
        binding.progressBar.setMax(100)
        binding.progressBar.progress = 0

        executor.execute(Runnable {
            var progress = 0
            var isDownloadFinished = false
            while (!isDownloadFinished) {
                downloadManager!!.query(DownloadManager.Query().setFilterById(downloadId!!))
                    .use { cursor ->
                        if (cursor.moveToFirst()) {
                            val downloadStatus =
                                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                            when (downloadStatus) {
                                DownloadManager.STATUS_RUNNING -> {
                                    val totalBytes =
                                        cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                    if (totalBytes > 0) {
                                        val downloadedBytes = cursor.getLong(
                                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                        )
                                        progress = (downloadedBytes * 100 / totalBytes).toInt()
                                    }
                                }

                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    progress = 100
                                    isDownloadFinished = true
                                }

                                DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {}
                                DownloadManager.STATUS_FAILED -> isDownloadFinished = true
                            }
                            val message = Message.obtain()
                            message.what = UPDATE_DOWNLOAD_PROGRESS
                            message.arg1 = progress
                            mainHandler.sendMessage(message)
                        }
                    }
            }
        })
    }

    private val mainHandler = Handler(Looper.getMainLooper(), object : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            if (msg.what == UPDATE_DOWNLOAD_PROGRESS) {
                val downloadProgress = msg.arg1
                binding.progressBar.progress = downloadProgress
            }
            return true
        }
    })

    private inner class DownloadBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId != id) {
                return
            }
            downloadManager!!.query(DownloadManager.Query().setFilterById(downloadId!!))
                .use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status =
                            cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val uri =
                                cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            downloadEnded(Uri.parse(uri))
                        }
                    }
                }
        }
    }

    private fun downloadEnded(downloadedUri: Uri) {
        executor.shutdown()
        mainHandler.removeCallbacksAndMessages(null)
        val destinationDir = downloadType.getDirectoryUri()
        if (destinationDir == null) {
            if (downloadType.extractMapFromZIP) {
                try {
                    contentResolver.openInputStream(downloadedUri).use { inputStream ->
                        val zis = ZipInputStream(inputStream)
                        var ze: ZipEntry? = null
                        var foundMapInZip = false
                        while (!foundMapInZip && (zis.getNextEntry().also { ze = it }) != null) {
                            val filename = ze!!.name
                            if (filename.endsWith(".map")) {
                                val downloadedFile = File(downloadedUri.path!!)
                                val targetFile = File(downloadedFile.getParent(), filename)
                                copy(zis, FileOutputStream(targetFile))
                                foundMapInZip = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                } finally {
                    File(downloadedUri.path!!).delete()
                }
            }
        } else {
            try {
                contentResolver.openInputStream(downloadedUri).use { inputStream ->
                    if (downloadType.extractMapFromZIP) {
                        val zis = ZipInputStream(inputStream)
                        var ze: ZipEntry? = null
                        var foundMapInZip = false
                        while (!foundMapInZip && (zis.getNextEntry().also { ze = it }) != null) {
                            val filename = ze!!.name
                            if (filename.endsWith(".map")) {
                                val targetFile =
                                    createBinaryDocumentFile(this, destinationDir, filename)
                                copy(
                                    zis,
                                    contentResolver.openOutputStream(
                                        targetFile.uri,
                                        "wt"
                                    )!!
                                )
                                foundMapInZip = true
                            }
                        }
                    } else {
                        val filename = downloadedUri.lastPathSegment
                        val targetFile = createBinaryDocumentFile(this, destinationDir, filename!!)
                        copy(
                            inputStream!!,
                            contentResolver.openOutputStream(targetFile.uri, "wt")!!
                        )
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            } finally {
                File(downloadedUri.path!!).delete()
            }
        }

        binding.progressBar.visibility = View.GONE
        downloadId = null
        Toast.makeText(this, downloadType.successMessageId, Toast.LENGTH_LONG).show()
    }

    @Throws(IOException::class)
    private fun copy(input: InputStream, output: OutputStream) {
        val data = ByteArray(4096)
        var count: Int
        while ((input.read(data).also { count = it }) != -1) {
            output.write(data, 0, count)
        }
        input.close()
        output.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (downloadBroadcastReceiver != null) {
            unregisterReceiver(downloadBroadcastReceiver)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            navigateUp()
            return true
        }
        return false
    }

    fun navigateUp() {
        finish()
    }

    enum class DownloadType(
        val downloadMessageId: Int,
        val overwriteMessageId: Int,
        val successMessageId: Int,
        val extractMapFromZIP: Boolean,
        val subdir: String
    ) {
        MAP(
            R.string.download_map,
            R.string.overwrite_map_question,
            R.string.download_success,
            false,
            "maps"
        ) {
            override fun getDirectoryUri(): Uri? {
                return PreferencesUtils.getMapDirectoryUri()
            }

            override fun getDirectoryChooser(): Class<out DirectoryChooserActivity?> {
                return MapDirectoryChooserActivity::class.java
            }
        },
        MAP_ZIP(
            R.string.download_map,
            R.string.overwrite_map_question,
            R.string.download_success,
            true,
            "maps"
        ) {
            override fun getDirectoryUri(): Uri? {
                return PreferencesUtils.getMapDirectoryUri()
            }

            override fun getDirectoryChooser(): Class<out DirectoryChooserActivity?> {
                return MapDirectoryChooserActivity::class.java
            }
        },
        THEME(
            R.string.download_theme,
            R.string.overwrite_theme_question,
            R.string.download_theme_success,
            false,
            "themes"
        ) {
            override fun getDirectoryUri(): Uri? {
                return PreferencesUtils.getMapThemeDirectoryUri()
            }

            override fun getDirectoryChooser(): Class<out DirectoryChooserActivity?> {
                return ThemeDirectoryChooserActivity::class.java
            }
        };

        abstract fun getDirectoryUri(): Uri?
        abstract fun getDirectoryChooser(): Class<out DirectoryChooserActivity?>?
    }

}
