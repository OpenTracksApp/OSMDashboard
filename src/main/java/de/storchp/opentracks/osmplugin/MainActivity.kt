package de.storchp.opentracks.osmplugin

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.View
import de.storchp.opentracks.osmplugin.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        setSupportActionBar(binding.toolbar.mapsToolbar)

        binding.usageInfo.movementMethod = LinkMovementMethod.getInstance()
        binding.osmInfo.movementMethod = LinkMovementMethod.getInstance()
        binding.offlineMaps.movementMethod = LinkMovementMethod.getInstance()
        binding.versionInfo.text = Html.fromHtml(
            getString(
                R.string.version_info,
                BuildConfig.BUILD_TYPE,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            ), Html.FROM_HTML_MODE_COMPACT
        )

        if (BuildConfig.offline) {
            binding.offlineVersionInfo.visibility = View.VISIBLE
            binding.offlineMapInfo.visibility = View.GONE
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu, false)
        return true
    }
}
