package de.storchp.opentracks.osmplugin;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import de.storchp.opentracks.osmplugin.databinding.ActivityMapSelectionBinding;
import de.storchp.opentracks.osmplugin.databinding.MapItemBinding;
import de.storchp.opentracks.osmplugin.utils.FileItem;
import de.storchp.opentracks.osmplugin.utils.FileUtil;
import de.storchp.opentracks.osmplugin.utils.MapItemAdapter;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public class MapSelectionActivity extends AppCompatActivity {

    private MapItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var binding = ActivityMapSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.mapsToolbar.setTitle(R.string.map_selection);
        setSupportActionBar(binding.toolbar.mapsToolbar);

        var items = new ArrayList<FileItem>();
        if (!BuildConfig.offline) {
            items.add(new FileItem(getString(R.string.online_osm_mapnick), null, null));
        }
        var mapDirectory = PreferencesUtils.getMapDirectoryUri();
        if (mapDirectory == null) {
            var filesDir = getExternalFilesDir(null).toPath();
            var mapDir = filesDir.resolve(DownloadActivity.DownloadType.MAP.getSubdir());
            if (Files.exists(mapDir)) {
                Arrays.stream(mapDir.toFile().listFiles())
                        .filter(file -> file.isFile() && file.exists() && file.getName().endsWith(".map"))
                        .forEach(file -> items.add(new FileItem(file.getName(), file, null)));
            }
        } else {
            var documentsTree = FileUtil.getDocumentFileFromTreeUri(this, mapDirectory);
            if (documentsTree != null) {
                Arrays.stream(documentsTree.listFiles())
                        .filter(file -> file.isFile() && file.getName().endsWith(".map"))
                        .forEach(file -> items.add(new FileItem(file.getName(), null, file)));
            }
        }
        adapter = new MapItemAdapter(this, items, PreferencesUtils.getMapUris());

        binding.mapList.setAdapter(adapter);
        binding.mapList.setOnItemClickListener((listview, view, position, id) -> {
            var itemBinding = (MapItemBinding) view.getTag();
            itemBinding.checkbox.setChecked(!itemBinding.checkbox.isChecked());
            itemBinding.checkbox.callOnClick();
        });
        binding.mapList.setOnItemLongClickListener((parent, view, position, id) -> {
            var fileItem = items.get(position);
            if (fileItem.file() == null && fileItem.documentFile() == null) {
                // online map can't be deleted
                return false;
            }
            new AlertDialog.Builder(MapSelectionActivity.this)
                    .setIcon(R.drawable.ic_logo_color_24dp)
                    .setTitle(R.string.app_name)
                    .setMessage(getString(R.string.delete_map_question, fileItem.name()))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        boolean deleted;
                        if (fileItem.file() != null) {
                            deleted = fileItem.file().delete();
                        } else {
                            deleted = fileItem.documentFile().delete();
                        }
                        if (deleted) {
                            items.remove(position);
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(MapSelectionActivity.this, R.string.delete_map_error, Toast.LENGTH_LONG).show();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            navigateUp();
            return true;
        }
        return false;
    }

    public void navigateUp() {
        PreferencesUtils.setMapUris(adapter.getSelectedUris());
        finish();
    }

}