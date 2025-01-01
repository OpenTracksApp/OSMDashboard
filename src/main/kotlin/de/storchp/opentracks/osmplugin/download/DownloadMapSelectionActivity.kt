package de.storchp.opentracks.osmplugin.download

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import de.storchp.opentracks.osmplugin.BaseActivity
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.databinding.ActivityDownloadMapSelectionBinding
import org.jsoup.Jsoup
import java.io.IOException
import java.lang.RuntimeException

const val MAPS_V_5_DOWNLOAD_URI: String =
    "https://ftp-stud.hs-esslingen.de/pub/Mirrors/download.mapsforge.org/maps/v5/"

class DownloadMapSelectionActivity : BaseActivity() {
    private lateinit var adapter: DownloadMapItemAdapter
    private var pageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityDownloadMapSelectionBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot()) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.getRoot())

        pageUri = Uri.parse(MAPS_V_5_DOWNLOAD_URI)
        val intent = getIntent()
        if (intent != null) {
            val mapDownloadUri = intent.data
            if (mapDownloadUri != null) {
                pageUri = mapDownloadUri
            }
        }
        binding.downloadProviderInfo.text = getString(R.string.download_page_info, pageUri)

        binding.toolbar.mapsToolbar.setTitle(R.string.choose_map_to_download)
        setSupportActionBar(binding.toolbar.mapsToolbar)

        adapter = DownloadMapItemAdapter(this, mutableListOf())
        binding.mapDownloadList.setAdapter(adapter)
        binding.mapDownloadList.onItemClickListener =
            OnItemClickListener { listview: AdapterView<*>?, view: View?, position: Int, id: Long ->
                val item = adapter.getItem(position)
                if (item != null) {
                    if (item.downloadItemType == DownloadItemType.MAP) {
                        startActivity(
                            Intent(
                                Intent.ACTION_DEFAULT,
                                item.uri,
                                this@DownloadMapSelectionActivity,
                                DownloadActivity::class.java
                            )
                        )
                    } else if (item.downloadItemType == DownloadItemType.SUBDIR) {
                        startActivity(
                            Intent(
                                Intent.ACTION_DEFAULT,
                                item.uri,
                                this@DownloadMapSelectionActivity,
                                DownloadMapSelectionActivity::class.java
                            )
                        )
                    }
                }
            }

        Thread(Runnable {
            try {
                val doc = Jsoup.connect(pageUri.toString()).get()
                val rows = doc.select("tr")
                val items = buildList {
                    for (element in rows) {
                        val cells = element.select("td")
                        if (cells.size >= 4) {
                            val alt = cells[0].select("img")[0].attr("alt")
                            val link = cells[1].select("a")[0]
                            val href = link.attr("href")
                            val linkText = link.text()
                            val date = cells[2].text()
                            val size = cells[3].text()
                            alt.toDownloadItemType()
                                ?.let { type ->
                                    add(
                                        DownloadMapItem(
                                            downloadItemType = type,
                                            name = linkText,
                                            date = date,
                                            size = size,
                                            uri = pageUri!!.buildUpon().appendEncodedPath(href)
                                                .build()
                                        )
                                    )
                                }
                        }
                    }
                }
                items.sorted()
                runOnUiThread(Runnable { adapter.addAll(items) })
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }).start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return false
    }

}
