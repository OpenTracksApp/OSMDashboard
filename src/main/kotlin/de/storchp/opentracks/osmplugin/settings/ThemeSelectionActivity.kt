package de.storchp.opentracks.osmplugin.settings

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.databinding.ActivityThemeSelectionBinding
import de.storchp.opentracks.osmplugin.databinding.ThemeItemBinding
import de.storchp.opentracks.osmplugin.download.DownloadActivity.DownloadType
import de.storchp.opentracks.osmplugin.settings.ThemeSelectionActivity.MapThemeDirScanner
import de.storchp.opentracks.osmplugin.utils.FileUtil
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils
import org.oscim.theme.ZipXmlThemeResourceProvider
import java.io.BufferedInputStream
import java.io.File
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.ref.WeakReference
import java.nio.file.Files
import java.util.zip.ZipInputStream

class ThemeSelectionActivity : AppCompatActivity() {
    private lateinit var adapter: ThemeItemAdapter
    private lateinit var binding: ActivityThemeSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityThemeSelectionBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot()) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.getRoot())

        binding.toolbar.mapsToolbar.setTitle(R.string.theme_selection)
        setSupportActionBar(binding.toolbar.mapsToolbar)

        val onlineMapSelected = PreferencesUtils.getMapUris().isEmpty()
        if (onlineMapSelected) {
            PreferencesUtils.setMapThemeUri(null)
        }

        adapter = ThemeItemAdapter(
            this,
            PreferencesUtils.getMapThemeUri(),
            onlineMapSelected
        )
        adapter.add(FileItem(getString(R.string.default_theme), null, null, null))

        Thread(MapThemeDirScanner(this)).start()

        binding.themeList.setAdapter(adapter)
        binding.themeList.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, view: View?, _: Int, _: Long ->
                val itemBinding = view!!.tag as ThemeItemBinding
                itemBinding.radiobutton.setChecked(!itemBinding.radiobutton.isChecked)
                itemBinding.radiobutton.callOnClick()
            }
        binding.themeList.onItemLongClickListener =
            OnItemLongClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                onItemLongClick(position)
            }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateUp()
            }
        })
    }

    private fun onItemLongClick(
        position: Int,
    ): Boolean {
        val fileItem = adapter.getItem(position)
        val uri = fileItem?.uri
        if (fileItem == null || uri == null) {
            // online theme can't be deleted
            return false
        }
        AlertDialog.Builder(this@ThemeSelectionActivity)
            .setIcon(R.drawable.ic_logo_color_24dp)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.delete_theme_question, fileItem.name))
            .setPositiveButton(
                android.R.string.ok,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    var deleted: Boolean
                    if ("file" == uri.scheme) {
                        deleted = uri.toFile().delete()
                    } else {
                        val file = FileUtil.getDocumentFileFromTreeUri(this, uri)
                        deleted = file!!.delete()
                    }
                    if (deleted) {
                        adapter.remove(fileItem)
                        adapter.selectedUri = null
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(
                            this,
                            R.string.delete_theme_error,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create().show()
        return true
    }

    private class MapThemeDirScanner(activity: ThemeSelectionActivity) : Runnable {
        val activityRef: WeakReference<ThemeSelectionActivity> =
            WeakReference<ThemeSelectionActivity>(activity)

        override fun run() {
            val directory = PreferencesUtils.getMapThemeDirectoryUri()
            val activity = activityRef.get()
            if (activity == null) {
                return
            }
            val items = buildList {
                if (directory == null) {
                    val filesDir = activity.getExternalFilesDir(null)!!.toPath()
                    val themeDir = filesDir.resolve(DownloadType.THEME.subdir)
                    if (Files.exists(themeDir)) {
                        themeDir.toFile().listFiles()!!.forEach {
                            addAll(activity.readThemeFile(it))
                        }
                    }
                } else {
                    val documentsTree = FileUtil.getDocumentFileFromTreeUri(activity, directory)
                    documentsTree?.listFiles()?.forEach {
                        addAll(activity.readThemeFile(it))
                    }
                }
            }

            activity.runOnUiThread(Runnable {
                activity.adapter.addAll(items)
                activity.adapter.notifyDataSetChanged()
                activity.binding.progressBar.visibility = View.GONE
            })
        }
    }

    private fun readThemeFile(file: DocumentFile) = sequence {
        if (file.isFile && file.name != null) {
            if (file.name!!.endsWith(".xml")) {
                yield(FileItem(file.name!!, file.uri, null, file))
            } else if (file.name!!.endsWith(".zip")) {
                yieldAll(
                    resolveThemesFromZip(
                        uri = file.uri,
                        filename = file.name!!,
                        documentFile = file,
                    )
                )
            }
        } else if (file.isDirectory) {
            val childFile = file.findFile(file.name + ".xml")
            if (childFile != null) {
                yield(FileItem(childFile.name!!, childFile.uri, null, childFile))
            }
        }
    }

    private fun readThemeFile(file: File) = sequence {
        if (file.isFile() && file.exists()) {
            val uri = Uri.fromFile(file)
            if (file.getName().endsWith(".xml")) {
                yield(FileItem(file.getName(), uri, file, null))
            } else if (file.getName().endsWith(".zip")) {
                yieldAll(
                    resolveThemesFromZip(
                        uri = uri,
                        filename = file.getName(),
                        file = file
                    )
                )
            }
        } else if (file.isDirectory()) {
            val childFile = File(file, file.getName() + ".xml")
            if (childFile.exists()) {
                yield(FileItem(childFile.getName(), Uri.fromFile(childFile), childFile, null))
            }
        }
    }

    private fun resolveThemesFromZip(
        uri: Uri,
        filename: String,
        documentFile: DocumentFile? = null,
        file: File? = null,
    ) = sequence {
        try {
            val xmlThemes = ZipXmlThemeResourceProvider.scanXmlThemes(
                ZipInputStream(
                    BufferedInputStream(contentResolver.openInputStream(uri))
                )
            )
            xmlThemes.forEach { xmlTheme ->
                yield(
                    FileItem(
                        "$filename#$xmlTheme",
                        uri.buildUpon().fragment(xmlTheme).build(),
                        file,
                        documentFile
                    )
                )
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to read theme .zip file: $filename", e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) {
            navigateUp()
            true
        } else {
            false
        }

    fun navigateUp() {
        PreferencesUtils.setMapThemeUri(adapter.selectedUri)
        finish()
    }
}