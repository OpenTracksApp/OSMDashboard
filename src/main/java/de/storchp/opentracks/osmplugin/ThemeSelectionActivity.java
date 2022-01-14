package de.storchp.opentracks.osmplugin;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import org.mapsforge.map.rendertheme.ZipXmlThemeResourceProvider;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.zip.ZipInputStream;

import de.storchp.opentracks.osmplugin.databinding.ActivityThemeSelectionBinding;
import de.storchp.opentracks.osmplugin.databinding.ThemeItemBinding;
import de.storchp.opentracks.osmplugin.utils.FileItem;
import de.storchp.opentracks.osmplugin.utils.FileUtil;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;
import de.storchp.opentracks.osmplugin.utils.ThemeItemAdapter;

public class ThemeSelectionActivity extends AppCompatActivity {

    private static final String TAG = ThemeSelectionActivity.class.getSimpleName();

    private ThemeItemAdapter adapter;

    private ActivityThemeSelectionBinding binding;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThemeSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.mapsToolbar.setTitle(R.string.theme_selection);
        setSupportActionBar(binding.toolbar.mapsToolbar);

        final boolean onlineMapSelected = PreferencesUtils.getMapUris().isEmpty();
        if (onlineMapSelected) {
            PreferencesUtils.setMapThemeUri(null);
        }

        adapter = new ThemeItemAdapter(this, new ArrayList<>(), PreferencesUtils.getMapThemeUri(), onlineMapSelected);
        adapter.add(new FileItem(getString(R.string.default_theme), null));

        new Thread(() -> {
            final var directory = PreferencesUtils.getMapThemeDirectoryUri();
            final var items = new ArrayList<FileItem>();
            if (directory != null) {
                final var documentsTree = FileUtil.getDocumentFileFromTreeUri(ThemeSelectionActivity.this, directory);
                if (documentsTree != null) {
                    for (final var file : documentsTree.listFiles()) {
                        readThemeFile(items, file);
                    }
                }
            }
            runOnUiThread(() -> {
                adapter.addAll(items);
                adapter.notifyDataSetChanged();
                binding.progressBar.setVisibility(View.GONE);
            });
        }).start();

        binding.themeList.setAdapter(adapter);
        binding.themeList.setOnItemClickListener((listview, view, position, id) -> {
            final var itemBinding = (ThemeItemBinding) view.getTag();
            itemBinding.radiobutton.setChecked(!itemBinding.radiobutton.isChecked());
            itemBinding.radiobutton.callOnClick();
        });
        binding.themeList.setOnItemLongClickListener((parent, view, position, id) -> {
            final var fileItem = adapter.getItem(position);
            final var uri = fileItem.getUri();
            if (uri == null) {
                // online theme can't be deleted
                return false;
            }
            new AlertDialog.Builder(ThemeSelectionActivity.this)
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.delete_theme_question, fileItem.getName()))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Log.d(TAG, "Delete " + fileItem.getName());
                    final var file = FileUtil.getDocumentFileFromTreeUri(ThemeSelectionActivity.this, uri);
                    assert file != null;
                    final boolean deleted = file.delete();
                    if (deleted) {
                        adapter.remove(fileItem);
                        adapter.setSelectedUri(null);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(ThemeSelectionActivity.this, R.string.delete_theme_error, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
            return false;
        });
    }

    private void readThemeFile(final ArrayList<FileItem> items, final DocumentFile file) {
        if (file.isFile() && file.getName() != null) {
            if (file.getName().endsWith(".xml")) {
                items.add(new FileItem(file.getName(), file.getUri()));
            } else if (file.getName().endsWith(".zip")) {
                try {
                    final var xmlThemes = ZipXmlThemeResourceProvider.scanXmlThemes(new ZipInputStream(new BufferedInputStream(getContentResolver().openInputStream(file.getUri()))));
                    xmlThemes.forEach(xmlTheme -> items.add(new FileItem(file.getName() + "#" + xmlTheme, file.getUri().buildUpon().fragment(xmlTheme).build())));
                } catch (final Exception e) {
                    Log.e(TAG, "Failed to read theme .zip file: " + file.getName(), e);
                }
            }
        } else if (file.isDirectory()) {
            final var childFile = file.findFile(file.getName() + ".xml");
            if (childFile != null) {
                items.add(new FileItem(childFile.getName(), childFile.getUri()));
            }
        }
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
        PreferencesUtils.setMapThemeUri(adapter.getSelectedUri());
        super.onBackPressed();
    }

}