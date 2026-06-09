package com.app.replaybuffer.service

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.app.replaybuffer.ReplayBufferModule
import com.app.replaybuffer.merger.ReplayMerger
import com.app.replaybuffer.notification.RecordingNotification
import com.app.replaybuffer.projection.ProjectionManager
import com.app.replaybuffer.segment.SegmentCleaner
import com.app.replaybuffer.segment.SegmentManager
import com.app.replaybuffer.utils.Constants
import com.app.replaybuffer.utils.GalleryExporter
import com.app.replaybuffer.utils.Logger
import java.io.File

class ReplayBufferService : Service() {

    companion object {
        const val ACTION_START_BUFFER = "com.app.replaybuffer.START_BUFFER"
        const val ACTION_SAVE_REPLAY = "com.app.replaybuffer.SAVE_REPLAY"
        const val ACTION_STOP_BUFFER = "com.app.replaybuffer.STOP_BUFFER"
        const val ACTION_KEEP_ALIVE = "com.app.replaybuffer.KEEP_ALIVE"

        @Volatile
        var isRecordingActive: Boolean = false
    }

    private var bufferMinutes = Constants.DEFAULT_BUFFER_MINUTES
    private var isForeground = false

    private lateinit var projectionManager: ProjectionManager
    private var segmentManager: SegmentManager? = null
    private lateinit var segmentCleaner: SegmentCleaner
    private lateinit var replayMerger: ReplayMerger
    private lateinit var recordingNotification: RecordingNotification
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        projectionManager = ProjectionManager(this)
        segmentCleaner = SegmentCleaner()
        replayMerger = ReplayMerger()
        recordingNotification = RecordingNotification(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // We are started via startForegroundService for START, so we must enter the
        // foreground promptly. For SAVE/STOP the service is already foreground.
        if (!isForeground) {
            val msg = if (intent?.action == ACTION_START_BUFFER) "Starting screen capture..."
                      else "Replay Buffer"
            ensureForeground(msg)
        }

        when (intent?.action) {
            ACTION_START_BUFFER -> handleStartBuffer(intent)
            ACTION_SAVE_REPLAY -> handleSaveReplay(intent)
            ACTION_STOP_BUFFER -> handleStopBuffer()
            ACTION_KEEP_ALIVE -> handleKeepAlive()
            else -> {
                // START_STICKY can restart us with a null intent. If the in-memory
                // recording objects still exist, keep the foreground session alive.
                if (isRecordingActive) {
                    handleKeepAlive()
                } else {
                    stopSelfSafely(showIdleNotification = false)
                }
            }
        }
        return START_STICKY
    }

    private fun handleStartBuffer(intent: Intent) {
        try {
            val resultCode = intent.getIntExtra(Constants.EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Constants.EXTRA_PROJECTION_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Constants.EXTRA_PROJECTION_DATA)
            }

            if (resultCode != Activity.RESULT_OK || data == null) {
                Logger.error(Constants.LOG_SERVICE_TAG, "Missing MediaProjection consent data")
                ReplayBufferModule.rejectStart("Screen capture permission not granted")
                stopSelfSafely(showIdleNotification = false)
                return
            }

            bufferMinutes = intent.getIntExtra(Constants.EXTRA_MINUTES, Constants.DEFAULT_BUFFER_MINUTES)

            if (!projectionManager.setup(resultCode, data)) {
                ReplayBufferModule.rejectStart("Failed to initialize screen capture")
                stopSelfSafely(showIdleNotification = false)
                return
            }

            // Re-assert foreground now that projection exists (adds mediaProjection type)
            ensureForeground("Recording last $bufferMinutes minutes...")
            acquireWakeLock()

            projectionManager.onProjectionStopped = {
                Logger.warn(Constants.LOG_SERVICE_TAG, "Projection revoked — stopping")
                handleStopBuffer()
            }

            val manager = SegmentManager(this, projectionManager)
            segmentManager = manager

            isRecordingActive = true
            ReplayBufferModule.emitRecordingState(true)

            manager.startRecording(
                onSegmentComplete = {
                    segmentCleaner.cleanOldSegments(manager.getSegmentDirectory(), bufferMinutes)
                },
                onFatalError = { reason ->
                    Logger.error(Constants.LOG_SERVICE_TAG, "Recording fatal error: $reason")
                    ReplayBufferModule.emitRecordingError(reason)
                    handleStopBuffer()
                }
            )

            ReplayBufferModule.resolveStart()
            Logger.info(Constants.LOG_SERVICE_TAG, "Buffer started ($bufferMinutes min retention)")
        } catch (e: Exception) {
            Logger.error(Constants.LOG_SERVICE_TAG, "Failed to start buffer", e)
            ReplayBufferModule.rejectStart(e.message ?: "Failed to start recording")
            stopSelfSafely(showIdleNotification = false)
        }
    }

    private fun handleSaveReplay(intent: Intent) {
        val manager = segmentManager
        if (manager == null || !isRecordingActive) {
            ReplayBufferModule.rejectSave("Recording is not active")
            return
        }

        try {
            updateNotification("Saving replay...")
            val segmentCount = intent.getIntExtra("segmentCount", Constants.SEGMENTS_FOR_REPLAY)

            manager.finalizeCurrentSegment {
                try {
                    val segmentsDir = manager.getSegmentDirectory()
                    val fromDisk = segmentCleaner.getLastNSegments(segmentsDir, segmentCount)
                    val fromMemory = manager.getCompletedSegments()
                    val segments = (fromMemory + fromDisk)
                        .distinct()
                        .sortedBy { File(it).lastModified() }
                        .takeLast(segmentCount)

                    if (segments.isEmpty()) {
                        ReplayBufferModule.rejectSave(
                            "No segments available yet. Wait at least 30 seconds after starting."
                        )
                        updateNotification("Recording... (no clip ready yet)")
                        return@finalizeCurrentSegment
                    }

                    val replaysDir = getReplaysDirectory()
                    val fileName =
                        "${Constants.REPLAY_FILE_PREFIX}${System.currentTimeMillis()}${Constants.FILE_EXTENSION}"
                    val outputPath = File(replaysDir, fileName).absolutePath

                    replayMerger.mergeSegments(segments, outputPath) { success, result ->
                        if (success && result != null) {
                            val galleryUri = GalleryExporter.saveVideoToGallery(this, File(result), fileName)
                            updateNotification("Replay saved to gallery")
                            ReplayBufferModule.resolveSave(galleryUri ?: result, galleryUri)
                        } else {
                            updateNotification("Recording...")
                            ReplayBufferModule.rejectSave(result ?: "Failed to merge segments")
                        }
                    }
                } catch (e: Exception) {
                    Logger.error(Constants.LOG_SERVICE_TAG, "Save failed", e)
                    ReplayBufferModule.rejectSave(e.message ?: "Failed to save replay")
                }
            }
        } catch (e: Exception) {
            Logger.error(Constants.LOG_SERVICE_TAG, "Failed to save replay", e)
            ReplayBufferModule.rejectSave(e.message ?: "Failed to save replay")
        }
    }

    private fun handleStopBuffer() {
        if (!isRecordingActive && segmentManager == null) {
            stopSelfSafely(showIdleNotification = true)
            return
        }
        isRecordingActive = false
        try {
            projectionManager.onProjectionStopped = null
            segmentManager?.stopRecording()
            projectionManager.cleanup()
        } catch (e: Exception) {
            Logger.error(Constants.LOG_SERVICE_TAG, "Error during stop", e)
        } finally {
            segmentManager = null
            ReplayBufferModule.emitRecordingState(false)
            stopSelfSafely(showIdleNotification = true)
            Logger.info(Constants.LOG_SERVICE_TAG, "Buffer stopped")
        }
    }

    private fun handleKeepAlive() {
        if (!isRecordingActive) {
            Logger.info(Constants.LOG_SERVICE_TAG, "Keep-alive ignored while idle")
            return
        }

        // Do not touch MediaProjection here. This is only to keep the existing
        // foreground service/session strongly visible to Android after task removal.
        ensureForeground("Recording in background...")
        acquireWakeLock()
        Logger.info(Constants.LOG_SERVICE_TAG, "Keep-alive refreshed foreground recording")
    }

    private fun ensureForeground(message: String) {
        val notification = recordingNotification.buildRecordingNotification("Replay Buffer", message)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Must be a mediaProjection-type FGS before createVirtualDisplay (Android 14+).
                // Microphone type is only added when RECORD_AUDIO is granted, otherwise
                // startForeground throws a SecurityException on SDK 34+.
                var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasAudioPermission()) {
                    types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                ServiceCompat.startForeground(this, Constants.NOTIFICATION_ID, notification, types)
            } else {
                @Suppress("DEPRECATION")
                startForeground(Constants.NOTIFICATION_ID, notification)
            }
            isForeground = true
        } catch (e: Exception) {
            Logger.error(Constants.LOG_SERVICE_TAG, "startForeground failed", e)
        }
    }

    private fun updateNotification(message: String) {
        recordingNotification.updateRecordingNotification(message)
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "${packageName}:ReplayBufferWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
            Logger.info(Constants.LOG_SERVICE_TAG, "Recording wake lock acquired")
        } catch (e: Exception) {
            Logger.error(Constants.LOG_SERVICE_TAG, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
            wakeLock = null
            Logger.info(Constants.LOG_SERVICE_TAG, "Recording wake lock released")
        } catch (e: Exception) {
            Logger.error(Constants.LOG_SERVICE_TAG, "Failed to release wake lock", e)
        }
    }

    private fun stopSelfSafely(showIdleNotification: Boolean) {
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isForeground = false
        stopSelf()
        if (showIdleNotification) {
            recordingNotification.showIdleNotification()
        } else {
            recordingNotification.cancelNotification()
        }
    }

    private fun getReplaysDirectory(): File {
        val dir = File(getExternalFilesDir(null), Constants.REPLAYS_DIRECTORY)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (isRecordingActive) {
            // The UI task was removed, but the native foreground recording service
            // must keep running. Re-assert foreground state because some OEMs lower
            // service priority aggressively when the launcher task disappears.
            Logger.info(Constants.LOG_SERVICE_TAG, "Task removed — keeping recording service alive")
            ensureForeground("Recording in background...")
            acquireWakeLock()
            try {
                val keepAliveIntent = Intent(applicationContext, ReplayBufferService::class.java).apply {
                    action = ACTION_KEEP_ALIVE
                    setPackage(packageName)
                }
                startService(keepAliveIntent)
            } catch (e: Exception) {
                Logger.error(Constants.LOG_SERVICE_TAG, "Failed to send keep-alive command", e)
            }
            return
        }
        Logger.info(Constants.LOG_SERVICE_TAG, "Task removed while idle")
        stopSelfSafely(showIdleNotification = false)
    }

    override fun onDestroy() {
        if (isRecordingActive) {
            isRecordingActive = false
            try {
                segmentManager?.stopRecording()
                projectionManager.cleanup()
            } catch (e: Exception) {
                Logger.error(Constants.LOG_SERVICE_TAG, "Error in onDestroy", e)
            }
            segmentManager = null
            ReplayBufferModule.emitRecordingState(false)
        }
        releaseWakeLock()
        super.onDestroy()
    }
}
