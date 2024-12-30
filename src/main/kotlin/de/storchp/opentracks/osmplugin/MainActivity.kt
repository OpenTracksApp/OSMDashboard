package de.storchp.opentracks.osmplugin

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import de.storchp.opentracks.osmplugin.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot()) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            v.updateLayoutParams<MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                topMargin = insets.top
                rightMargin = insets.right
            }

            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.getRoot())

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu, false)
        return true
    }
}
