package com.app.replaybuffer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.app.replaybuffer.projection.ProjectionManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.app.replaybuffer.service.ReplayBufferService
import com.app.replaybuffer.utils.Constants
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import com.replay.MainActivity
import java.io.File

class ReplayBufferModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), ActivityEventListener {

    private var startPromise: Promise? = null
    private var pendingMinutes: Int = Constants.DEFAULT_BUFFER_MINUTES
    private var permissionsPromise: Promise? = null
    private var pendingStartOptions: ReadableMap? = null

    private val permissionListener = PermissionListener { requestCode, _, grantResults ->
        if (requestCode != REQUEST_RUNTIME_PERMISSIONS) {
            return@PermissionListener false
        }

        val promise = permissionsPromise
        val options = pendingStartOptions
        permissionsPromise = null
        pendingStartOptions = null

        val denied = grantResults.any { it != PackageManager.PERMISSION_GRANTED }
        if (denied) {
            promise?.reject(
                "PERMISSION_DENIED",
                "Microphone and notification permissions are required for recording"
            )
            return@PermissionListener true
        }

        if (promise != null && options != null) {
            launchScreenCapture(options, promise)
        }
        true
    }

    init {
        reactContext.addActivityEventListener(this)
        moduleInstance = this
    }

    override fun getName(): String = "ReplayBuffer"

    @ReactMethod
    fun startBuffer(options: ReadableMap, promise: Promise) {
        reactContext.runOnUiQueueThread {
            try {
                val missingPermissions = getMissingPermissions()
                if (missingPermissions.isNotEmpty()) {
                    val activity = MainActivity.getInstance() ?: reactContext.currentActivity
                    if (activity is PermissionAwareActivity) {
                        permissionsPromise = promise
                        pendingStartOptions = options
                        activity.requestPermissions(
                            missingPermissions.toTypedArray(),
                            REQUEST_RUNTIME_PERMISSIONS,
                            permissionListener
                        )
                        return@runOnUiQueueThread
                    }
                }

                launchScreenCapture(options, promise)
            } catch (e: Exception) {
                promise.reject("START_BUFFER_ERROR", e.message ?: "Failed to start buffer", e)
            }
        }
    }

    private fun launchScreenCapture(options: ReadableMap, promise: Promise) {
        try {
            val minutes = if (options.hasKey("minutes")) {
                options.getInt("minutes")
            } else {
                Constants.DEFAULT_BUFFER_MINUTES
            }

            if (minutes < Constants.MIN_BUFFER_MINUTES || minutes > Constants.MAX_BUFFER_MINUTES) {
                promise.reject(
                    "INVALID_ARGS",
                    "Minutes must be between ${Constants.MIN_BUFFER_MINUTES} and ${Constants.MAX_BUFFER_MINUTES}"
                )
                return
            }

            val captureIntent = ProjectionManager(reactContext).createScreenCaptureIntent()
            if (captureIntent == null) {
                promise.reject(
                    "PROJECTION_UNAVAILABLE",
                    "Screen capture is not available on this device"
                )
                return
            }

            val mainActivity = MainActivity.getInstance() ?: reactContext.currentActivity as? MainActivity
            val fallbackActivity = reactContext.currentActivity

            when {
                mainActivity != null -> {
                    startPromise = promise
                    pendingMinutes = minutes
                    mainActivity.requestScreenCapture(captureIntent) { resultCode, data ->
                        reactContext.runOnUiQueueThread {
                            handleProjectionResult(resultCode, data)
                        }
                    }
                }

                fallbackActivity != null -> {
                    startPromise = promise
                    pendingMinutes = minutes
                    @Suppress("DEPRECATION")
                    fallbackActivity.startActivityForResult(
                        captureIntent,
                        Constants.REQUEST_CODE_PROJECTION
                    )
                }

                else -> {
                    promise.reject(
                        "NO_ACTIVITY",
                        "Activity not available. Keep the app open in the foreground and try again."
                    )
                }
            }
        } catch (e: Exception) {
            promise.reject("START_BUFFER_ERROR", e.message ?: "Failed to start buffer", e)
        }
    }

    private fun getMissingPermissions(): List<String> {
        val required = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return required.filter {
            ContextCompat.checkSelfPermission(reactContext, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    @ReactMethod
    fun saveReplay(options: ReadableMap?, promise: Promise) {
        try {
            if (pendingSavePromise != null) {
                promise.reject("SAVE_IN_PROGRESS", "A save operation is already in progress")
                return
            }

            pendingSavePromise = promise

            val segmentCount = if (options != null && options.hasKey("segmentCount")) {
                options.getInt("segmentCount")
            } else {
                Constants.SEGMENTS_FOR_REPLAY
            }

            if (!ReplayBufferService.isRecordingActive) {
                pendingSavePromise = null
                promise.reject("NOT_RECORDING", "Recording is not active")
                return
            }

            val intent = Intent(reactContext, ReplayBufferService::class.java).apply {
                action = ReplayBufferService.ACTION_SAVE_REPLAY
                putExtra("segmentCount", segmentCount)
            }
            // Service is already running in the foreground; deliver as a plain command
            // (startForegroundService here would impose a startForeground() obligation).
            reactContext.startService(intent)
        } catch (e: Exception) {
            pendingSavePromise = null
            promise.reject("SAVE_REPLAY_ERROR", e.message ?: "Failed to save replay", e)
        }
    }

    @ReactMethod
    fun getSavedReplays(promise: Promise) {
        try {
            val directory = getReplaysDirectory()
            val records = directory.listFiles()
                ?.filter { it.isFile && it.name.endsWith(Constants.FILE_EXTENSION) }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()

            val result = Arguments.createArray()
            records.forEach { file ->
                val item = Arguments.createMap().apply {
                    putString("name", file.name)
                    putString("path", file.absolutePath)
                    putDouble("sizeBytes", file.length().toDouble())
                    putDouble("modifiedAt", file.lastModified().toDouble())
                }
                result.pushMap(item)
            }
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("LIST_REPLAYS_ERROR", e.message ?: "Failed to list saved replays", e)
        }
    }

    @ReactMethod
    fun stopBuffer(promise: Promise) {
        try {
            val intent = Intent(reactContext, ReplayBufferService::class.java).apply {
                action = ReplayBufferService.ACTION_STOP_BUFFER
            }
            // Deliver as a plain command to the already-running service. Using
            // startForegroundService for a stop would require the service to call
            // startForeground() (which it won't), crashing the app.
            reactContext.startService(intent)
            promise.resolve(null)
        } catch (e: Exception) {
            // Service isn't running — nothing to stop. Treat as already stopped.
            ReplayBufferService.isRecordingActive = false
            emitRecordingState(false)
            promise.resolve(null)
        }
    }

    @ReactMethod
    fun isRecordingActive(promise: Promise) {
        promise.resolve(ReplayBufferService.isRecordingActive)
    }

    @ReactMethod
    fun requestBatteryOptimizationExemption(promise: Promise) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                promise.resolve(true)
                return
            }

            val powerManager =
                reactContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (powerManager.isIgnoringBatteryOptimizations(reactContext.packageName)) {
                promise.resolve(true)
                return
            }

            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${reactContext.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val activity = MainActivity.getInstance() ?: reactContext.currentActivity
            if (activity != null) {
                activity.startActivity(intent)
            } else {
                reactContext.startActivity(intent)
            }
            promise.resolve(false)
        } catch (e: Exception) {
            promise.reject(
                "BATTERY_OPTIMIZATION_ERROR",
                e.message ?: "Failed to open battery optimization settings",
                e
            )
        }
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Required for NativeEventEmitter
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for NativeEventEmitter
    }

    private fun sendEvent(eventName: String, params: WritableMap?) {
        if (reactContext.hasActiveReactInstance()) {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
        }
    }

    override fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode != Constants.REQUEST_CODE_PROJECTION) return
        handleProjectionResult(resultCode, data)
    }

    override fun onNewIntent(intent: Intent) {
        // No-op
    }

    private fun handleProjectionResult(resultCode: Int, data: Intent?) {
        val promise = startPromise
        startPromise = null

        if (resultCode != Activity.RESULT_OK || data == null) {
            promise?.reject("PERMISSION_DENIED", "Screen capture permission denied")
            return
        }

        try {
            val intent = Intent(reactContext, ReplayBufferService::class.java).apply {
                action = ReplayBufferService.ACTION_START_BUFFER
                putExtra(Constants.EXTRA_MINUTES, pendingMinutes)
                putExtra(Constants.EXTRA_RESULT_CODE, resultCode)
                putExtra(Constants.EXTRA_PROJECTION_DATA, data)
            }
            startServiceCompat(intent)
            pendingStartPromise = promise
        } catch (e: Exception) {
            promise?.reject("START_BUFFER_ERROR", e.message ?: "Failed to start buffer", e)
        }
    }

    private fun startServiceCompat(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            reactContext.startForegroundService(intent)
        } else {
            reactContext.startService(intent)
        }
    }

    private fun getReplaysDirectory(): File {
        val baseDir = reactContext.getExternalFilesDir(null) ?: reactContext.filesDir
        val dir = File(baseDir, Constants.REPLAYS_DIRECTORY)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    companion object {
        private const val REQUEST_RUNTIME_PERMISSIONS = 1002

        @Volatile
        private var moduleInstance: ReplayBufferModule? = null

        @Volatile
        var pendingStartPromise: Promise? = null

        @Volatile
        var pendingSavePromise: Promise? = null

        fun resolveStart() {
            pendingStartPromise?.resolve(null)
            pendingStartPromise = null
        }

        fun rejectStart(message: String) {
            pendingStartPromise?.reject("START_BUFFER_ERROR", message)
            pendingStartPromise = null
        }

        fun resolveSave(path: String, galleryUri: String? = null) {
            pendingSavePromise?.resolve(path)
            pendingSavePromise = null
            emitReplaySaved(path, galleryUri)
        }

        fun rejectSave(message: String) {
            pendingSavePromise?.reject("SAVE_REPLAY_ERROR", message)
            pendingSavePromise = null
        }

        fun emitRecordingState(isRecording: Boolean) {
            moduleInstance?.sendEvent(
                Constants.EVENT_RECORDING_STATE,
                Arguments.createMap().apply { putBoolean("isRecording", isRecording) }
            )
        }

        fun emitReplaySaved(path: String, galleryUri: String?) {
            moduleInstance?.sendEvent(
                Constants.EVENT_REPLAY_SAVED,
                Arguments.createMap().apply {
                    putString("path", path)
                    putString("galleryUri", galleryUri)
                }
            )
        }

        fun emitNotificationStartRequest() {
            moduleInstance?.sendEvent(Constants.EVENT_NOTIFICATION_START, null)
        }

        fun emitRecordingError(message: String) {
            moduleInstance?.sendEvent(
                Constants.EVENT_RECORDING_ERROR,
                Arguments.createMap().apply { putString("message", message) }
            )
        }
    }
}
