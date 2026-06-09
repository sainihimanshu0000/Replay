package com.app.replaybuffer.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.app.replaybuffer.service.ReplayBufferService
import com.app.replaybuffer.utils.Constants
import com.replay.MainActivity

class RecordingNotification(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Replay buffer recording controls"
                enableVibration(false)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildRecordingNotification(
        title: String = "Replay Buffer",
        message: String = "Recording..."
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            Constants.NOTIFICATION_REQUEST_CONTENT,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            pendingIntentFlags()
        )

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_save,
                "Save Replay",
                servicePendingIntent(ReplayBufferService.ACTION_SAVE_REPLAY, Constants.NOTIFICATION_REQUEST_SAVE)
            )
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                servicePendingIntent(ReplayBufferService.ACTION_STOP_BUFFER, Constants.NOTIFICATION_REQUEST_STOP)
            )
            .build()
    }

    fun buildIdleNotification(): Notification {
        val startIntent = Intent(context, MainActivity::class.java).apply {
            action = Constants.ACTION_START_FROM_NOTIFICATION
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Replay Buffer")
            .setContentText("Tap Start to begin screen recording")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(PendingIntent.getActivity(context, Constants.NOTIFICATION_REQUEST_START, startIntent, pendingIntentFlags()))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_media_play,
                "Start Recording",
                PendingIntent.getActivity(context, Constants.NOTIFICATION_REQUEST_START, startIntent, pendingIntentFlags())
            )
            .build()
    }

    fun updateRecordingNotification(message: String) {
        notificationManager.notify(
            Constants.NOTIFICATION_ID,
            buildRecordingNotification(message = message)
        )
    }

    fun showIdleNotification() {
        notificationManager.notify(Constants.NOTIFICATION_ID, buildIdleNotification())
    }

    fun cancelNotification() {
        notificationManager.cancel(Constants.NOTIFICATION_ID)
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReplayBufferService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(context, requestCode, intent, pendingIntentFlags())
    }

    private fun pendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
}
