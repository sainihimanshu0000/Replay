import { NativeModules, NativeEventEmitter, Platform } from 'react-native';
import type {
  SavedReplay,
  StartBufferOptions,
  SaveReplayOptions,
  ReplayBufferModuleSpec,
} from './types';

const NativeReplayBuffer: ReplayBufferModuleSpec = NativeModules.ReplayBuffer;

if (!NativeReplayBuffer) {
  throw new Error(
    'ReplayBuffer native module not found. Make sure it is properly registered in MainApplication.kt'
  );
}

export const replayBufferEvents =
  Platform.OS === 'android' ? new NativeEventEmitter(NativeReplayBuffer) : null;

class ReplayBuffer {
  async startBuffer(options: StartBufferOptions): Promise<void> {
    if (!options.minutes || options.minutes < 1) {
      throw new Error('Minutes must be greater than 0');
    }
    return NativeReplayBuffer.startBuffer(options);
  }

  async saveReplay(options?: SaveReplayOptions): Promise<string> {
    const filePath = await NativeReplayBuffer.saveReplay(options ?? null);
    if (!filePath) {
      throw new Error('Failed to save replay');
    }
    return filePath;
  }

  async stopBuffer(): Promise<void> {
    return NativeReplayBuffer.stopBuffer();
  }

  async isRecordingActive(): Promise<boolean> {
    return NativeReplayBuffer.isRecordingActive();
  }

  async requestBatteryOptimizationExemption(): Promise<boolean> {
    return NativeReplayBuffer.requestBatteryOptimizationExemption();
  }

  async getSavedReplays(): Promise<SavedReplay[]> {
    return NativeReplayBuffer.getSavedReplays();
  }
}

export default new ReplayBuffer();
