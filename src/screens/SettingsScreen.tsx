import React, { useState } from 'react';
import {
  Alert,
  NativeModules,
  Platform,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';

const { FloatingModule } = NativeModules as {
  FloatingModule?: {
    start: () => Promise<boolean>;
    stop: () => Promise<boolean>;
    requestPermission: () => void;
  };
};

interface SettingsScreenProps {
  requestBackgroundPermission: () => Promise<void>;
  isRecording: boolean;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  requestBackgroundPermission,
  isRecording,
}) => {
  const [governor, setGovernor] = useState<'Gamer' | 'Balanced' | 'Saver'>(
    'Balanced'
  );
  const [floatingButton, setFloatingButton] = useState(false);
  const [gameHook, setGameHook] = useState(true);
  const [resolution, setResolution] = useState<'540p' | '720p' | '1080p'>(
    '720p'
  );
  const [chunkDuration, setChunkDuration] = useState<15 | 30 | 60>(30);

  const handleFloatingToggle = async (value: boolean) => {
    setFloatingButton(value);

    if (Platform.OS !== 'android') {
      setFloatingButton(false);
      Alert.alert('Not supported', 'Floating bubble works only on Android');
      return;
    }

    if (!FloatingModule) {
      setFloatingButton(false);
      Alert.alert('Unavailable', 'FloatingModule is not registered');
      return;
    }

    try {
      if (value) {
        await FloatingModule.start();
      } else {
        await FloatingModule.stop();
      }
    } catch {
      setFloatingButton(false);
      Alert.alert(
        'Permission required',
        'Enable "Display over other apps" to show the floating capture button.',
        [
          {
            text: 'Open Settings',
            onPress: () => FloatingModule.requestPermission(),
          },
          { text: 'Cancel', style: 'cancel' },
        ]
      );
    }
  };

  const handleBatteryPress = async () => {
    try {
      await requestBackgroundPermission();
      Alert.alert(
        'Battery optimization',
        'Set ReplayBack to "Unrestricted"'
      );
    } catch {
      Alert.alert('Error', 'Failed to open settings');
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
          <Text style={styles.title}>Settings</Text>
          <Text style={styles.subtitle}>performance and recording rules</Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionTitle}>CPU governor</Text>
          <View style={styles.segmentGrid}>
            {(['Gamer', 'Balanced', 'Saver'] as const).map(profile => (
              <TouchableOpacity
                key={profile}
                style={[
                  styles.segmentButton,
                  governor === profile && styles.segmentButtonActive,
                ]}
                onPress={() =>
                  profile === 'Gamer'
                    ? selectPremiumFeature('Gamer 60fps mode')
                    : setGovernor(profile)
                }
              >
                <Text
                  style={[
                    styles.segmentText,
                    governor === profile && styles.segmentTextActive,
                  ]}
                >
                  {profile}
                </Text>
                <Text style={styles.segmentMeta}>
                  {profile === 'Gamer'
                    ? '60f PRO'
                    : profile === 'Balanced'
                    ? '30f'
                    : 'thermal'}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
          <Text style={styles.helperText}>
            {governor === 'Balanced'
              ? 'Optimized balanced profile'
              : governor === 'Saver'
              ? 'Battery saver with thermal guard'
              : 'High performance capture'}
          </Text>
        </View>

        <View style={styles.toggleCard}>
          <View>
            <Text style={styles.toggleTitle}>Floating capture button</Text>
            <Text style={styles.toggleSub}>quick-save bubble overlay</Text>
          </View>
          <Switch
            value={floatingButton}
            onValueChange={handleFloatingToggle}
            trackColor={{ false: '#CBD5E1', true: '#93C5FD' }}
            thumbColor={floatingButton ? '#2563EB' : '#FFFFFF'}
          />
        </View>

        <View style={styles.toggleCard}>
          <View>
            <Text style={styles.toggleTitle}>Game hook</Text>
            <Text style={styles.toggleSub}>listen for capture shortcuts</Text>
          </View>
          <Switch
            value={gameHook}
            onValueChange={setGameHook}
            trackColor={{ false: '#CBD5E1', true: '#93C5FD' }}
            thumbColor={gameHook ? '#2563EB' : '#FFFFFF'}
          />
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Resolution</Text>
          <View style={styles.segmentGrid}>
            {(['540p', '720p', '1080p'] as const).map(option => (
              <TouchableOpacity
                key={option}
                style={[
                  styles.segmentButton,
                  resolution === option && styles.segmentButtonActive,
                ]}
                onPress={() =>
                  option === '1080p'
                    ? selectPremiumFeature('1080p capture')
                    : setResolution(option)
                }
              >
                <Text
                  style={[
                    styles.segmentText,
                    resolution === option && styles.segmentTextActive,
                  ]}
                >
                  {option}
                </Text>
                <Text style={styles.segmentMeta}>
                  {option === '1080p'
                    ? 'HQ PRO'
                    : option === '720p'
                    ? 'balanced'
                    : 'saver'}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Chunk duration</Text>
          <View style={styles.segmentGrid}>
            {([15, 30, 60] as const).map(option => (
              <TouchableOpacity
                key={option}
                style={[
                  styles.segmentButton,
                  chunkDuration === option && styles.segmentButtonActive,
                ]}
                onPress={() => setChunkDuration(option)}
              >
                <Text
                  style={[
                    styles.segmentText,
                    chunkDuration === option && styles.segmentTextActive,
                  ]}
                >
                  {option}s
                </Text>
                <Text style={styles.segmentMeta}>segment</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.permissionsCard}>
          <View style={styles.permissionRow}>
            <Text style={styles.permissionLabel}>Screen projection</Text>
            <Text style={styles.permissionValue}>
              {isRecording ? 'Active' : 'Authorized'}
            </Text>
          </View>
          <View style={styles.permissionRow}>
            <Text style={styles.permissionLabel}>Notification</Text>
            <Text style={styles.permissionValue}>On</Text>
          </View>
          <TouchableOpacity
            style={styles.batteryButton}
            onPress={handleBatteryPress}
            activeOpacity={0.8}
          >
            <Text style={styles.batteryButtonText}>
              Disable battery optimization
            </Text>
          </TouchableOpacity>
        </View>
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
    paddingBottom: 24,
  },
  header: {
    paddingBottom: 14,
    borderBottomColor: '#DBEAFE',
    borderBottomWidth: 1,
  },
  title: {
    color: '#1E293B',
    fontSize: 20,
    fontWeight: '900',
  },
  subtitle: {
    color: '#94A3B8',
    fontSize: 10,
    fontWeight: '800',
    marginTop: 3,
  },
  card: {
    marginTop: 16,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#DBEAFE',
    backgroundColor: '#FFFFFF',
    padding: 12,
  },
  sectionTitle: {
    color: '#2563EB',
    fontSize: 11,
    fontWeight: '900',
    textTransform: 'uppercase',
    letterSpacing: 0.4,
    marginBottom: 10,
  },
  segmentGrid: {
    flexDirection: 'row',
    gap: 8,
  },
  segmentButton: {
    flex: 1,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    paddingVertical: 11,
  },
  segmentButtonActive: {
    backgroundColor: '#EFF6FF',
    borderColor: '#93C5FD',
  },
  segmentText: {
    color: '#475569',
    fontSize: 11,
    fontWeight: '900',
  },
  segmentTextActive: {
    color: '#1D4ED8',
  },
  segmentMeta: {
    marginTop: 3,
    color: '#94A3B8',
    fontSize: 8,
    fontWeight: '800',
  },
  helperText: {
    color: '#94A3B8',
    fontSize: 10,
    fontWeight: '700',
    textAlign: 'center',
    marginTop: 10,
  },
  toggleCard: {
    marginTop: 12,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#DBEAFE',
    backgroundColor: '#F8FAFC',
    padding: 13,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  toggleTitle: {
    color: '#334155',
    fontSize: 13,
    fontWeight: '900',
  },
  toggleSub: {
    color: '#94A3B8',
    fontSize: 10,
    fontWeight: '700',
    marginTop: 3,
  },
  permissionsCard: {
    marginTop: 16,
    borderRadius: 16,
    backgroundColor: '#F8FAFC',
    borderWidth: 1,
    borderColor: '#DBEAFE',
    padding: 13,
  },
  permissionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 7,
  },
  permissionLabel: {
    color: '#334155',
    fontSize: 12,
    fontWeight: '800',
  },
  permissionValue: {
    color: '#64748B',
    fontSize: 12,
    fontWeight: '800',
  },
  batteryButton: {
    marginTop: 10,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#BFDBFE',
    backgroundColor: '#EFF6FF',
    paddingVertical: 10,
    alignItems: 'center',
  },
  batteryButtonText: {
    color: '#2563EB',
    fontSize: 11,
    fontWeight: '900',
  },
});
