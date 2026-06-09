package com.app.replaybuffer.utils

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes uncaught exceptions to a file so crashes can be diagnosed even when
 * a live logcat connection is unavailable (e.g. flaky USB / locked device).
 *
 * Log location: <app external files>/crash_log.txt
 * Pull with: adb shell run-as com.replay cat files/crash_log.txt
 * or:        adb pull /sdcard/Android/data/com.replay/files/crash_log.txt
 */
object CrashLogger {

    private const val FILE_NAME = "crash_log.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrash(appContext, thread, throwable)
            } catch (_: Throwable) {
                // Never let the logger itself mask the original crash
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, FILE_NAME)

        val stackWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stackWriter))

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val entry = buildString {
            appendLine("==================================================")
            appendLine("CRASH @ $timestamp")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name}")
            appendLine("Message: ${throwable.message}")
            appendLine(stackWriter.toString())
            appendLine()
        }

        file.appendText(entry)
        Logger.error(Constants.LOG_TAG, "Uncaught crash written to ${file.absolutePath}\n$entry")
    }
}
