package de.storchp.opentracks.osmplugin;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;
import de.storchp.opentracks.osmplugin.utils.FileItem;
import de.storchp.opentracks.osmplugin.utils.FileItemAdapter;

public class MapSelectionActivity extends AppCompatActivity {

    private static final String TAG = MapSelectionActivity.class.getSimpleName();

    private ListView listView;
    private FileItemAdapter adapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_selection);

        final Toolbar toolbar = findViewById(R.id.maps_toolbar);
        toolbar.setTitle(R.string.map_selection);
        setSupportActionBar(toolbar);

        final List<FileItem> items = new ArrayList<>();
        items.add(new FileItem(getString(R.string.online_osm_mapnick), null));
        final Uri mapDirectory = PreferencesUtils.getMapDirectoryUri(this);
        if (mapDirectory != null) {
            final DocumentFile documentsTree = getDocumentFileFromTreeUri(mapDirectory);
            if (documentsTree != null) {
                for (final DocumentFile file : documentsTree.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".map")) {
                        items.add(new FileItem(file.getName(), file.getUri()));
                    }
                }
            }
        }
        final Set<Uri> selected = PreferencesUtils.getMapUris(this);
        adapter = new FileItemAdapter(this, items, selected);

        listView = findViewById(R.id.map_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> listview, final View view, final int position, final long id) {
                final FileItemAdapter.ViewHolder holder = (FileItemAdapter.ViewHolder) view.getTag();
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
                holder.checkBox.callOnClick();
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                final FileItemAdapter.ViewHolder holder = (FileItemAdapter.ViewHolder) view.getTag();
                final FileItem fileItem = items.get(position);
                final Uri uri = fileItem.getUri();
                if (uri == null) {
                    // online map can't be deleted
                    return false;
                }
                new AlertDialog.Builder(MapSelectionActivity.this)
                        .setIcon(R.drawable.ic_logo_color_24dp)
                        .setTitle(R.string.app_name)
                        .setMessage(getString(R.string.delete_map_question, fileItem.getName()))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                Log.d(TAG, "Delete " + fileItem.getName());
                                final DocumentFile file = getDocumentFileFromTreeUri(uri);
                                final boolean deleted = file.delete();
                                if (deleted) {
                                    items.remove(position);
                                    selected.remove(uri);
                                    adapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(MapSelectionActivity.this, R.string.delete_map_error, Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create().show();
                return false;
            }
        });
    }

    protected DocumentFile getDocumentFileFromTreeUri(final Uri uri) {
        try {
            return DocumentFile.fromTreeUri(getApplication(), uri);
        } catch (final Exception e) {
            Log.w(TAG, "Error getting DocumentFile from Uri: " + uri);
        }
        return null;
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
        final Set<Uri> selectedUris = adapter.getSelectedUris();
        PreferencesUtils.setMapUris(this, selectedUris);

        super.onBackPressed();
    }

}