import React, { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  ScrollView,
  Share,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import type { SavedReplay } from '../modules/ReplayBuffer/types';

const DEFAULT_BUFFER_MINUTES = 5;
const BUFFER_OPTIONS = [1, 2, 3, 5, 10, 15, 20];

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
  isRecording,
  isLoading,
  error,
  lastGalleryUri,
  elapsedTime,
  recordingsCount,
}) => {
  const [lastReplayPath, setLastReplayPath] = useState<string | null>(null);
  const [bufferMinutes, setBufferMinutes] = useState(DEFAULT_BUFFER_MINUTES);
  const [audioProfile, setAudioProfile] = useState<
    'Mute' | 'System' | 'Mic' | 'Hybrid'
  >('Mute');
  const [governor, setGovernor] = useState<'Gamer' | 'Balanced' | 'Saver'>(
    'Balanced'
  );

  const fps = governor === 'Gamer' ? 60 : governor === 'Saver' ? 15 : 30;
  const cacheEstimate = isRecording
    ? `${(bufferMinutes * 4.2).toFixed(1)} MB`
    : '0 MB';
  const timelineBars = Array.from({ length: isRecording ? 14 : 0 });

  const handleStart = async () => {
    try {
      await start(bufferMinutes);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Failed to start recording';
      Alert.alert('Error', message);
    }
  };

  const handleSave = async () => {
    try {
      const filePath = await save();
      setLastReplayPath(filePath);
      Alert.alert('Replay saved', filePath);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to save replay';
      Alert.alert('Error', message);
    }
  };

  const handleStop = async () => {
    try {
      await stop();
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Failed to stop recording';
      Alert.alert('Error', message);
    }
  };

  const handlePowerPress = () => {
    if (isRecording) {
      handleStop();
    } else {
      handleStart();
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
    } catch {
      Alert.alert('Error', 'Failed to share replay');
    }
  };

  const selectPremiumFeature = (feature: string) => {
    Alert.alert('ReplayBack Premium', `${feature} is a premium feature.`);
  };

  return (
    <View style={styles.container}>
      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.header}>
          <View style={styles.brandRow}>
            <View style={styles.brandIcon}>
              <Text style={styles.brandIconText}>RB</Text>
            </View>
            <View>
              <View style={styles.titleRow}>
                <Text style={styles.title}>ReplayBack</Text>
                <Text style={styles.freeBadge}>FREE</Text>
              </View>
              <Text style={styles.subtitle}>background buffer dashcam</Text>
            </View>
          </View>

          <View style={[styles.statusBadge, isRecording && styles.statusBadgeActive]}>
            <View style={[styles.statusDot, isRecording && styles.statusDotActive]} />
            <Text
              style={[
                styles.statusBadgeText,
                isRecording && styles.statusBadgeTextActive,
              ]}
            >
              {isRecording ? 'ACTIVE' : 'STANDBY'}
            </Text>
          </View>
        </View>

        <View style={styles.controllerCard}>
          <View style={[styles.outerRing, isRecording && styles.outerRingActive]}>
            <TouchableOpacity
              style={[styles.powerButton, isRecording && styles.powerButtonActive]}
              onPress={handlePowerPress}
              disabled={isLoading}
              activeOpacity={0.8}
            >
              <View style={styles.powerIconWrap}>
                {isLoading ? (
                  <ActivityIndicator color="#FFFFFF" size="small" />
                ) : (
                  <Text style={styles.powerIcon}>IO</Text>
                )}
              </View>
              <Text style={styles.powerText}>
                {isRecording ? 'STOP DASHCAM' : 'START DASHCAM'}
              </Text>
              <Text style={styles.powerSubText}>
                {isRecording ? 'background active' : 'background capture'}
              </Text>
            </TouchableOpacity>
          </View>

          <View style={styles.statsGrid}>
            <View style={styles.statCell}>
              <Text style={styles.statLabel}>Rolling Pool</Text>
              <Text style={styles.statValue}>{elapsedTime}</Text>
            </View>
            <View style={[styles.statCell, styles.statCellMiddle]}>
              <Text style={styles.statLabel}>Cache</Text>
              <Text style={styles.statValue}>{cacheEstimate}</Text>
            </View>
            <View style={styles.statCell}>
              <Text style={styles.statLabel}>FPS</Text>
              <Text style={styles.statValue}>
                {isRecording ? `${fps} fps` : '--'}
              </Text>
            </View>
          </View>
        </View>

        <View style={styles.card}>
          <View style={styles.cardHeader}>
            <Text style={styles.sectionTitle}>Audio stream</Text>
            <Text style={styles.waveText}>
              {isRecording && audioProfile !== 'Mute' ? '||||' : ''}
            </Text>
          </View>
          <View style={styles.pillGrid}>
            {(['Mute', 'System', 'Mic', 'Hybrid'] as const).map(profile => (
              <TouchableOpacity
                key={profile}
                style={[
                  styles.pillButton,
                  audioProfile === profile && styles.pillButtonActive,
                ]}
                onPress={() =>
                  profile === 'Hybrid'
                    ? selectPremiumFeature('Hybrid dual audio')
                    : setAudioProfile(profile)
                }
                activeOpacity={0.8}
              >
                <Text
                  style={[
                    styles.pillText,
                    audioProfile === profile && styles.pillTextActive,
                  ]}
                >
                  {profile}
                </Text>
                {profile === 'Hybrid' && <Text style={styles.proTag}>PRO</Text>}
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.card}>
          <View style={styles.cardHeader}>
            <Text style={styles.sectionTitle}>Buffer window</Text>
            <Text style={styles.valueBadge}>{bufferMinutes}:00 Min</Text>
          </View>
          <View style={styles.bufferOptions}>
            {BUFFER_OPTIONS.map(minutes => (
              <TouchableOpacity
                key={minutes}
                style={[
                  styles.bufferOption,
                  bufferMinutes === minutes && styles.bufferOptionActive,
                ]}
                onPress={() => setBufferMinutes(minutes)}
                disabled={isRecording}
              >
                <Text
                  style={[
                    styles.bufferOptionText,
                    bufferMinutes === minutes && styles.bufferOptionTextActive,
                  ]}
                >
                  {minutes}m
                </Text>
              </TouchableOpacity>
            ))}
          </View>
          <View style={styles.storageRow}>
            <Text style={styles.storageLabel}>Est. storage:</Text>
            <Text style={styles.storageValue}>
              ~{(bufferMinutes * 4.2).toFixed(1)} MB
            </Text>
          </View>
        </View>

        <View style={styles.card}>
          <View style={styles.cardHeader}>
            <Text style={styles.sectionTitle}>Segment timeline</Text>
            <Text style={styles.timelineLimit}>Max {bufferMinutes}:00</Text>
          </View>
          <View style={styles.timelineTrack}>
            {timelineBars.length === 0 ? (
              <Text style={styles.timelineEmpty}>Buffer inactive</Text>
            ) : (
              timelineBars.map((_, index) => (
                <View
                  key={index}
                  style={[
                    styles.timelineBar,
                    index === timelineBars.length - 1 && styles.timelineBarLive,
                  ]}
                />
              ))
            )}
          </View>
          <View style={styles.timelineLabels}>
            <Text style={styles.timelineLabel}>{bufferMinutes}m ago (purge)</Text>
            <Text style={styles.timelineLabel}>live</Text>
          </View>
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionTitle}>CPU governor</Text>
          <View style={styles.governorGrid}>
            {(['Gamer', 'Balanced', 'Saver'] as const).map(profile => (
              <TouchableOpacity
                key={profile}
                style={[
                  styles.governorButton,
                  governor === profile && styles.governorButtonActive,
                ]}
                onPress={() =>
                  profile === 'Gamer'
                    ? selectPremiumFeature('Gamer 60fps mode')
                    : setGovernor(profile)
                }
              >
                <Text
                  style={[
                    styles.governorText,
                    governor === profile && styles.governorTextActive,
                  ]}
                >
                  {profile}
                </Text>
                <Text style={styles.governorMeta}>
                  {profile === 'Gamer'
                    ? '60f PRO'
                    : profile === 'Balanced'
                    ? '30f'
                    : '15f'}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {error && (
          <View style={styles.errorBox}>
            <Text style={styles.errorText}>{error}</Text>
          </View>
        )}

        <View style={styles.saveGrid}>
          <TouchableOpacity
            style={[styles.savePreset, !isRecording && styles.disabledButton]}
            onPress={handleSave}
            disabled={!isRecording || isLoading}
            activeOpacity={0.8}
          >
            <Text style={styles.savePresetTitle}>SAVE LAST 2 MIN</Text>
            <Text style={styles.savePresetSub}>stitch segments</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.savePreset, !isRecording && styles.disabledButton]}
            onPress={handleSave}
            disabled={!isRecording || isLoading}
            activeOpacity={0.8}
          >
            <Text style={styles.savePresetTitle}>SAVE LAST 3 MIN</Text>
            <Text style={styles.savePresetSub}>stitch segments</Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          style={[styles.saveFullButton, !isRecording && styles.disabledButton]}
          onPress={handleSave}
          disabled={!isRecording || isLoading}
          activeOpacity={0.8}
        >
          {isLoading && isRecording ? (
            <ActivityIndicator color="#FFFFFF" />
          ) : (
            <Text style={styles.saveFullText}>
              SAVE FULL {bufferMinutes}:00 BUFFER
            </Text>
          )}
        </TouchableOpacity>

        <View style={styles.quickActions}>
          <TouchableOpacity
            style={styles.quickButton}
            onPress={() => selectPremiumFeature('Shake-to-save')}
          >
            <Text style={styles.quickTitle}>Shake-to-Save</Text>
            <Text style={styles.proTag}>PRO</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.quickButton} onPress={handleShare}>
            <Text style={styles.quickTitle}>Share last replay</Text>
            <Text style={styles.quickSub}>
              {lastReplayPath ? 'Ready' : `${recordingsCount} saved`}
            </Text>
          </TouchableOpacity>
        </View>

        {lastReplayPath && (
          <View style={styles.lastReplayCard}>
            <Text style={styles.lastReplayLabel}>Last replay</Text>
            <Text style={styles.lastReplayPath} numberOfLines={1}>
              {lastReplayPath.split('/').pop()}
            </Text>
          </View>
        )}

        {lastGalleryUri && (
          <View style={styles.lastReplayCard}>
            <Text style={styles.lastReplayLabel}>Gallery</Text>
            <Text style={styles.lastReplayPath} numberOfLines={1}>
              Saved to Movies/Replay
            </Text>
          </View>
        )}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  content: {
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 18,
  },
  header: {
    marginBottom: 14,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  brandIcon: {
    width: 42,
    height: 42,
    borderRadius: 14,
    backgroundColor: '#2563EB',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#2563EB',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.22,
    shadowRadius: 8,
    elevation: 3,
  },
  brandIconText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '900',
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  title: {
    fontSize: 20,
    fontWeight: '900',
    color: '#1E293B',
    letterSpacing: -0.3,
  },
  freeBadge: {
    overflow: 'hidden',
    borderRadius: 99,
    backgroundColor: '#DBEAFE',
    color: '#1D4ED8',
    fontSize: 9,
    fontWeight: '900',
    paddingHorizontal: 7,
    paddingVertical: 2,
  },
  subtitle: {
    color: '#94A3B8',
    fontSize: 10,
    fontWeight: '700',
    marginTop: 1,
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 11,
    paddingVertical: 7,
    borderRadius: 99,
    backgroundColor: '#F1F5F9',
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  statusBadgeActive: {
    backgroundColor: '#EFF6FF',
    borderColor: '#BFDBFE',
  },
  statusDot: {
    width: 7,
    height: 7,
    borderRadius: 4,
    backgroundColor: '#94A3B8',
  },
  statusDotActive: {
    backgroundColor: '#2563EB',
  },
  statusBadgeText: {
    color: '#64748B',
    fontSize: 10,
    fontWeight: '900',
  },
  statusBadgeTextActive: {
    color: '#1D4ED8',
  },
  controllerCard: {
    borderRadius: 22,
    borderWidth: 1,
    borderColor: '#DBEAFE',
    backgroundColor: '#FFFFFF',
    padding: 18,
    alignItems: 'center',
    shadowColor: '#93C5FD',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.18,
    shadowRadius: 16,
    elevation: 3,
  },
  outerRing: {
    width: 154,
    height: 154,
    borderRadius: 77,
    borderWidth: 1,
    borderColor: '#BFDBFE',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 18,
  },
  outerRingActive: {
    borderWidth: 3,
    borderColor: '#60A5FA',
    backgroundColor: '#EFF6FF',
  },
  powerButton: {
    width: 122,
    height: 122,
    borderRadius: 61,
    backgroundColor: '#EFF6FF',
    borderWidth: 1,
    borderColor: '#BFDBFE',
    alignItems: 'center',
    justifyContent: 'center',
  },
  powerButtonActive: {
    backgroundColor: '#DBEAFE',
  },
  powerIconWrap: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#2563EB',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 7,
  },
  powerIcon: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '900',
  },
  powerText: {
    color: '#334155',
    fontSize: 11,
    fontWeight: '900',
  },
  powerSubText: {
    color: '#94A3B8',
    fontSize: 9,
    fontWeight: '700',
    marginTop: 2,
  },
  statsGrid: {
    width: '100%',
    borderTopWidth: 1,
    borderTopColor: '#DBEAFE',
    paddingTop: 12,
    flexDirection: 'row',
  },
  statCell: {
    flex: 1,
    alignItems: 'center',
  },
  statCellMiddle: {
    borderLeftWidth: 1,
    borderRightWidth: 1,
    borderColor: '#DBEAFE',
  },
  statLabel: {
    color: '#94A3B8',
    fontSize: 9,
    fontWeight: '800',
  },
  statValue: {
    color: '#1E293B',
    fontSize: 14,
    fontWeight: '900',
    marginTop: 4,
  },
  card: {
    marginTop: 10,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#DBEAFE',
    backgroundColor: '#FFFFFF',
    padding: 12,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  sectionTitle: {
    color: '#2563EB',
    fontSize: 11,
    fontWeight: '900',
    textTransform: 'uppercase',
    letterSpacing: 0.4,
  },
  waveText: {
    color: '#2563EB',
    fontSize: 12,
    fontWeight: '900',
    letterSpacing: 2,
  },
  pillGrid: {
    flexDirection: 'row',
    gap: 8,
  },
  pillButton: {
    flex: 1,
    minHeight: 46,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
  },
  pillButtonActive: {
    backgroundColor: '#EFF6FF',
    borderColor: '#93C5FD',
  },
  pillText: {
    color: '#64748B',
    fontSize: 10,
    fontWeight: '900',
  },
  pillTextActive: {
    color: '#1D4ED8',
  },
  proTag: {
    color: '#2563EB',
    fontSize: 8,
    fontWeight: '900',
    marginTop: 2,
  },
  valueBadge: {
    overflow: 'hidden',
    borderRadius: 99,
    backgroundColor: '#EFF6FF',
    color: '#1D4ED8',
    fontSize: 12,
    fontWeight: '900',
    paddingHorizontal: 10,
    paddingVertical: 3,
  },
  bufferOptions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  bufferOption: {
    minWidth: 42,
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    backgroundColor: '#F8FAFC',
    alignItems: 'center',
  },
  bufferOptionActive: {
    backgroundColor: '#2563EB',
    borderColor: '#2563EB',
  },
  bufferOptionText: {
    color: '#64748B',
    fontSize: 10,
    fontWeight: '900',
  },
  bufferOptionTextActive: {
    color: '#FFFFFF',
  },
  storageRow: {
    marginTop: 10,
    borderRadius: 12,
    backgroundColor: '#F8FAFC',
    padding: 9,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  storageLabel: {
    color: '#94A3B8',
    fontSize: 10,
    fontWeight: '700',
  },
  storageValue: {
    color: '#334155',
    fontSize: 10,
    fontWeight: '900',
  },
  timelineTrack: {
    minHeight: 34,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#DBEAFE',
    backgroundColor: '#F8FAFC',
    padding: 3,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  timelineEmpty: {
    flex: 1,
    textAlign: 'center',
    color: '#94A3B8',
    fontSize: 10,
    fontWeight: '700',
  },
  timelineBar: {
    flex: 1,
    height: '100%',
    borderRadius: 4,
    backgroundColor: '#93C5FD',
  },
  timelineBarLive: {
    backgroundColor: '#2563EB',
  },
  timelineLabels: {
    marginTop: 5,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  timelineLabel: {
    color: '#94A3B8',
    fontSize: 9,
    fontWeight: '700',
  },
  timelineLimit: {
    color: '#94A3B8',
    fontSize: 10,
    fontWeight: '800',
  },
  governorGrid: {
    marginTop: 10,
    flexDirection: 'row',
    gap: 8,
  },
  governorButton: {
    flex: 1,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    paddingVertical: 10,
    alignItems: 'center',
  },
  governorButtonActive: {
    backgroundColor: '#EFF6FF',
    borderColor: '#93C5FD',
  },
  governorText: {
    color: '#475569',
    fontSize: 10,
    fontWeight: '900',
  },
  governorTextActive: {
    color: '#1D4ED8',
  },
  governorMeta: {
    color: '#94A3B8',
    fontSize: 8,
    fontWeight: '800',
    marginTop: 3,
  },
  errorBox: {
    marginTop: 10,
    backgroundColor: '#FEF2F2',
    borderLeftWidth: 4,
    borderLeftColor: '#EF4444',
    borderRadius: 12,
    padding: 12,
  },
  errorText: {
    color: '#B91C1C',
    fontSize: 12,
    fontWeight: '700',
  },
  saveGrid: {
    marginTop: 12,
    flexDirection: 'row',
    gap: 10,
  },
  savePreset: {
    flex: 1,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#BFDBFE',
    backgroundColor: '#FFFFFF',
    padding: 13,
  },
  savePresetTitle: {
    color: '#2563EB',
    fontSize: 12,
    fontWeight: '900',
  },
  savePresetSub: {
    color: '#94A3B8',
    fontSize: 9,
    fontWeight: '700',
    marginTop: 3,
  },
  saveFullButton: {
    marginTop: 10,
    minHeight: 50,
    borderRadius: 16,
    backgroundColor: '#2563EB',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#2563EB',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.22,
    shadowRadius: 10,
    elevation: 3,
  },
  saveFullText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '900',
  },
  disabledButton: {
    opacity: 0.48,
  },
  quickActions: {
    marginTop: 10,
    flexDirection: 'row',
    gap: 10,
  },
  quickButton: {
    flex: 1,
    borderRadius: 16,
    backgroundColor: '#F8FAFC',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    padding: 12,
  },
  quickTitle: {
    color: '#334155',
    fontSize: 11,
    fontWeight: '900',
  },
  quickSub: {
    color: '#94A3B8',
    fontSize: 9,
    fontWeight: '700',
    marginTop: 3,
  },
  lastReplayCard: {
    marginTop: 10,
    borderRadius: 14,
    backgroundColor: '#EFF6FF',
    borderWidth: 1,
    borderColor: '#BFDBFE',
    padding: 12,
  },
  lastReplayLabel: {
    color: '#2563EB',
    fontSize: 10,
    fontWeight: '900',
    textTransform: 'uppercase',
  },
  lastReplayPath: {
    color: '#334155',
    fontSize: 12,
    fontWeight: '700',
    marginTop: 4,
  },
});
