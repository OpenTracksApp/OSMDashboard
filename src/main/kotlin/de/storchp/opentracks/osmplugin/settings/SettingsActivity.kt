package de.storchp.opentracks.osmplugin.settings

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import de.storchp.opentracks.osmplugin.BuildConfig
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.databinding.ActivitySettingsBinding
import de.storchp.opentracks.osmplugin.download.MAPS_V_5_DOWNLOAD_URI
import de.storchp.opentracks.osmplugin.settings.SettingsActivity.SettingsFragment
import de.storchp.opentracks.osmplugin.utils.FileUtil
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()

        setSupportActionBar(binding.toolbar.mapsToolbar)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private val sharedPreferenceChangeListener =
            OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (PreferencesUtils.isKey(R.string.night_mode_key, key)) {
                    requireActivity().runOnUiThread(Runnable { PreferencesUtils.applyNightMode() })
                }
            }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            if (BuildConfig.offline) {
                val onlineMapConsentPreference =
                    findPreference<Preference?>(getString(R.string.APP_PREF_ONLINE_MAP_CONSENT))
                if (onlineMapConsentPreference != null) {
                    onlineMapConsentPreference.isVisible = false
                }

                val mapDownloadPreference =
                    findPreference<Preference?>(getString(R.string.APP_PREF_MAP_DOWNLOAD))
                if (mapDownloadPreference != null) {
                    mapDownloadPreference.isVisible = false
                }
            }

            val dynamicColors =
                findPreference<Preference?>(getString(R.string.settings_ui_dynamic_colors_key))
            if (dynamicColors != null) {
                dynamicColors.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            }
            setSummaries()
        }

        override fun onResume() {
            super.onResume()
            setSummaries()
            PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        }

        override fun onPause() {
            super.onPause()
            PreferencesUtils.unregisterOnSharedPreferenceChangeListener(
                sharedPreferenceChangeListener
            )
        }

        private fun setSummaries() {
            val mapsPreference =
                findPreference<Preference?>(getString(R.string.APP_PREF_MAP_SELECTION))
            mapsPreference?.setSummaryProvider(SummaryProvider { preference: Preference? ->
                val mapUris = PreferencesUtils.getMapUris()
                if (mapUris.isEmpty() && !BuildConfig.offline) {
                    return@SummaryProvider getString(R.string.online_osm_mapnick)
                }
                mapUris
                    .mapNotNull { uri -> FileUtil.getFilenameFromUri(requireActivity(), uri) }
                    .joinToString(", ")
            })

            val mapDownloadPreference =
                findPreference<Preference?>(getString(R.string.APP_PREF_MAP_DOWNLOAD))
            mapDownloadPreference?.setSummary(
                getString(R.string.map_download_summary, MAPS_V_5_DOWNLOAD_URI)
            )

            val mapDirectoryPreference =
                findPreference<Preference?>(getString(R.string.APP_PREF_MAP_DIRECTORY))
            mapDirectoryPreference?.setSummaryProvider(SummaryProvider { preference: Preference? ->
                val uri = PreferencesUtils.getMapDirectoryUri()
                if (uri != null) uri.lastPathSegment else getString(R.string.INTERNAL_APP_STORAGE)
            })

            val themePreference =
                findPreference<Preference?>(getString(R.string.APP_PREF_MAP_THEME))
            themePreference?.setSummaryProvider(SummaryProvider { preference: Preference? ->
                val themeUri = PreferencesUtils.getMapThemeUri()
                if (themeUri == null) {
                    return@SummaryProvider getString(R.string.default_theme)
                }
                FileUtil.getFilenameFromUri(requireActivity(), themeUri)
                    ?.plus((if (themeUri.fragment != null) "#" + themeUri.fragment else ""))
                    ?: getString(R.string.default_theme)
            })

            val themeDirectoryPreference =
                findPreference<Preference?>(getString(R.string.APP_PREF_MAP_THEME_DIRECTORY))
            themeDirectoryPreference?.setSummaryProvider(SummaryProvider { preference: Preference? ->
                val uri = PreferencesUtils.getMapThemeDirectoryUri()
                if (uri != null) uri.lastPathSegment else getString(R.string.INTERNAL_APP_STORAGE)
            })
        }
    }
}