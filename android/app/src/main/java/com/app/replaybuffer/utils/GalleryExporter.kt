package com.app.replaybuffer.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object GalleryExporter {

    private const val GALLERY_FOLDER = "Replay"

    /**
     * Copies a video file into the public Movies/Replay gallery folder via MediaStore.
     * @return Content URI string (gallery location), or null on failure
     */
    fun saveVideoToGallery(context: Context, sourceFile: File, displayName: String): String? {
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
            Logger.error(Constants.LOG_TAG, "Source file missing or empty: ${sourceFile.absolutePath}")
            return null
        }

        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/$GALLERY_FOLDER"
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values)
            if (uri == null) {
                Logger.error(Constants.LOG_TAG, "MediaStore insert returned null")
                return null
            }

            resolver.openOutputStream(uri)?.use { output ->
                sourceFile.inputStream().use { input -> input.copyTo(output) }
            } ?: run {
                resolver.delete(uri, null, null)
                Logger.error(Constants.LOG_TAG, "Failed to open MediaStore output stream")
                return null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publishValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(uri, publishValues, null, null)
            }

            Logger.info(Constants.LOG_TAG, "Saved to gallery: $uri")
            uri.toString()
        } catch (e: Exception) {
            Logger.error(Constants.LOG_TAG, "Failed to save video to gallery", e)
            null
        }
    }
}
