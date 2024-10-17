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
import java.lang.StringBuilder

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

    private fun formatException(thread: Thread, exception: Throwable): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(String.format("Exception in thread \"%s\": ", thread.getName()))

        // print available stacktrace
        val writer = StringWriter()
        exception.printStackTrace(PrintWriter(writer))
        stringBuilder.append(writer)

        return stringBuilder.toString()
    }

    private fun generateErrorReport(stackTrace: String?): String {
        return "### App information\n" +
                "* ID: " + BuildConfig.APPLICATION_ID + "\n" +
                "* Version: " + BuildConfig.VERSION_CODE + " " + BuildConfig.VERSION_NAME + "\n" +
                "* Build flavor: " + BuildConfig.FLAVOR + "\n" +
                "\n" +
                "### Device information\n" +
                "* Brand: " + Build.BRAND + "\n" +
                "* Device: " + Build.DEVICE + "\n" +
                "* Model: " + Build.MODEL + "\n" +
                "* Id: " + Build.ID + "\n" +
                "* Product: " + Build.PRODUCT + "\n" +
                "\n" +
                "### Firmware\n" +
                "* SDK: " + Build.VERSION.SDK_INT + "\n" +
                "* Release: " + Build.VERSION.RELEASE + "\n" +
                "* Incremental: " + Build.VERSION.INCREMENTAL + "\n" +
                "\n" +
                "### Cause of error\n" +
                "```java\n" +
                stackTrace + "\n" +
                "```\n"
    }
}
