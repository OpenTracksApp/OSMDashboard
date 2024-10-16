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
        val binding = ActivityMainBinding.inflate(getLayoutInflater())
        setContentView(binding.getRoot())

        setSupportActionBar(binding.toolbar.mapsToolbar)

        binding.usageInfo.setMovementMethod(LinkMovementMethod.getInstance())
        binding.osmInfo.setMovementMethod(LinkMovementMethod.getInstance())
        binding.offlineMaps.setMovementMethod(LinkMovementMethod.getInstance())
        binding.versionInfo.setText(
            Html.fromHtml(
                getString(
                    R.string.version_info,
                    BuildConfig.BUILD_TYPE,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                ), Html.FROM_HTML_MODE_COMPACT
            )
        )

        if (BuildConfig.offline) {
            binding.offlineVersionInfo.setVisibility(View.VISIBLE)
            binding.offlineMapInfo.setVisibility(View.GONE)
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
