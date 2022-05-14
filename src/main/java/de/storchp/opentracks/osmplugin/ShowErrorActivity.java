package de.storchp.opentracks.osmplugin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import de.storchp.opentracks.osmplugin.databinding.ActivityShowErrorBinding;

public class ShowErrorActivity extends AppCompatActivity {

    public static final String EXTRA_ERROR_TEXT = "error";

    private static final String TAG = ShowErrorActivity.class.getSimpleName();

    private ActivityShowErrorBinding binding;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityShowErrorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.textViewError.setText(getIntent().getStringExtra(EXTRA_ERROR_TEXT));

        setSupportActionBar(binding.toolbar.mapsToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(createErrorTitle());
        }
    }

    private String createErrorTitle() {
        return String.format(getString(R.string.error_crash_title), getString(R.string.app_name));
    }

    private void reportBug() {
        copyToClipboard(binding.textViewError.getText().toString());
        final Uri uriUrl;
        try {
            uriUrl = Uri.parse(
                    String.format(
                            getString(R.string.report_issue_link),
                            URLEncoder.encode(binding.textViewError.getText().toString(), StandardCharsets.UTF_8.toString())
                    )
            );
        } catch (final UnsupportedEncodingException e) {
            Log.e(TAG, "Can't encode error report", e);
            return;
        }
        final var intent = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(intent);
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_LONG).show();
    }

    public void copyToClipboard(final String text) {
        try {
            final var clip = ClipData.newPlainText(getString(R.string.clipboard_label, getString(R.string.app_name)), text);
            ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);
        } catch (final Exception e) {
            Toast.makeText(this, R.string.clipboard_unexpected_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception caught while copying to clipboard", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.show_error, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.error_share) {
            onClickedShare();
            return true;
        } else if (item.getItemId() == R.id.error_report) {
            reportBug();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onClickedShare() {
        final var intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, createErrorTitle());
        intent.putExtra(Intent.EXTRA_TEXT, binding.textViewError.getText());
        intent.setType("text/plain");
        startActivity(intent);
    }

}
