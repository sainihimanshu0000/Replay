package com.app.replaybuffer.utils

object Constants {

    // Video — 720p @ 3 Mbps / 30 FPS (battery-friendly MVP defaults)
    const val VIDEO_WIDTH = 720
    const val VIDEO_HEIGHT = 1280
    const val VIDEO_BITRATE = 3_000_000
    const val VIDEO_FRAME_RATE = 30
    const val VIDEO_I_FRAME_INTERVAL = 2

    // Segments — 30 s chunks; 10 segments ≈ 5 min buffer
    const val SEGMENT_DURATION_SECONDS = 30
    const val SEGMENT_DURATION_MILLIS = SEGMENT_DURATION_SECONDS * 1000L
    const val SEGMENTS_FOR_REPLAY = 10

    // Buffer
    const val DEFAULT_BUFFER_MINUTES = 5
    const val MAX_BUFFER_MINUTES = 60
    const val MIN_BUFFER_MINUTES = 1

    // Files
    const val REPLAY_FILE_PREFIX = "replay_"
    const val SEGMENT_FILE_PREFIX = "segment_"
    const val FILE_EXTENSION = ".mp4"

    // Notification
    const val NOTIFICATION_ID = 1001
    const val NOTIFICATION_CHANNEL_ID = "ReplayBufferChannel"
    const val NOTIFICATION_CHANNEL_NAME = "Replay Buffer Service"
    const val NOTIFICATION_REQUEST_SAVE = 2001
    const val NOTIFICATION_REQUEST_STOP = 2002
    const val NOTIFICATION_REQUEST_START = 2003
    const val NOTIFICATION_REQUEST_CONTENT = 2004

    const val ACTION_START_FROM_NOTIFICATION = "com.app.replaybuffer.START_FROM_NOTIFICATION"

    // Events (React Native)
    const val EVENT_RECORDING_STATE = "onRecordingStateChange"
    const val EVENT_REPLAY_SAVED = "onReplaySaved"
    const val EVENT_NOTIFICATION_START = "onNotificationStartRequest"
    const val EVENT_RECORDING_ERROR = "onRecordingError"

    // Directories
    const val SEGMENTS_DIRECTORY = "segments"
    const val REPLAYS_DIRECTORY = "Movies"

    // Intent extras
    const val EXTRA_MINUTES = "minutes"
    const val EXTRA_RESULT_CODE = "resultCode"
    const val EXTRA_PROJECTION_DATA = "projectionData"
    const val EXTRA_OUTPUT_PATH = "outputPath"

    // Logging
    const val LOG_TAG = "ReplayBuffer"
    const val LOG_SEGMENT_TAG = "SegmentManager"
    const val LOG_ENCODER_TAG = "VideoEncoder"
    const val LOG_MERGER_TAG = "ReplayMerger"
    const val LOG_SERVICE_TAG = "ReplayBufferService"
    const val LOG_PROJECTION_TAG = "ProjectionManager"

    const val REQUEST_CODE_PROJECTION = 1001
}
