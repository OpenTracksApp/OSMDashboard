package de.storchp.opentracks.osmplugin;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.android.material.color.DynamicColors;

import de.storchp.opentracks.osmplugin.utils.ExceptionHandler;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

/**
 * Code that is executed when the application starts.
 * <p>
 * NOTE: How often actual application startup happens depends on the OS.
 * Not every start of an activity will trigger this.
 */
public class Startup extends Application {

    private static final String TAG = Startup.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        //Include version information into stack traces.
        Log.i(TAG, BuildConfig.APPLICATION_ID + "; BuildType: " + BuildConfig.BUILD_TYPE + "; VersionName: " + BuildConfig.VERSION_NAME + "/" + " VersionCode: " + BuildConfig.VERSION_CODE);

        PreferencesUtils.initPreferences(this);
        PreferencesUtils.applyNightMode();
        if (PreferencesUtils.shouldUseDynamicColors()) {
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // handle crashes only outside the crash reporter activity/process
        if (!isCrashReportingProcess()) {
            Thread.setDefaultUncaughtExceptionHandler(
                    new ExceptionHandler(this, Thread.getDefaultUncaughtExceptionHandler()));
        }
    }

    private boolean isCrashReportingProcess() {
        var processName = "";
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // Using the same technique as Application.getProcessName() for older devices
            // Using reflection since ActivityThread is an internal API
            try {
                @SuppressLint("PrivateApi")
                var activityThread = Class.forName("android.app.ActivityThread");
                @SuppressLint("DiscouragedPrivateApi")
                var getProcessName = activityThread.getDeclaredMethod("currentProcessName");
                processName = (String) getProcessName.invoke(null);
            } catch (Exception ignored) {
            }
        } else {
            processName = Application.getProcessName();
        }
        return processName != null && processName.endsWith(":crash");
    }

}
