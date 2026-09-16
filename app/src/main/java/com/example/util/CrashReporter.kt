package com.example.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashReporter {
    private const val TAG = "CrashReporter"

    private val crashlytics: FirebaseCrashlytics? by lazy {
        try {
            FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseCrashlytics not initialized: ${e.message}")
            null
        }
    }

    /**
     * Records a non-fatal exception to Crashlytics and logs to Android logcat.
     */
    fun recordException(throwable: Throwable, contextTag: String? = null) {
        Log.e(contextTag ?: TAG, "Tracked error: ${throwable.message}", throwable)
        try {
            if (contextTag != null) {
                crashlytics?.setCustomKey("error_context", contextTag)
            }
            crashlytics?.recordException(throwable)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record exception to Crashlytics", e)
        }
    }

    /**
     * Adds a breadcrumb log to Crashlytics.
     */
    fun log(message: String) {
        Log.d(TAG, message)
        try {
            crashlytics?.log(message)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send log to Crashlytics", e)
        }
    }

    /**
     * Sets a custom key-value attribute in Crashlytics reports.
     */
    fun setCustomKey(key: String, value: String) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set custom key in Crashlytics", e)
        }
    }

    /**
     * Configures the global uncaught exception handler to ensure all fatal crashes
     * are logged to Crashlytics before terminating.
     */
    fun initUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordException(throwable, "UncaughtException_${thread.name}")
                crashlytics?.sendUnsentReports()
            } catch (e: Exception) {
                Log.e(TAG, "Error in uncaught exception handler", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
        log("CrashReporter initialized successfully.")
    }
}
