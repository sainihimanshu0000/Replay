import ReplayBuffer from '../modules/ReplayBuffer';
import { ReplayState } from '../modules/ReplayBuffer/types';

class ReplayService {
  private state: ReplayState = {
    isRecording: false,
    isReady: false,
    error: null,
  };

  /**
   * Initialize the replay buffer with specified duration
   */
  async initializeReplayBuffer(minutes: number = 5): Promise<void> {
    try {
      this.state.error = null;
      await ReplayBuffer.startBuffer({ minutes });
      this.state.isRecording = true;
      this.state.isReady = true;
    } catch (error) {
      this.state.error = error instanceof Error ? error.message : 'Unknown error';
      this.state.isRecording = false;
      throw error;
    }
  }

  /**
   * Save the current replay buffer to a file
   */
  async saveCurrentReplay(): Promise<string> {
    try {
      if (!this.state.isRecording) {
        throw new Error('Replay buffer is not recording');
      }
      const filePath = await ReplayBuffer.saveReplay();
      return filePath;
    } catch (error) {
      this.state.error = error instanceof Error ? error.message : 'Unknown error';
      throw error;
    }
  }

  /**
   * Stop the replay buffer and clean up
   */
  async stopReplayBuffer(): Promise<void> {
    try {
      await ReplayBuffer.stopBuffer();
      this.state.isRecording = false;
      this.state.isReady = false;
    } catch (error) {
      this.state.error = error instanceof Error ? error.message : 'Unknown error';
      throw error;
    }
  }

  /**
   * Get current replay state
   */
  getState(): ReplayState {
    return { ...this.state };
  }

  /**
   * Clear error state
   */
  clearError(): void {
    this.state.error = null;
  }
}

export default new ReplayService();
