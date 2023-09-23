package de.storchp.opentracks.osmplugin;

import static java.util.stream.Collectors.joining;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import de.storchp.opentracks.osmplugin.databinding.ActivitySettingsBinding;
import de.storchp.opentracks.osmplugin.utils.FileUtil;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings, new SettingsFragment())
            .commit();

        setSupportActionBar(binding.toolbar.mapsToolbar);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            var onlineMapConsentPreference = findPreference(getString(R.string.APP_PREF_ONLINE_MAP_CONSENT));
            if (onlineMapConsentPreference != null && BuildConfig.offline) {
                onlineMapConsentPreference.setVisible(false);
            }

            var mapDownloadPreference = findPreference(getString(R.string.APP_PREF_MAP_DOWNLOAD));
            if (mapDownloadPreference != null && BuildConfig.offline) {
                mapDownloadPreference.setVisible(false);
            }

            setSummaries();
        }

        @Override
        public void onResume() {
            super.onResume();
            setSummaries();
        }

        private void setSummaries() {
            var mapsPreference = findPreference(getString(R.string.APP_PREF_MAP_SELECTION));
            if (mapsPreference != null) {
                mapsPreference.setSummaryProvider((Preference.SummaryProvider<Preference>) preference -> {
                    var mapUris = PreferencesUtils.getMapUris();
                    if (mapUris.isEmpty() && !BuildConfig.offline) {
                        return getString(R.string.online_osm_mapnick);
                    }
                    return mapUris.stream()
                            .map(uri -> FileUtil.getDocumentFileFromTreeUri(getContext(), uri))
                            .filter(Objects::nonNull)
                            .map(DocumentFile::getName)
                            .collect(joining(", "));
                });
            }

            var mapDirectoryPreference = findPreference(getString(R.string.APP_PREF_MAP_DIRECTORY));
            if (mapDirectoryPreference != null) {
                mapDirectoryPreference.setSummaryProvider((Preference.SummaryProvider<Preference>) preference -> {
                    var uri = PreferencesUtils.getMapDirectoryUri();
                    return uri != null ? uri.getLastPathSegment() : null;
                });
            }

            var themePreference = findPreference(getString(R.string.APP_PREF_MAP_THEME));
            if (themePreference != null) {
                themePreference.setSummaryProvider((Preference.SummaryProvider<Preference>) preference -> {
                    var themeUri = PreferencesUtils.getMapThemeUri();
                    if (themeUri == null) {
                        return getString(R.string.default_theme);
                    }
                    var documentFile = FileUtil.getDocumentFileFromTreeUri(getContext(), themeUri);
                    if (documentFile == null) {
                        return getString(R.string.default_theme);
                    }
                    return documentFile.getName();
                });
            }

            var themeDirectoryPreference = findPreference(getString(R.string.APP_PREF_MAP_THEME_DIRECTORY));
            if (themeDirectoryPreference != null) {
                themeDirectoryPreference.setSummaryProvider((Preference.SummaryProvider<Preference>) preference -> {
                    var uri = PreferencesUtils.getMapThemeDirectoryUri();
                    return uri != null ? uri.getLastPathSegment() : null;
                });
            }
        }

    }

}