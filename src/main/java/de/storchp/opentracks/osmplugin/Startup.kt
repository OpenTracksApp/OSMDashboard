package de.storchp.opentracks.osmplugin

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.material.color.DynamicColors
import de.storchp.opentracks.osmplugin.utils.ExceptionHandler
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils
import java.lang.Exception

/**
 * Code that is executed when the application starts.
 *
 *
 * NOTE: How often actual application startup happens depends on the OS.
 * Not every start of an activity will trigger this.
 */
class Startup : Application() {
    override fun onCreate() {
        super.onCreate()

        //Include version information into stack traces.
        Log.i(
            Startup.Companion.TAG,
            BuildConfig.APPLICATION_ID + "; BuildType: " + BuildConfig.BUILD_TYPE + "; VersionName: " + BuildConfig.VERSION_NAME + "/" + " VersionCode: " + BuildConfig.VERSION_CODE
        )

        PreferencesUtils.initPreferences(this)
        PreferencesUtils.applyNightMode()
        if (PreferencesUtils.shouldUseDynamicColors()) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        // handle crashes only outside the crash reporter activity/process
        if (!isCrashReportingProcess()) {
            Thread.setDefaultUncaughtExceptionHandler(
                ExceptionHandler(this, Thread.getDefaultUncaughtExceptionHandler())
            )
        }
    }

    private fun isCrashReportingProcess(): Boolean {
        var processName: String? = ""
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // Using the same technique as Application.getProcessName() for older devices
            // Using reflection since ActivityThread is an internal API
            try {
                @SuppressLint("PrivateApi") val activityThread =
                    Class.forName("android.app.ActivityThread")
                @SuppressLint("DiscouragedPrivateApi") val getProcessName =
                    activityThread.getDeclaredMethod("currentProcessName")
                processName = getProcessName.invoke(null) as String?
            } catch (ignored: Exception) {
            }
        } else {
            processName = getProcessName()
        }
        return processName != null && processName.endsWith(":crash")
    }

    companion object {
        private val TAG: String = Startup::class.java.getSimpleName()
    }
}
