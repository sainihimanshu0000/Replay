package com.app.replaybuffer.encoder

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.app.replaybuffer.utils.Constants
import com.app.replaybuffer.utils.Logger
import java.io.File

class VideoEncoder(private val context: Context) {

    companion object {
        const val MIME_TYPE = "video/avc"
    }

    private var mediaCodec: MediaCodec? = null

    /**
     * Initialize video encoder with optimized 720p settings
     * @param callback Callback for encoder initialization
     */
    fun initialize(callback: (Boolean) -> Unit) {
        try {
            val format = MediaFormat.createVideoFormat(
                MIME_TYPE,
                Constants.VIDEO_WIDTH,
                Constants.VIDEO_HEIGHT
            )
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            format.setInteger(MediaFormat.KEY_BIT_RATE, Constants.VIDEO_BITRATE)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, Constants.VIDEO_FRAME_RATE)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constants.VIDEO_I_FRAME_INTERVAL)

            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec?.start()

            Logger.info(
                Constants.LOG_ENCODER_TAG,
                "Encoder initialized: ${Constants.VIDEO_WIDTH}x${Constants.VIDEO_HEIGHT} @ ${Constants.VIDEO_FRAME_RATE}fps, ${Constants.VIDEO_BITRATE / 1_000_000}Mbps"
            )
            callback(true)
        } catch (e: Exception) {
            Logger.error(Constants.LOG_ENCODER_TAG, "Error initializing encoder", e)
            callback(false)
        }
    }

    /**
     * Get encoding surface for media projection
     */
    fun getEncodingSurface(): android.view.Surface? {
        return mediaCodec?.createInputSurface()
    }

    /**
     * Stop encoding
     */
    fun stop() {
        try {
            mediaCodec?.signalEndOfInputStream()
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
            Logger.info(Constants.LOG_ENCODER_TAG, "Encoder stopped")
        } catch (e: Exception) {
            Logger.error(Constants.LOG_ENCODER_TAG, "Error stopping encoder", e)
        }
    }

    /**
     * Encode frame
     */
    fun encodeFrame(presentationTimeUs: Long): ByteArray? {
        return try {
            mediaCodec?.let { codec ->
                val bufferIndex = codec.dequeueOutputBuffer(
                    MediaCodec.BufferInfo(),
                    0
                )
                if (bufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(bufferIndex)
                    val data = ByteArray(outputBuffer?.remaining() ?: 0)
                    outputBuffer?.get(data)
                    codec.releaseOutputBuffer(bufferIndex, false)
                    data
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Logger.error(Constants.LOG_ENCODER_TAG, "Error encoding frame", e)
            null
        }
    }
}
