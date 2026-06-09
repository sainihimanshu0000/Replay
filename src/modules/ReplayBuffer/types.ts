/**
 * ReplayBuffer Module Types
 */

export interface StartBufferOptions {
  minutes: number;
}

export interface SaveReplayOptions {
  /** Number of 30s segments to include (default: 10 ≈ 5 min) */
  segmentCount?: number;
}

export interface SavedReplay {
  name: string;
  path: string;
  sizeBytes: number;
  modifiedAt: number;
}

export interface ReplayBufferModuleSpec {
  startBuffer(options: StartBufferOptions): Promise<void>;
  saveReplay(options?: SaveReplayOptions | null): Promise<string>;
  stopBuffer(): Promise<void>;
  isRecordingActive(): Promise<boolean>;
  requestBatteryOptimizationExemption(): Promise<boolean>;
  getSavedReplays(): Promise<SavedReplay[]>;
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export interface RecordingStateEvent {
  isRecording: boolean;
}

export interface ReplaySavedEvent {
  path: string;
  galleryUri?: string | null;
}

export interface RecordingErrorEvent {
  message: string;
}

export interface ReplayState {
  isRecording: boolean;
  isReady: boolean;
  error: string | null;
}
