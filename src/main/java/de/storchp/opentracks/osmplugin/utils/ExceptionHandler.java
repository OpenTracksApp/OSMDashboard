package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Arrays;

import de.storchp.opentracks.osmplugin.BuildConfig;
import de.storchp.opentracks.osmplugin.ShowErrorActivity;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final int EXCEPTION_FORMAT_MAX_RECURSIVITY = 10;

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultExceptionHandler;

    public ExceptionHandler(final Context context, final Thread.UncaughtExceptionHandler defaultExceptionHandler) {
        this.context = context;
        this.defaultExceptionHandler = defaultExceptionHandler;
    }

    @Override
    public void uncaughtException(@NonNull final Thread thread, @NonNull final Throwable exception) {
        try {
            final var errorReport = generateErrorReport(formatException(thread, exception));
            final var intent = new Intent(context, ShowErrorActivity.class);
            intent.putExtra(ShowErrorActivity.EXTRA_ERROR_TEXT, errorReport);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            // Pass exception to OS for graceful handling - OS will report it via ADB
            // and close all activities and services.
            defaultExceptionHandler.uncaughtException(thread, exception);
        } catch (final Exception fatalException) {
            // do not recurse into custom handler if exception is thrown during
            // exception handling. Pass this ultimate fatal exception to OS
            defaultExceptionHandler.uncaughtException(thread, fatalException);
        }
    }

    private String formatException(final Thread thread, final Throwable exception) {
        return formatExceptionRecursive(thread, exception, 0);
    }

    private String formatExceptionRecursive(final Thread thread, final Throwable exception, final int count) {
        if (count > EXCEPTION_FORMAT_MAX_RECURSIVITY) {
            return "Max number of recursive exception causes exceeded!";
        }
        // print exception
        final var stringBuilder = new StringBuilder();
        final var stackTrace = exception.getStackTrace();
        stringBuilder.append(String.format("Exception in thread \"%s\" %s\n", thread.getName(), exception));
        // print available stacktrace
        Arrays.stream(stackTrace).forEach(element -> stringBuilder.append("    at ").append(element).append("\n"));

        // print cause recursively
        if (exception.getCause() != null) {
            stringBuilder.append("Caused by: ");
            stringBuilder.append(formatExceptionRecursive(thread, exception.getCause(), count + 1));
        }
        return stringBuilder.toString();
    }

    private String generateErrorReport(final String stackTrace) {
        return "### Cause of error\n" +
            "```java\n" +
            stackTrace + "\n" +
            "```\n" +
            "\n" +
            "### App information\n" +
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
            "* Incremental: " + Build.VERSION.INCREMENTAL + "\n";
    }

}
