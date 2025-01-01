package de.storchp.opentracks.osmplugin.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import de.storchp.opentracks.osmplugin.BuildConfig
import de.storchp.opentracks.osmplugin.EXTRA_ERROR_TEXT
import de.storchp.opentracks.osmplugin.ShowErrorActivity
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Exception

class ExceptionHandler(
    private val context: Context,
    private val defaultExceptionHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            val errorReport = generateErrorReport(formatException(thread, exception))
            val intent = Intent(context, ShowErrorActivity::class.java)
            intent.putExtra(EXTRA_ERROR_TEXT, errorReport)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            // Pass exception to OS for graceful handling - OS will report it via ADB
            // and close all activities and services.
            defaultExceptionHandler?.uncaughtException(thread, exception)
        } catch (fatalException: Exception) {
            // do not recurse into custom handler if exception is thrown during
            // exception handling. Pass this ultimate fatal exception to OS
            defaultExceptionHandler?.uncaughtException(thread, fatalException)
        }
    }

    private fun formatException(thread: Thread, exception: Throwable) =
        buildString {
            append("Exception in thread \"${thread.name}\": ")

            // print available stacktrace
            val writer = StringWriter()
            exception.printStackTrace(PrintWriter(writer))
            append(writer)
        }

    private fun generateErrorReport(stackTrace: String) =
        """
            ### App information
            * ID: ${BuildConfig.APPLICATION_ID}
            * Version: ${BuildConfig.VERSION_CODE} ${BuildConfig.VERSION_NAME}
            * Build flavor: ${BuildConfig.FLAVOR}
            
            ### Device information
            * Brand: ${Build.BRAND}
            * Device: ${Build.DEVICE}
            * Model: ${Build.MODEL}
            * Id: ${Build.ID}
            * Product: ${Build.PRODUCT}
            
            ### Firmware
            * SDK: ${Build.VERSION.SDK_INT}
            * Release: ${Build.VERSION.RELEASE}
            * Incremental: ${Build.VERSION.INCREMENTAL}
            
            ### Cause of error
            ```java
            $stackTrace
            ```
        """.trimIndent()

}
