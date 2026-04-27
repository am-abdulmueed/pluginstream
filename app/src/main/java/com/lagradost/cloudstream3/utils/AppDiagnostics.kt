package com.lagradost.cloudstream3.utils

import android.os.Build
import android.util.Log
import com.lagradost.cloudstream3.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppDiagnostics {
    private const val TAG = "AppDiagnostics"

    /**
     * Reads recent Logcat entries for this app's process.
     * 
     * @param maxLines Maximum number of log lines to return.
     */
    fun readLogs(maxLines: Int = 100): String {
        return try {
            val pid = android.os.Process.myPid()
            val process = ProcessBuilder(
                "logcat", "-d", "-t", maxLines.toString(), "--pid=$pid", "*:D"
            )
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            
            if (output.isBlank()) "No logs found for this session." else output
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read logs", e)
            "Unable to read logs: ${e.message}"
        }
    }

    /**
     * Builds a device info summary.
     */
    fun getDeviceInfo(): String = buildString {
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
    }
}
