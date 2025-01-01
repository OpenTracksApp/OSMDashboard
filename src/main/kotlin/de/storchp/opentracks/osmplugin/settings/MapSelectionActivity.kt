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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import de.storchp.opentracks.osmplugin.BuildConfig
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.databinding.ActivityMapSelectionBinding
import de.storchp.opentracks.osmplugin.databinding.MapItemBinding
import de.storchp.opentracks.osmplugin.download.DownloadActivity.DownloadType
import de.storchp.opentracks.osmplugin.utils.FileUtil
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils
import java.nio.file.Files

class MapSelectionActivity : AppCompatActivity() {
    private lateinit var adapter: MapItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityMapSelectionBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot()) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.getRoot())

        binding.toolbar.mapsToolbar.setTitle(R.string.map_selection)
        setSupportActionBar(binding.toolbar.mapsToolbar)

        val items = buildList {
            @Suppress("KotlinConstantConditions")
            if (!BuildConfig.offline) {
                add(FileItem(getString(R.string.online_osm_mapnick), null, null, null))
            }
            val mapDirectory = PreferencesUtils.getMapDirectoryUri()
            if (mapDirectory == null) {
                val filesDir = getExternalFilesDir(null)!!.toPath()
                val mapDir = filesDir.resolve(DownloadType.MAP.subdir)
                if (Files.exists(mapDir)) {
                    mapDir.toFile().listFiles()
                        ?.filter { file ->
                            file.isFile() && file.exists() && file.getName().endsWith(".map")
                        }
                        ?.forEach { file ->
                            add(
                                FileItem(
                                    name = file.getName(),
                                    uri = Uri.fromFile(file),
                                    file = file,
                                )
                            )
                        }
                }
            } else {
                val documentsTree =
                    FileUtil.getDocumentFileFromTreeUri(this@MapSelectionActivity, mapDirectory)
                documentsTree?.listFiles()?.filter { file ->
                    file.isFile && file.name?.endsWith(".map") == true
                }?.forEach { file ->
                    add(
                        FileItem(
                            name = file.name.toString(),
                            uri = file.uri,
                            documentFile = file
                        )
                    )
                }
            }
        }
        adapter = MapItemAdapter(this, items, PreferencesUtils.getMapUris())

        binding.mapList.setAdapter(adapter)
        binding.mapList.onItemClickListener =
            OnItemClickListener { listview: AdapterView<*>?, view: View?, position: Int, id: Long ->
                val itemBinding = view!!.tag as MapItemBinding
                itemBinding.checkbox.setChecked(!itemBinding.checkbox.isChecked)
                itemBinding.checkbox.callOnClick()
            }
        binding.mapList.onItemLongClickListener =
            OnItemLongClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                onItemLongClick(position)
            }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateUp()
            }
        })
    }

    private fun onItemLongClick(
        position: Int
    ): Boolean {
        val fileItem = adapter.getItem(position)
        if (fileItem?.file == null && fileItem?.documentFile == null) {
            // online map can't be deleted
            return false
        }
        AlertDialog.Builder(this@MapSelectionActivity)
            .setIcon(R.drawable.ic_logo_color_24dp)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.delete_map_question, fileItem.name))
            .setPositiveButton(
                android.R.string.ok,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    var deleted: Boolean = if (fileItem.file != null) {
                        fileItem.file.delete()
                    } else {
                        fileItem.documentFile!!.delete()
                    }
                    if (deleted) {
                        adapter.remove(fileItem)
                    } else {
                        Toast.makeText(
                            this,
                            R.string.delete_map_error,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create().show()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) {
            navigateUp()
            true
        } else {
            false
        }

    fun navigateUp() {
        PreferencesUtils.setMapUris(adapter.getSelectedUris())
        finish()
    }
}