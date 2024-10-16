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

class ShowErrorActivity : AppCompatActivity() {
    private var binding: ActivityShowErrorBinding? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowErrorBinding.inflate(getLayoutInflater())
        setContentView(binding!!.getRoot())

        binding!!.textViewError.setText(getIntent().getStringExtra(EXTRA_ERROR_TEXT))

        setSupportActionBar(binding!!.toolbar.mapsToolbar)
        if (getSupportActionBar() != null) {
            getSupportActionBar()!!.setTitle(createErrorTitle())
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
                        binding!!.textViewError.getText().toString(),
                        StandardCharsets.UTF_8.toString()
                    )
                )
            )
        } catch (ignored: UnsupportedEncodingException) {
            // can't happen as UTF-8 is always available
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.show_error, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.error_share) {
            onClickedShare()
            return true
        } else if (item.getItemId() == R.id.error_report) {
            reportBug()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onClickedShare() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_SUBJECT, createErrorTitle())
        intent.putExtra(Intent.EXTRA_TEXT, binding!!.textViewError.getText())
        intent.setType("text/plain")
        startActivity(intent)
    }

    companion object {
        const val EXTRA_ERROR_TEXT: String = "error"
    }
}
