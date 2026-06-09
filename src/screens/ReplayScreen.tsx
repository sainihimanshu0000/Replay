import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  Text,
  Alert,
  Share,
} from 'react-native';
import { ReplayButton } from '../components/ReplayButton';
import { RecordingIndicator } from '../components/RecordingIndicator';
import type { SavedReplay } from '../modules/ReplayBuffer/types';

const BUFFER_DURATION = 5; // minutes

interface ReplayScreenProps {
  start: (minutes?: number) => Promise<void>;
  save: () => Promise<string>;
  stop: () => Promise<void>;
  requestBackgroundPermission: () => Promise<void>;
  isRecording: boolean;
  isLoading: boolean;
  error: string | null;
  lastGalleryUri: string | null;
  savedReplays: SavedReplay[];
  elapsedTime: string;
  recordingsCount: number;
}

export const ReplayScreen: React.FC<ReplayScreenProps> = ({
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
  recordingsCount,
}) => {
  const [lastReplayPath, setLastReplayPath] = useState<string | null>(null);

  const handleStart = async () => {
    try {
      await start(BUFFER_DURATION);
      Alert.alert('Success', `Recording buffer started (${BUFFER_DURATION} minutes)`);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Failed to start recording';
      Alert.alert('Error', message);
    }
  };

  const handleBackgroundPermission = async () => {
    try {
      await requestBackgroundPermission();
      Alert.alert(
        'Background Recording',
        'If Android shows a battery prompt, choose Allow. On Samsung, also set this app to Unrestricted in App info > Battery.'
      );
    } catch (err) {
      const message =
        err instanceof Error
          ? err.message
          : 'Failed to open background recording settings';
      Alert.alert('Error', message);
    }
  };

  const handleSave = async () => {
    try {
      const filePath = await save();
      setLastReplayPath(filePath);
      Alert.alert(
        'Success',
        'Replay saved successfully.\n\n' + filePath
      );
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Failed to save replay';
      Alert.alert('Error', message);
    }
  };

  const handleStop = async () => {
    try {
      await stop();
      Alert.alert('Success', 'Recording stopped');
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Failed to stop recording';
      Alert.alert('Error', message);
    }
  };

  const handleShare = async () => {
    if (!lastReplayPath) {
      Alert.alert('Info', 'No replay file to share');
      return;
    }

    const shareUrl =
      lastReplayPath.startsWith('content://') ||
      lastReplayPath.startsWith('file://')
        ? lastReplayPath
        : `file://${lastReplayPath}`;

    try {
      await Share.share({
        url: shareUrl,
        message: 'Check out my replay!',
        title: 'Share Replay',
      });
    } catch (err) {
      Alert.alert('Error', 'Failed to share replay');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.title}>Replay Buffer</Text>
          <RecordingIndicator
            isRecording={isRecording}
            isLoading={isLoading}
          />
        </View>

        {/* Status Section */}
        <View style={styles.statusSection}>
          <View style={styles.statusItem}>
            <Text style={styles.statusLabel}>Status:</Text>
            <Text style={styles.statusValue}>
              {isRecording ? 'Recording' : 'Idle'}
            </Text>
          </View>
          <View style={styles.statusItem}>
            <Text style={styles.statusLabel}>Elapsed:</Text>
            <Text style={styles.statusValue}>{elapsedTime}</Text>
          </View>
          <View style={styles.statusItem}>
            <Text style={styles.statusLabel}>Saved Recordings:</Text>
            <Text style={styles.statusValue}>{recordingsCount}</Text>
          </View>
          {lastReplayPath && (
            <View style={styles.statusItem}>
              <Text style={styles.statusLabel}>Last Replay:</Text>
              <Text style={styles.statusValue} numberOfLines={1}>
                {lastReplayPath.split('/').pop()}
              </Text>
            </View>
          )}
          {lastGalleryUri && (
            <View style={styles.statusItem}>
              <Text style={styles.statusLabel}>Gallery:</Text>
              <Text style={styles.statusValue} numberOfLines={1}>
                Saved to Movies/Replay
              </Text>
            </View>
          )}
        </View>

        {/* Error Display */}
        {error && (
          <View style={styles.errorBox}>
            <Text style={styles.errorText}>{error}</Text>
          </View>
        )}

        {/* Control Buttons */}
        <View style={styles.buttonGroup}>
          <ReplayButton
            title={isRecording ? 'Recording...' : 'Start Recording'}
            onPress={handleStart}
            disabled={isRecording}
            isLoading={isLoading && !isRecording}
            variant="primary"
          />

          <ReplayButton
            title="Allow Background Recording"
            onPress={handleBackgroundPermission}
            disabled={isRecording}
            variant="secondary"
          />

          <ReplayButton
            title="Save Last 5 Minutes"
            onPress={handleSave}
            disabled={!isRecording}
            isLoading={isLoading && isRecording}
            variant="secondary"
          />

          <ReplayButton
            title="Stop Recording"
            onPress={handleStop}
            disabled={!isRecording}
            variant="danger"
          />

          {lastReplayPath && (
            <ReplayButton
              title="Share Replay"
              onPress={handleShare}
              variant="secondary"
            />
          )}
        </View>

        {/* Instructions */}
        <View style={styles.instructionsBox}>
          <Text style={styles.instructionsTitle}>How it works:</Text>
          <Text style={styles.instructionItem}>
            1. Tap "Allow Background Recording" once to disable battery limits
          </Text>
          <Text style={styles.instructionItem}>
            2. Tap "Start Recording" and approve entire-screen capture
          </Text>
          <Text style={styles.instructionItem}>
            3. A rolling {BUFFER_DURATION}-minute buffer continues in the background
          </Text>
          <Text style={styles.instructionItem}>
            4. Use notification actions to Save or Stop anytime
          </Text>
          <Text style={styles.instructionItem}>
            5. Tap "Save Last 5 Minutes" to export to Gallery (Movies/Replay)
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  content: {
    paddingHorizontal: 16,
    paddingVertical: 20,
  },
  header: {
    marginBottom: 24,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    color: '#000',
  },
  statusSection: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  statusItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 8,
  },
  statusLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#666',
  },
  statusValue: {
    fontSize: 14,
    fontWeight: '500',
    color: '#007AFF',
  },
  errorBox: {
    backgroundColor: '#FFE5E5',
    borderLeftWidth: 4,
    borderLeftColor: '#FF3B30',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
  },
  errorText: {
    color: '#C41E3A',
    fontSize: 14,
    fontWeight: '500',
  },
  buttonGroup: {
    gap: 12,
    marginBottom: 24,
  },
  instructionsBox: {
    backgroundColor: '#F0F8FF',
    borderRadius: 12,
    padding: 16,
    borderLeftWidth: 4,
    borderLeftColor: '#007AFF',
  },
  instructionsTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#000',
    marginBottom: 12,
  },
  instructionItem: {
    fontSize: 14,
    color: '#333',
    marginBottom: 8,
    lineHeight: 20,
  },
});
