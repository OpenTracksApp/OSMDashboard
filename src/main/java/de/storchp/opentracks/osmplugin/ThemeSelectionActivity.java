package de.storchp.opentracks.osmplugin;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import org.oscim.theme.ZipXmlThemeResourceProvider;

import java.io.BufferedInputStream;
import java.lang.ref.WeakReference;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThemeSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.mapsToolbar.setTitle(R.string.theme_selection);
        setSupportActionBar(binding.toolbar.mapsToolbar);

        boolean onlineMapSelected = PreferencesUtils.getMapUris().isEmpty();
        if (onlineMapSelected) {
            PreferencesUtils.setMapThemeUri(null);
        }

        adapter = new ThemeItemAdapter(this, new ArrayList<>(), PreferencesUtils.getMapThemeUri(), onlineMapSelected);
        adapter.add(new FileItem(getString(R.string.default_theme), null));

        new Thread(new MapThemeDirScanner(this)).start();

        binding.themeList.setAdapter(adapter);
        binding.themeList.setOnItemClickListener((listview, view, position, id) -> {
            var itemBinding = (ThemeItemBinding) view.getTag();
            itemBinding.radiobutton.setChecked(!itemBinding.radiobutton.isChecked());
            itemBinding.radiobutton.callOnClick();
        });
        binding.themeList.setOnItemLongClickListener((parent, view, position, id) -> {
            var fileItem = adapter.getItem(position);
            var uri = fileItem.uri();
            if (uri == null) {
                // online theme can't be deleted
                return false;
            }
            new AlertDialog.Builder(ThemeSelectionActivity.this)
                    .setIcon(R.drawable.ic_logo_color_24dp)
                    .setTitle(R.string.app_name)
                    .setMessage(getString(R.string.delete_theme_question, fileItem.name()))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Log.d(TAG, "Delete " + fileItem.name());
                        var file = FileUtil.getDocumentFileFromTreeUri(ThemeSelectionActivity.this, uri);
                        assert file != null;
                        boolean deleted = file.delete();
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            public void handleOnBackPressed() {
                navigateUp();
            }
        });
    }

    private static class MapThemeDirScanner implements Runnable {

        final WeakReference<ThemeSelectionActivity> activityRef;

        private MapThemeDirScanner(final ThemeSelectionActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            var directory = PreferencesUtils.getMapThemeDirectoryUri();
            var items = new ArrayList<FileItem>();
            var activity = activityRef.get();
            if (activity == null) {
                return;
            }
            if (directory != null) {
                var documentsTree = FileUtil.getDocumentFileFromTreeUri(activity, directory);
                if (documentsTree != null) {
                    for (var file : documentsTree.listFiles()) {
                        activity.readThemeFile(items, file);
                    }
                }
            }

            activity.runOnUiThread(() -> {
                activity.adapter.addAll(items);
                activity.adapter.notifyDataSetChanged();
                activity.binding.progressBar.setVisibility(View.GONE);
            });
        }
    }

    private void readThemeFile(ArrayList<FileItem> items, DocumentFile file) {
        if (file.isFile() && file.getName() != null) {
            if (file.getName().endsWith(".xml")) {
                items.add(new FileItem(file.getName(), file.getUri()));
            } else if (file.getName().endsWith(".zip")) {
                try {
                    var xmlThemes = ZipXmlThemeResourceProvider.scanXmlThemes(new ZipInputStream(new BufferedInputStream(getContentResolver().openInputStream(file.getUri()))));
                    xmlThemes.forEach(xmlTheme -> items.add(new FileItem(file.getName() + "#" + xmlTheme, file.getUri().buildUpon().fragment(xmlTheme).build())));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to read theme .zip file: " + file.getName(), e);
                }
            }
        } else if (file.isDirectory()) {
            var childFile = file.findFile(file.getName() + ".xml");
            if (childFile != null) {
                items.add(new FileItem(childFile.getName(), childFile.getUri()));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            navigateUp();
            return true;
        }
        return false;
    }

    public void navigateUp() {
        PreferencesUtils.setMapThemeUri(adapter.getSelectedUri());
        finish();
    }

}