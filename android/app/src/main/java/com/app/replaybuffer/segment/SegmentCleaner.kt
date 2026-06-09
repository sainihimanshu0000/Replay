package com.app.replaybuffer.segment

import com.app.replaybuffer.utils.Constants
import com.app.replaybuffer.utils.Logger
import java.io.File

class SegmentCleaner {

    companion object {
        private const val MIN_VALID_SEGMENT_BYTES = 4096L
    }

    fun cleanOldSegments(segmentsDirectory: File, retentionMinutes: Int): List<String> {
        if (!segmentsDirectory.exists() || !segmentsDirectory.isDirectory) {
            return emptyList()
        }

        val retentionMillis = retentionMinutes * 60 * 1000L
        val currentTime = System.currentTimeMillis()
        val deletedSegments = mutableListOf<String>()

        segmentsDirectory.listFiles()?.forEach { file ->
            if (isCompletedSegment(file)) {
                val ageMillis = currentTime - file.lastModified()

                if (ageMillis > retentionMillis) {
                    if (file.delete()) {
                        deletedSegments.add(file.absolutePath)
                        Logger.debug(
                            Constants.LOG_SEGMENT_TAG,
                            "Deleted old segment: ${file.name} (age: ${ageMillis}ms)"
                        )
                    }
                }
            } else if (file.name.endsWith(".part")) {
                // Remove stale partial files older than one segment duration
                val ageMillis = currentTime - file.lastModified()
                if (ageMillis > Constants.SEGMENT_DURATION_MILLIS * 2) {
                    file.delete()
                }
            }
        }

        return deletedSegments
    }

    fun getSegmentsSorted(segmentsDirectory: File): List<File> {
        if (!segmentsDirectory.exists() || !segmentsDirectory.isDirectory) {
            return emptyList()
        }

        return segmentsDirectory.listFiles()?.filter { file ->
            isCompletedSegment(file) && file.length() >= MIN_VALID_SEGMENT_BYTES
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getLastNSegments(segmentsDirectory: File, count: Int): List<String> {
        return getSegmentsSorted(segmentsDirectory)
            .take(count)
            .map { it.absolutePath }
            .reversed()
    }

    fun getTotalSegmentsSize(segmentsDirectory: File): Long {
        return getSegmentsSorted(segmentsDirectory).sumOf { it.length() }
    }

    fun clearAllSegments(segmentsDirectory: File): Int {
        if (!segmentsDirectory.exists() || !segmentsDirectory.isDirectory) {
            return 0
        }

        var deletedCount = 0
        segmentsDirectory.listFiles()?.forEach { file ->
            if (file.name.startsWith(Constants.SEGMENT_FILE_PREFIX)) {
                if (file.delete()) deletedCount++
            }
        }

        Logger.info(Constants.LOG_SEGMENT_TAG, "Cleared $deletedCount segments")
        return deletedCount
    }

    private fun isCompletedSegment(file: File): Boolean {
        return file.isFile &&
            file.name.startsWith(Constants.SEGMENT_FILE_PREFIX) &&
            file.name.endsWith(Constants.FILE_EXTENSION) &&
            !file.name.endsWith(".part")
    }
}
