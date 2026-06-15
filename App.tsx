/**
 * @format
 */

import { useState } from 'react';
import {
  StatusBar,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ReplayScreen } from './src/screens/ReplayScreen';
import { RecordingsScreen } from './src/screens/RecordingsScreen';
import { SettingsScreen } from './src/screens/SettingsScreen';
import { useReplayBuffer } from './src/hooks/useReplayBuffer';

function App() {
  const [activeTab, setActiveTab] = useState<
    'dashcam' | 'recordings' | 'settings'
  >('dashcam');

  const replayState = useReplayBuffer();

  return (
    <SafeAreaProvider>
      <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

      <View style={styles.container}>
        {/* Main Content */}
        <View style={styles.content}>
          {activeTab === 'dashcam' ? (
            <ReplayScreen {...replayState} />
          ) : activeTab === 'recordings' ? (
            <RecordingsScreen {...replayState} />
          ) : (
            <SettingsScreen {...replayState} />
          )}
        </View>

        {/* Bottom Tab Bar */}
        <View style={styles.tabBar}>
          <NavButton
            label="Dashcam"
            active={activeTab === 'dashcam'}
            onPress={() => setActiveTab('dashcam')}
          />
          <NavButton
            label="Replays"
            active={activeTab === 'recordings'}
            onPress={() => setActiveTab('recordings')}
          />
          <NavButton
            label="Settings"
            active={activeTab === 'settings'}
            onPress={() => setActiveTab('settings')}
          />
        </View>
      </View>
    </SafeAreaProvider>
  );
}

/* ✅ Cleaner Nav Button */
const NavButton = ({
  label,
  active,
  onPress,
}: {
  label: string;
  active: boolean;
  onPress: () => void;
}) => (
  <TouchableOpacity
    style={[styles.tabButton, active && styles.tabButtonActive]}
    onPress={onPress}
    activeOpacity={0.8}
  >
    {active && <View style={styles.activeIndicator} />}

    <Text
      style={[styles.tabText, active && styles.activeText]}
    >
      {label}
    </Text>
  </TouchableOpacity>
);

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },

  content: {
    flex: 1,
  },

 tabBar: {
  flexDirection: 'row',
  margin: 12,
  borderRadius: 18,
  backgroundColor: '#FFFFFF',
  paddingVertical: 8,
  paddingHorizontal: 6,

  // iOS shadow
  shadowColor: '#000',
  shadowOpacity: 0.08,
  shadowRadius: 10,
  shadowOffset: { width: 0, height: 4 },

  // Android shadow
  elevation: 6,
},

tabButton: {
  flex: 1,
  alignItems: 'center',
  justifyContent: 'center',
  paddingVertical: 8,
  borderRadius: 12,
},

tabButtonActive: {
  backgroundColor: '#EFF6FF',
},

tabText: {
  fontSize: 13,
  fontWeight: '700',
  color: '#9CA3AF',
},

activeText: {
  color: '#2563EB',
},

activeIndicator: {
  position: 'absolute',
  top: 4,
  width: 20,
  height: 3,
  borderRadius: 10,
  backgroundColor: '#2563EB',
},
});

export default App;