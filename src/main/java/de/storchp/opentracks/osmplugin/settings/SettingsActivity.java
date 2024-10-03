package de.storchp.opentracks.osmplugin.settings;

import static java.util.stream.Collectors.joining;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import de.storchp.opentracks.osmplugin.BuildConfig;
import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.databinding.ActivitySettingsBinding;
import de.storchp.opentracks.osmplugin.download.DownloadMapSelectionActivity;
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
        private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (PreferencesUtils.isKey(R.string.night_mode_key, key)) {
                getActivity().runOnUiThread(PreferencesUtils::applyNightMode);
            }
        };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            if (BuildConfig.offline) {
                var onlineMapConsentPreference = findPreference(getString(R.string.APP_PREF_ONLINE_MAP_CONSENT));
                if (onlineMapConsentPreference != null) {
                    onlineMapConsentPreference.setVisible(false);
                }

                var mapDownloadPreference = findPreference(getString(R.string.APP_PREF_MAP_DOWNLOAD));
                if (mapDownloadPreference != null) {
                    mapDownloadPreference.setVisible(false);
                }
            }

            var dynamicColors = findPreference(getString(R.string.settings_ui_dynamic_colors_key));
            if (dynamicColors != null) {
                dynamicColors.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU);
            }
            setSummaries();
        }

        @Override
        public void onResume() {
            super.onResume();
            setSummaries();
            PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        }

        @Override
        public void onPause() {
            super.onPause();
            PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
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
                            .map(uri -> FileUtil.getFilenameFromUri(getContext(), uri))
                            .filter(Objects::nonNull)
                            .collect(joining(", "));
                });
            }

            var mapDownloadPreference = findPreference(getString(R.string.APP_PREF_MAP_DOWNLOAD));
            if (mapDownloadPreference != null) {
                mapDownloadPreference.setSummary(getString(R.string.map_download_summary, DownloadMapSelectionActivity.MAPS_V_5));
            }

            var mapDirectoryPreference = findPreference(getString(R.string.APP_PREF_MAP_DIRECTORY));
            if (mapDirectoryPreference != null) {
                mapDirectoryPreference.setSummaryProvider((Preference.SummaryProvider<Preference>) preference -> {
                    var uri = PreferencesUtils.getMapDirectoryUri();
                    return uri != null ? uri.getLastPathSegment() : getString(R.string.INTERNAL_APP_STORAGE);
                });
            }

            var themePreference = findPreference(getString(R.string.APP_PREF_MAP_THEME));
            if (themePreference != null) {
                themePreference.setSummaryProvider((Preference.SummaryProvider<Preference>) preference -> {
                    var themeUri = PreferencesUtils.getMapThemeUri();
                    if (themeUri == null) {
                        return getString(R.string.default_theme);
                    }
                    var filename = FileUtil.getFilenameFromUri(getContext(), themeUri) + (themeUri.getFragment() != null ? "#" + themeUri.getFragment() : "");
                    return Objects.requireNonNullElseGet(filename, () -> getString(R.string.default_theme));
                });
            }

            var themeDirectoryPreference = findPreference(getString(R.string.APP_PREF_MAP_THEME_DIRECTORY));
            if (themeDirectoryPreference != null) {
                themeDirectoryPreference.setSummaryProvider((Preference.SummaryProvider<Preference>) preference -> {
                    var uri = PreferencesUtils.getMapThemeDirectoryUri();
                    return uri != null ? uri.getLastPathSegment() : getString(R.string.INTERNAL_APP_STORAGE);
                });
            }
        }

    }

}