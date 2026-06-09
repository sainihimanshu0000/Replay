package com.app.replaybuffer.merger

import android.content.Context
import com.app.replaybuffer.utils.Constants
import com.app.replaybuffer.utils.Logger
import java.io.File

/**
 * FFmpegKit-based merger — enable by adding a local ffmpeg-kit AAR to `app/libs/`:
 *
 * ```
 * implementation(files("libs/ffmpeg-kit-min-6.0-2.aar"))
 * implementation("com.arthenica:smart-exception-java:0.2.1")
 * ```
 *
 * Then uncomment the FFmpegKit imports and implementation below.
 */
class FfmpegReplayMerger(private val context: Context) {

    fun mergeSegments(
        segments: List<String>,
        outputPath: String,
        callback: (Boolean, String?) -> Unit
    ) {
        if (segments.isEmpty()) {
            callback(false, "No segments to merge")
            return
        }

        // Fallback until FFmpegKit AAR is added locally (archived on Maven Central)
        Logger.warn(
            Constants.LOG_MERGER_TAG,
            "FfmpegReplayMerger not configured — falling back to MediaMuxer"
        )
        ReplayMerger().mergeSegments(segments, outputPath, callback)
    }

    /*
    fun mergeSegments(
        segments: List<String>,
        outputPath: String,
        callback: (Boolean, String?) -> Unit
    ) {
        Thread {
            var listFile: File? = null
            try {
                listFile = File.createTempFile("ffmpeg_concat_", ".txt", context.cacheDir)
                listFile.writeText(
                    segments.joinToString("\n") { "file '${it.replace("'", "'\\''")}'" }
                )
                val command =
                    "-y -f concat -safe 0 -i ${listFile.absolutePath} -c copy $outputPath"
                val session = FFmpegKit.execute(command)
                val success = ReturnCode.isSuccess(session.returnCode)
                callback(success, if (success) outputPath else session.output)
            } catch (e: Exception) {
                callback(false, e.message)
            } finally {
                listFile?.delete()
            }
        }.start()
    }
    */
}
