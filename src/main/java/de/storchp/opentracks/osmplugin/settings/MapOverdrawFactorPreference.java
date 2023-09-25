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

public class MapOverdrawFactorPreference extends DialogPreference {

    public MapOverdrawFactorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.overdraw_factor_dialog);

        setDialogTitle(R.string.overdraw_factor);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        SummaryProvider<DialogPreference> summaryProvider = preference -> format(PreferencesUtils.getOverdrawFactor());
        setSummaryProvider(summaryProvider);
    }

    @NonNull
    public static String format(final double factor) {
        return String.format(Locale.getDefault(), "%.2f", factor);
    }

    public static class MapOverdrawFactorPreferenceDialog extends PreferenceDialogFragmentCompat {

        TextView tvOverdrawFactor;

        SeekBar sbOverdrawFactor;

        static MapOverdrawFactorPreferenceDialog newInstance(String preferenceKey) {
            var dialog = new MapOverdrawFactorPreferenceDialog();
            var bundle = new Bundle(1);
            bundle.putString(PreferenceDialogFragmentCompat.ARG_KEY, preferenceKey);
            dialog.setArguments(bundle);
            return dialog;
        }

        @Override
        protected void onBindDialogView(@NonNull View view) {
            super.onBindDialogView(view);
            var currentOverdrawFactor = PreferencesUtils.getOverdrawFactor();

            tvOverdrawFactor = view.findViewById(R.id.tv_overdraw_factor);
            tvOverdrawFactor.setText(format(currentOverdrawFactor));

            sbOverdrawFactor = view.findViewById(R.id.sb_overdraw_factor);
            sbOverdrawFactor.setProgress((int) (100 * currentOverdrawFactor) - 100);
            sbOverdrawFactor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    tvOverdrawFactor.setText(format(getOverdrawFactorFromProgress()));
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
                var newOverdrawFactor = getOverdrawFactorFromProgress();
                if (getPreference().callChangeListener(newOverdrawFactor)) {
                    PreferencesUtils.setOverdrawFactor(newOverdrawFactor);
                    HackUtils.invalidatePreference(getPreference());
                }
            }
        }

        private double getOverdrawFactorFromProgress() {
            int progress = sbOverdrawFactor.getProgress();
            if (progress == 0) {
                return 1;
            }
            return (progress / (double) 100) + 1;
        }
    }
}
