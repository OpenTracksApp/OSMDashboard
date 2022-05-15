package de.storchp.opentracks.osmplugin;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

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
        //In debug builds: show thread and VM warnings.
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Enabling strict mode");
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }

    @Override
    protected void attachBaseContext(final Context base) {
        super.attachBaseContext(base);

        // we don't want to handle crashes occurring inside crash reporter activity/process;
        // let the platform deal with those
        final boolean isCrashReportingProcess = getAppProcessName().endsWith(":crash");

        if (!isCrashReportingProcess) {
            final var defaultPlatformHandler = Thread.getDefaultUncaughtExceptionHandler();
            final ExceptionHandler crashReporter = new ExceptionHandler(this,
                    defaultPlatformHandler);
            Thread.setDefaultUncaughtExceptionHandler(crashReporter);
        }
    }

    private String getAppProcessName() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            final var manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            final int ownPid = android.os.Process.myPid();
            final var processes = manager.getRunningAppProcesses();
            if (processes != null) {
                return processes.stream()
                        .filter(info -> info.pid == ownPid)
                        .findFirst()
                        .map(info -> info.processName)
                        .orElse("");
            }
        } else {
            return Application.getProcessName();
        }
        return "";
    }

}
