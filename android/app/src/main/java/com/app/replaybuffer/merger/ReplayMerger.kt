package com.app.replaybuffer.merger

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.app.replaybuffer.utils.Constants
import com.app.replaybuffer.utils.Logger
import java.io.File
import java.nio.ByteBuffer

class ReplayMerger {

    companion object {
        private const val BUFFER_SIZE = 256 * 1024
        private const val MIN_VALID_SEGMENT_BYTES = 4096L
    }

    private data class TrackKey(val mime: String)

    fun mergeSegments(
        segments: List<String>,
        outputPath: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val validSegments = segments.filter { path ->
            val file = File(path)
            file.exists() && file.length() >= MIN_VALID_SEGMENT_BYTES
        }

        if (validSegments.isEmpty()) {
            Logger.warn(Constants.LOG_MERGER_TAG, "No valid segments to merge")
            callback(false, "No valid segments to merge. Wait at least 30 seconds after starting.")
            return
        }

        Thread {
            var muxer: MediaMuxer? = null
            try {
                Logger.info(
                    Constants.LOG_MERGER_TAG,
                    "Merging ${validSegments.size} segments → $outputPath"
                )

                muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val trackMap = mutableMapOf<TrackKey, Int>()
                var muxerStarted = false
                var timeOffsetUs = 0L
                var mergedCount = 0

                for (segmentPath in validSegments) {
                    val extractor = MediaExtractor()
                    try {
                        extractor.setDataSource(segmentPath)
                        if (extractor.trackCount == 0) {
                            Logger.warn(Constants.LOG_MERGER_TAG, "No tracks in segment: $segmentPath")
                            continue
                        }

                        val segmentTrackToMuxer = mutableMapOf<Int, Int>()
                        var segmentDurationUs = 0L

                        for (i in 0 until extractor.trackCount) {
                            val format = extractor.getTrackFormat(i)
                            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                            val key = TrackKey(mime)

                            val muxerTrackId = trackMap.getOrPut(key) {
                                muxer.addTrack(format)
                            }
                            segmentTrackToMuxer[i] = muxerTrackId
                            extractor.selectTrack(i)
                        }

                        if (segmentTrackToMuxer.isEmpty()) {
                            Logger.warn(Constants.LOG_MERGER_TAG, "No mappable tracks: $segmentPath")
                            continue
                        }

                        if (!muxerStarted) {
                            muxer.start()
                            muxerStarted = true
                        }

                        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
                        val bufferInfo = MediaCodec.BufferInfo()

                        while (true) {
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) break

                            val trackIndex = extractor.sampleTrackIndex
                            val mappedTrack = segmentTrackToMuxer[trackIndex]
                            if (mappedTrack != null) {
                                bufferInfo.offset = 0
                                bufferInfo.size = sampleSize
                                bufferInfo.presentationTimeUs = extractor.sampleTime + timeOffsetUs
                                bufferInfo.flags = extractor.sampleFlags
                                muxer.writeSampleData(mappedTrack, buffer, bufferInfo)
                                segmentDurationUs = maxOf(segmentDurationUs, extractor.sampleTime)
                            }
                            extractor.advance()
                        }

                        timeOffsetUs += segmentDurationUs + 33_000
                        mergedCount++
                        Logger.debug(Constants.LOG_MERGER_TAG, "Merged segment: $segmentPath")
                    } catch (e: Exception) {
                        Logger.warn(
                            Constants.LOG_MERGER_TAG,
                            "Skipping unreadable segment $segmentPath: ${e.message}"
                        )
                    } finally {
                        extractor.release()
                    }
                }

                if (!muxerStarted || mergedCount == 0) {
                    callback(
                        false,
                        "No readable segments found. Wait for at least one 30-second segment to finish."
                    )
                    return@Thread
                }

                muxer.stop()
                muxer.release()
                muxer = null

                val fileSizeKB = File(outputPath).length() / 1024
                Logger.info(
                    Constants.LOG_MERGER_TAG,
                    "Merge successful: $outputPath (${fileSizeKB}KB, $mergedCount segments)"
                )
                callback(true, outputPath)
            } catch (e: Exception) {
                Logger.error(Constants.LOG_MERGER_TAG, "Error merging segments", e)
                File(outputPath).delete()
                callback(false, e.message ?: "Merge failed")
            } finally {
                try {
                    muxer?.release()
                } catch (_: Exception) {
                }
            }
        }.start()
    }
}
