package de.storchp.opentracks.osmplugin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import de.storchp.opentracks.osmplugin.databinding.ActivityShowErrorBinding
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val EXTRA_ERROR_TEXT: String = "error"

class ShowErrorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShowErrorBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityShowErrorBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot()) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.getRoot())

        binding.textViewError.text = intent.getStringExtra(EXTRA_ERROR_TEXT)

        setSupportActionBar(binding.toolbar.mapsToolbar)
        supportActionBar?.let {
            it.title = createErrorTitle()
        }
    }

    private fun createErrorTitle(): String {
        return String.format(getString(R.string.error_crash_title), getString(R.string.app_name))
    }

    private fun reportBug(): Boolean {
        var uriUrl: Uri?
        try {
            uriUrl = String.format(
                getString(R.string.report_issue_link),
                URLEncoder.encode(
                    binding.textViewError.getText().toString(),
                    StandardCharsets.UTF_8.toString()
                )
            ).toUri()
        } catch (_: UnsupportedEncodingException) {
            // can't happen as UTF-8 is always available
            return false
        }
        val intent = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(intent)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.show_error, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.error_share -> onClickedShare()
            R.id.error_report -> reportBug()
            else -> super.onOptionsItemSelected(item)
        }

    private fun onClickedShare(): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, createErrorTitle())
            putExtra(Intent.EXTRA_TEXT, binding.textViewError.getText())
            setType("text/plain")
        }
        startActivity(intent)
        return true
    }

}
