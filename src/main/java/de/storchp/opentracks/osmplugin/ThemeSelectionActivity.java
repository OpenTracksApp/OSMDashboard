package de.storchp.opentracks.osmplugin;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.List;

import de.storchp.opentracks.osmplugin.utils.FileItem;
import de.storchp.opentracks.osmplugin.utils.FileUtil;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;
import de.storchp.opentracks.osmplugin.utils.ThemeItemAdapter;

public class ThemeSelectionActivity extends AppCompatActivity {

    private static final String TAG = ThemeSelectionActivity.class.getSimpleName();

    private ThemeItemAdapter adapter;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_selection);

        final Toolbar toolbar = findViewById(R.id.maps_toolbar);
        toolbar.setTitle(R.string.theme_selection);
        setSupportActionBar(toolbar);

        progressBar = findViewById(R.id.progressBar);

        final Uri selected = PreferencesUtils.getMapThemeUri(this);
        adapter = new ThemeItemAdapter(this, new ArrayList<FileItem>(), selected);
        adapter.add(new FileItem(getString(R.string.default_theme), null));

        new Thread(() -> {
            final Uri directory = PreferencesUtils.getMapThemeDirectoryUri(ThemeSelectionActivity.this);
            final List<FileItem> items = new ArrayList<>();
            if (directory != null) {
                final DocumentFile documentsTree = FileUtil.getDocumentFileFromTreeUri(ThemeSelectionActivity.this, directory);
                if (documentsTree != null) {
                    for (final DocumentFile file : documentsTree.listFiles()) {
                        if (file.isFile() && file.getName().endsWith(".xml")) {
                            items.add(new FileItem(file.getName(), file.getUri()));
                        } else if (file.isDirectory()) {
                            final DocumentFile childFile = file.findFile(file.getName() + ".xml");
                            if (childFile != null) {
                                items.add(new FileItem(childFile.getName(), childFile.getUri()));
                            }
                        }
                    }
                }
            }
            runOnUiThread(() -> {
                adapter.addAll(items);
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            });
        }).start();

        final ListView listView = findViewById(R.id.theme_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((listview, view, position, id) -> {
            final ThemeItemAdapter.ViewHolder holder = (ThemeItemAdapter.ViewHolder) view.getTag();
            holder.radioButton.setChecked(!holder.radioButton.isChecked());
            holder.radioButton.callOnClick();
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            final ThemeItemAdapter.ViewHolder holder = (ThemeItemAdapter.ViewHolder) view.getTag();
            final FileItem fileItem = adapter.getItem(position);
            final Uri uri = fileItem.getUri();
            if (uri == null) {
                // online theme can't be deleted
                return false;
            }
            new AlertDialog.Builder(ThemeSelectionActivity.this)
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.delete_theme_question, fileItem.getName()))
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Log.d(TAG, "Delete " + fileItem.getName());
                    final DocumentFile file = FileUtil.getDocumentFileFromTreeUri(ThemeSelectionActivity.this, uri);
                    final boolean deleted = file.delete();
                    if (deleted) {
                        adapter.remove(fileItem);
                        adapter.setSelectedUri(null);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(ThemeSelectionActivity.this, R.string.delete_theme_error, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create().show();
            return false;
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        final Uri selectedUri = adapter.getSelectedUri();
        PreferencesUtils.setMapThemeUri(this, selectedUri);

        super.onBackPressed();
    }

}