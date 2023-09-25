package de.storchp.opentracks.osmplugin.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.Locale;

import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public class TilecacheCapacityFactorPreference extends DialogPreference {

    public TilecacheCapacityFactorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.tilecache_capacity_factor_dialog);

        setDialogTitle(R.string.tilecache_capacity_factor);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        SummaryProvider<DialogPreference> summaryProvider = preference -> format(PreferencesUtils.getTileCacheCapacityFactor());
        setSummaryProvider(summaryProvider);
    }

    @NonNull
    public static String format(final double factor) {
        return String.format(Locale.getDefault(), "%.2f", factor);
    }

    public static class TilecacheCapacityFactorPreferenceDialog extends PreferenceDialogFragmentCompat {

        TextView tvTilecacheCapacityFactor;

        SeekBar sbTilecacheCapacityFactor;

        static TilecacheCapacityFactorPreferenceDialog newInstance(String preferenceKey) {
            var dialog = new TilecacheCapacityFactorPreferenceDialog();
            var bundle = new Bundle(1);
            bundle.putString(PreferenceDialogFragmentCompat.ARG_KEY, preferenceKey);
            dialog.setArguments(bundle);
            return dialog;
        }

        @Override
        protected void onBindDialogView(@NonNull View view) {
            super.onBindDialogView(view);
            var tileCacheCapacityFactor = PreferencesUtils.getTileCacheCapacityFactor();

            tvTilecacheCapacityFactor = view.findViewById(R.id.tv_tilecache_capacity_factor);
            tvTilecacheCapacityFactor.setText(format(tileCacheCapacityFactor));

            sbTilecacheCapacityFactor = view.findViewById(R.id.sb_tilecache_capacity_factor);
            sbTilecacheCapacityFactor.setProgress(((int) (100 * tileCacheCapacityFactor) - 100) / 3);

            sbTilecacheCapacityFactor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    tvTilecacheCapacityFactor.setText(format(getTilecacheCapacityFactorFromProgress()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // no op
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // no op
                }
            });
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult) {
                var newTilecacheCapacityFactor = getTilecacheCapacityFactorFromProgress();
                if (getPreference().callChangeListener(newTilecacheCapacityFactor)) {
                    PreferencesUtils.setTileCacheCapacityFactor(newTilecacheCapacityFactor);
                    HackUtils.invalidatePreference(getPreference());
                }
            }
        }

        private float getTilecacheCapacityFactorFromProgress() {
            int progress = sbTilecacheCapacityFactor.getProgress();
            if (progress == 0) {
                return 1;
            }
            return (progress * 3 / (float) 100) + 1;
        }
    }
}
