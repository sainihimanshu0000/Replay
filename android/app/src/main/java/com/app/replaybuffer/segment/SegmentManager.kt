package com.app.replaybuffer.segment

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import com.app.replaybuffer.projection.ProjectionManager
import com.app.replaybuffer.utils.Constants
import com.app.replaybuffer.utils.Logger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Records the screen in fixed-length MP4 segments.
 *
 * All MediaRecorder / VirtualDisplay operations run on a dedicated background
 * thread (never the main thread) to avoid ANRs and keep capture stable while the
 * app is backgrounded. A single VirtualDisplay is kept alive for the whole
 * session; rotation only swaps the recorder output surface.
 *
 * Segments are written to a ".part" file and renamed to ".mp4" only after a
 * successful stop, so a half-written file is never merged.
 */
class SegmentManager(
    private val context: Context,
    private val projectionManager: ProjectionManager
) {

    companion object {
        private const val RECORDING_SUFFIX = ".part"
        private const val MIN_VALID_SEGMENT_BYTES = 8_192L
        private const val STOP_WAIT_MS = 4_000L
    }

    private val recorderThread = HandlerThread("ReplayRecorderThread").apply { start() }
    private val recorderHandler = Handler(recorderThread.looper)

    private var mediaRecorder: MediaRecorder? = null
    private var currentSegmentPartPath: String? = null
    private val completedSegments = Collections.synchronizedList(mutableListOf<String>())

    @Volatile
    private var isRecording = false

    private var onSegmentCompleteCallback: ((String) -> Unit)? = null
    private var onFatalErrorCallback: ((String) -> Unit)? = null

    private val rotationRunnable = Runnable { rotateSegment() }

    fun startRecording(
        onSegmentComplete: (String) -> Unit,
        onFatalError: (String) -> Unit = {}
    ) {
        if (isRecording) {
            Logger.warn(Constants.LOG_SEGMENT_TAG, "Already recording")
            return
        }
        if (projectionManager.getMediaProjection() == null) {
            onFatalError("Screen capture session is not available")
            return
        }

        isRecording = true
        onSegmentCompleteCallback = onSegmentComplete
        onFatalErrorCallback = onFatalError
        recorderHandler.post { startNewSegment(createDisplay = true) }
    }

    /**
     * Finalize the in-progress segment so it is a valid MP4, then (if still
     * recording) immediately begin a new one so buffering continues. [onComplete]
     * runs on the recorder thread once the current segment is on disk.
     */
    fun finalizeCurrentSegment(onComplete: () -> Unit) {
        recorderHandler.post {
            val finalized = finalizeSegmentFile()
            finalized?.let { onSegmentCompleteCallback?.invoke(it) }
            if (isRecording && projectionManager.getMediaProjection() != null) {
                startNewSegment(createDisplay = false)
            }
            onComplete()
        }
    }

    fun stopRecording() {
        isRecording = false
        recorderHandler.removeCallbacks(rotationRunnable)

        val latch = CountDownLatch(1)
        recorderHandler.post {
            try {
                finalizeSegmentFile()
                projectionManager.setSurface(null)
            } catch (e: Exception) {
                Logger.error(Constants.LOG_SEGMENT_TAG, "Error during stop", e)
            } finally {
                latch.countDown()
            }
        }

        try {
            if (!latch.await(STOP_WAIT_MS, TimeUnit.MILLISECONDS)) {
                Logger.warn(Constants.LOG_SEGMENT_TAG, "Timed out waiting for final segment")
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        onSegmentCompleteCallback = null
        onFatalErrorCallback = null
        recorderThread.quitSafely()
        Logger.info(
            Constants.LOG_SEGMENT_TAG,
            "Recording stopped. Total segments: ${completedSegments.size}"
        )
    }

    fun getCompletedSegments(): List<String> = ArrayList(completedSegments)

    fun getSegmentDirectory(): File {
        val segmentsDir = File(context.getExternalFilesDir(null), Constants.SEGMENTS_DIRECTORY)
        if (!segmentsDir.exists()) segmentsDir.mkdirs()
        return segmentsDir
    }

    // --- recorder-thread only below ---

    private fun startNewSegment(createDisplay: Boolean) {
        if (!isRecording) return

        var recorder: MediaRecorder? = null
        var partFile: File? = null
        try {
            partFile = createPartSegmentFile()
            currentSegmentPartPath = partFile.absolutePath
            val (width, height, density) = getCaptureDimensions()

            recorder = createMediaRecorder()
            val hasAudio = configureRecorder(recorder, partFile.absolutePath, width, height)
            recorder.prepare()

            val surface = recorder.surface
            if (createDisplay) {
                if (projectionManager.createVirtualDisplay(surface, width, height, density) == null) {
                    throw IllegalStateException("Failed to create VirtualDisplay")
                }
            } else {
                projectionManager.setSurface(surface)
            }

            recorder.start()
            mediaRecorder = recorder
            Logger.info(
                Constants.LOG_SEGMENT_TAG,
                "Segment started: ${partFile.name} (${width}x$height, ${if (hasAudio) "a/v" else "video"})"
            )

            recorderHandler.postDelayed(rotationRunnable, Constants.SEGMENT_DURATION_MILLIS)
        } catch (e: Exception) {
            Logger.error(Constants.LOG_SEGMENT_TAG, "Error starting segment", e)
            safeReleaseRecorder(recorder)
            mediaRecorder = null
            currentSegmentPartPath = null
            partFile?.delete()
            isRecording = false
            onFatalErrorCallback?.invoke(e.message ?: "Recording failed")
        }
    }

    private fun rotateSegment() {
        if (!isRecording) return
        val finalized = finalizeSegmentFile()
        finalized?.let { onSegmentCompleteCallback?.invoke(it) }
        startNewSegment(createDisplay = false)
    }

    private fun finalizeSegmentFile(): String? {
        recorderHandler.removeCallbacks(rotationRunnable)

        val recorder = mediaRecorder
        val partPath = currentSegmentPartPath
        mediaRecorder = null
        currentSegmentPartPath = null

        if (recorder == null) return null

        // Detach the surface before stopping so the display isn't writing to a dying surface
        projectionManager.setSurface(null)

        var stoppedCleanly = true
        try {
            recorder.stop()
        } catch (e: Exception) {
            // Thrown when too few frames were captured; the file is unusable
            stoppedCleanly = false
            Logger.warn(Constants.LOG_SEGMENT_TAG, "recorder.stop() failed: ${e.message}")
        } finally {
            safeReleaseRecorder(recorder)
        }

        if (partPath == null) return null
        val partFile = File(partPath)

        if (!stoppedCleanly || !partFile.exists() || partFile.length() < MIN_VALID_SEGMENT_BYTES) {
            Logger.warn(
                Constants.LOG_SEGMENT_TAG,
                "Discarding invalid segment (${if (partFile.exists()) partFile.length() else -1} bytes)"
            )
            partFile.delete()
            return null
        }

        val finalFile = File(partPath.removeSuffix(RECORDING_SUFFIX) + Constants.FILE_EXTENSION)
        return if (partFile.renameTo(finalFile)) {
            completedSegments.add(finalFile.absolutePath)
            Logger.info(Constants.LOG_SEGMENT_TAG, "Segment finalized: ${finalFile.name}")
            finalFile.absolutePath
        } else {
            Logger.error(Constants.LOG_SEGMENT_TAG, "Failed to rename ${partFile.name}")
            partFile.delete()
            null
        }
    }

    private fun safeReleaseRecorder(recorder: MediaRecorder?) {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (e: Exception) {
            Logger.error(Constants.LOG_SEGMENT_TAG, "Error releasing recorder", e)
        }
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun configureRecorder(
        recorder: MediaRecorder,
        outputPath: String,
        width: Int,
        height: Int
    ): Boolean {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        if (hasAudioPermission) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setAudioSamplingRate(44_100)
        }
        recorder.setVideoSize(width, height)
        recorder.setVideoFrameRate(Constants.VIDEO_FRAME_RATE)
        recorder.setVideoEncodingBitRate(Constants.VIDEO_BITRATE)
        recorder.setOutputFile(outputPath)
        return hasAudioPermission
    }

    private fun createPartSegmentFile(): File {
        val segmentsDir = getSegmentDirectory()
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(Date())
        return File(segmentsDir, "${Constants.SEGMENT_FILE_PREFIX}$timestamp$RECORDING_SUFFIX")
    }

    private fun getCaptureDimensions(): Triple<Int, Int, Int> {
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels.coerceAtLeast(1)
        val screenHeight = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi.coerceAtLeast(1)

        var targetWidth = Constants.VIDEO_WIDTH.coerceAtMost(screenWidth)
        var targetHeight = (screenHeight.toLong() * targetWidth / screenWidth).toInt()
        if (targetWidth % 2 != 0) targetWidth -= 1
        if (targetHeight % 2 != 0) targetHeight -= 1
        return Triple(targetWidth.coerceAtLeast(2), targetHeight.coerceAtLeast(2), density)
    }

    fun clearSegments() {
        completedSegments.clear()
        getSegmentDirectory().listFiles()?.forEach { file ->
            if (file.name.startsWith(Constants.SEGMENT_FILE_PREFIX)) file.delete()
        }
        Logger.info(Constants.LOG_SEGMENT_TAG, "All segments cleared")
    }
}
