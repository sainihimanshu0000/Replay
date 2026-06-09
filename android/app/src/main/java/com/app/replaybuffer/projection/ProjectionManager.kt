package com.app.replaybuffer.projection

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import com.app.replaybuffer.utils.Constants
import com.app.replaybuffer.utils.Logger

/**
 * Owns the MediaProjection + a single long-lived VirtualDisplay.
 *
 * The VirtualDisplay is created once per session; segment rotation swaps the
 * output Surface via [setSurface] instead of destroying/recreating the display,
 * which is far more stable and avoids screen flicker.
 */
class ProjectionManager(private val context: Context) {

    private val mediaProjectionManager: MediaProjectionManager? =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    /** Invoked when the system or user revokes the projection. */
    @Volatile
    var onProjectionStopped: (() -> Unit)? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Logger.warn(Constants.LOG_PROJECTION_TAG, "MediaProjection stopped by system/user")
            onProjectionStopped?.invoke()
        }
    }

    fun createScreenCaptureIntent(): Intent? {
        val manager = mediaProjectionManager ?: return null
        // On Android 14+ the picker defaults to "single app" capture, which stops
        // the moment our app is backgrounded. Force full-display capture so the
        // recording keeps running in the background. createConfigForDefaultDisplay()
        // removes the single-app option from the picker entirely.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            manager.createScreenCaptureIntent(
                MediaProjectionConfig.createConfigForDefaultDisplay()
            )
        } else {
            manager.createScreenCaptureIntent()
        }
    }

    fun setup(resultCode: Int, data: Intent): Boolean {
        return try {
            val projection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            if (projection == null) {
                Logger.error(Constants.LOG_PROJECTION_TAG, "Failed to obtain MediaProjection")
                return false
            }
            // Required on Android 14+, harmless elsewhere. Must be registered before
            // createVirtualDisplay().
            projection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))
            mediaProjection = projection
            Logger.info(Constants.LOG_PROJECTION_TAG, "MediaProjection ready")
            true
        } catch (e: Exception) {
            Logger.error(Constants.LOG_PROJECTION_TAG, "Error setting up MediaProjection", e)
            false
        }
    }

    @Synchronized
    fun createVirtualDisplay(
        surface: Surface,
        width: Int,
        height: Int,
        density: Int
    ): VirtualDisplay? {
        val projection = mediaProjection ?: return null
        if (virtualDisplay != null) {
            // Reuse: just point it at the new surface
            setSurface(surface)
            return virtualDisplay
        }
        virtualDisplay = projection.createVirtualDisplay(
            "ReplayBufferDisplay",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null
        )
        return virtualDisplay
    }

    @Synchronized
    fun setSurface(surface: Surface?) {
        try {
            virtualDisplay?.surface = surface
        } catch (e: Exception) {
            Logger.error(Constants.LOG_PROJECTION_TAG, "Error swapping surface", e)
        }
    }

    @Synchronized
    fun releaseVirtualDisplay() {
        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Logger.error(Constants.LOG_PROJECTION_TAG, "Error releasing virtual display", e)
        } finally {
            virtualDisplay = null
        }
    }

    fun getMediaProjection(): MediaProjection? = mediaProjection

    @Synchronized
    fun cleanup() {
        releaseVirtualDisplay()
        try {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
        } catch (e: Exception) {
            Logger.error(Constants.LOG_PROJECTION_TAG, "Error stopping projection", e)
        } finally {
            mediaProjection = null
        }
    }
}
