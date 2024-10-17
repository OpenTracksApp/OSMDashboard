package de.storchp.opentracks.osmplugin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import de.storchp.opentracks.osmplugin.databinding.ActivityShowErrorBinding
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val EXTRA_ERROR_TEXT: String = "error"

class ShowErrorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShowErrorBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowErrorBinding.inflate(layoutInflater)
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

    private fun reportBug() {
        var uriUrl: Uri?
        try {
            uriUrl = Uri.parse(
                String.format(
                    getString(R.string.report_issue_link),
                    URLEncoder.encode(
                        binding.textViewError.getText().toString(),
                        StandardCharsets.UTF_8.toString()
                    )
                )
            )
        } catch (_: UnsupportedEncodingException) {
            // can't happen as UTF-8 is always available
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.show_error, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == R.id.error_share) {
            onClickedShare()
            true
        } else if (item.itemId == R.id.error_report) {
            reportBug()
            true
        } else {
            super.onOptionsItemSelected(item)
        }

    private fun onClickedShare() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, createErrorTitle())
            putExtra(Intent.EXTRA_TEXT, binding.textViewError.getText())
            setType("text/plain")
        }
        startActivity(intent)
    }

}
