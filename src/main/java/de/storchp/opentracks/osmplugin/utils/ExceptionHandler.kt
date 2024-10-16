package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.storchp.opentracks.osmplugin.BuildConfig;
import de.storchp.opentracks.osmplugin.ShowErrorActivity;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultExceptionHandler;

    public ExceptionHandler(Context context, Thread.UncaughtExceptionHandler defaultExceptionHandler) {
        this.context = context;
        this.defaultExceptionHandler = defaultExceptionHandler;
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable exception) {
        try {
            var errorReport = generateErrorReport(formatException(thread, exception));
            var intent = new Intent(context, ShowErrorActivity.class);
            intent.putExtra(ShowErrorActivity.EXTRA_ERROR_TEXT, errorReport);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            // Pass exception to OS for graceful handling - OS will report it via ADB
            // and close all activities and services.
            defaultExceptionHandler.uncaughtException(thread, exception);
        } catch (Exception fatalException) {
            // do not recurse into custom handler if exception is thrown during
            // exception handling. Pass this ultimate fatal exception to OS
            defaultExceptionHandler.uncaughtException(thread, fatalException);
        }
    }

    private String formatException(Thread thread, Throwable exception) {
        var stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("Exception in thread \"%s\": ", thread.getName()));

        // print available stacktrace
        var writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        stringBuilder.append(writer);

        return stringBuilder.toString();
    }

    private String generateErrorReport(String stackTrace) {
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
            "```\n";
    }

}
