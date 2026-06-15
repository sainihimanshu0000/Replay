import { useState, useCallback, useEffect } from 'react';
import ReplayBuffer, { replayBufferEvents } from '../modules/ReplayBuffer';
import type {
  ReplaySavedEvent,
  RecordingStateEvent,
  RecordingErrorEvent,
  SavedReplay,
} from '../modules/ReplayBuffer/types';

const DEFAULT_BUFFER_MINUTES = 5;
const SEGMENTS_FOR_REPLAY = 10;

const formatDuration = (seconds: number) => {
  const minutes = Math.floor(seconds / 60)
    .toString()
    .padStart(2, '0');
  const remainder = Math.floor(seconds % 60)
    .toString()
    .padStart(2, '0');
  return `${minutes}:${remainder}`;
};

export const useReplayBuffer = () => {
  const [isRecording, setIsRecording] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [lastGalleryUri, setLastGalleryUri] = useState<string | null>(null);
  const [savedReplays, setSavedReplays] = useState<SavedReplay[]>([]);
  const [recordingStartedAt, setRecordingStartedAt] = useState<number | null>(null);
  const [elapsedTime, setElapsedTime] = useState('00:00');

  const refreshSavedReplays = useCallback(async () => {
    try {
      const recordings = await ReplayBuffer.getSavedReplays();
      setSavedReplays(recordings);
    } catch {
      // ignore errors from listing saved files
    }
  }, []);

  const start = useCallback(async (minutes: number = DEFAULT_BUFFER_MINUTES) => {
    try {
      setIsLoading(true);
      setError(null);
      await ReplayBuffer.startBuffer({ minutes });
      setIsRecording(true);
      setRecordingStartedAt(Date.now());
      await refreshSavedReplays();
    } catch (err) {
      const errorMsg =
        (err as { message?: string })?.message ?? 'Failed to start buffer';
      setError(errorMsg);
      setIsRecording(false);
      setRecordingStartedAt(null);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, [refreshSavedReplays]);

  const save = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const filePath = await ReplayBuffer.saveReplay({ segmentCount: SEGMENTS_FOR_REPLAY });
      await refreshSavedReplays();
      return filePath;
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to save replay';
      setError(errorMsg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, [refreshSavedReplays]);

  const stop = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      await ReplayBuffer.stopBuffer();
      setIsRecording(false);
      setRecordingStartedAt(null);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to stop buffer';
      setError(errorMsg);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const requestBackgroundPermission = useCallback(async () => {
    try {
      setError(null);
      await ReplayBuffer.requestBatteryOptimizationExemption();
    } catch (err) {
      const errorMsg =
        err instanceof Error
          ? err.message
          : 'Failed to open battery optimization settings';
      setError(errorMsg);
      throw err;
    }
  }, []);

  useEffect(() => {
    let timer: number | null = null;

    if (isRecording && recordingStartedAt) {
      setElapsedTime(formatDuration((Date.now() - recordingStartedAt) / 1000));
      timer = setInterval(() => {
        setElapsedTime(formatDuration((Date.now() - recordingStartedAt) / 1000));
      }, 1000) as unknown as number;
    } else {
      setElapsedTime('00:00');
    }

    return () => {
      if (timer !== null) {
        clearInterval(timer);
      }
    };
  }, [isRecording, recordingStartedAt]);

  useEffect(() => {
    refreshSavedReplays();
    ReplayBuffer.isRecordingActive()
      .then((active) => {
        setIsRecording(active);
        if (active && recordingStartedAt === null) {
          setRecordingStartedAt(Date.now());
        }
      })
      .catch(() => {});

    if (!replayBufferEvents) return;

    const stateSub = replayBufferEvents.addListener(
      'onRecordingStateChange',
      (event: RecordingStateEvent) => {
        setIsRecording(event.isRecording);
        if (event.isRecording) {
          setRecordingStartedAt((prev) => prev ?? Date.now());
        } else {
          setRecordingStartedAt(null);
        }
      }
    );

    const savedSub = replayBufferEvents.addListener(
      'onReplaySaved',
      (event: ReplaySavedEvent) => {
        if (event.galleryUri) {
          setLastGalleryUri(event.galleryUri);
        }
        refreshSavedReplays();
      }
    );

    const startSub = replayBufferEvents.addListener(
      'onNotificationStartRequest',
      () => {
        start(DEFAULT_BUFFER_MINUTES).catch(() => {});
      }
    );

    const errorSub = replayBufferEvents.addListener(
      'onRecordingError',
      (event: RecordingErrorEvent) => {
        setError(event.message);
        setIsRecording(false);
        setRecordingStartedAt(null);
      }
    );

    return () => {
      stateSub.remove();
      savedSub.remove();
      startSub.remove();
      errorSub.remove();
    };
  }, [refreshSavedReplays, start, recordingStartedAt]);

  return {
    start,
    save,
    stop,
    requestBackgroundPermission,
    isRecording,
    isLoading,
    error,
    lastGalleryUri,
    savedReplays,
    elapsedTime,
    recordingsCount: savedReplays.length,
    refreshSavedReplays,
  };
};
