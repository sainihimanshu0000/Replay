/**
 * @format
 */

import { useState } from 'react';
import {
  StatusBar,
  useColorScheme,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ReplayScreen } from './src/screens/ReplayScreen';
import { RecordingsScreen } from './src/screens/RecordingsScreen';
import { useReplayBuffer } from './src/hooks/useReplayBuffer';

function App() {
  const isDarkMode = useColorScheme() === 'dark';
  const [activeTab, setActiveTab] = useState<'recorder' | 'recordings'>('recorder');
  const replayState = useReplayBuffer();

  return (
    <SafeAreaProvider>
      <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
      <View style={styles.tabBar}>
        <TouchableOpacity
          style={[styles.tabButton, activeTab === 'recorder' && styles.tabButtonActive]}
          onPress={() => setActiveTab('recorder')}
        >
          <Text style={[styles.tabText, activeTab === 'recorder' && styles.tabTextActive]}>
            Recorder
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tabButton, activeTab === 'recordings' && styles.tabButtonActive]}
          onPress={() => setActiveTab('recordings')}
        >
          <Text style={[styles.tabText, activeTab === 'recordings' && styles.tabTextActive]}>
            Recordings
          </Text>
        </TouchableOpacity>
      </View>
      {activeTab === 'recorder' ? (
        <ReplayScreen {...replayState} />
      ) : (
        <RecordingsScreen {...replayState} />
      )}
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  tabBar: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingVertical: 10,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e6e6e6',
  },
  tabButton: {
    paddingHorizontal: 24,
    paddingVertical: 8,
    borderRadius: 24,
  },
  tabButtonActive: {
    backgroundColor: '#007AFF',
  },
  tabText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  tabTextActive: {
    color: '#fff',
  },
});

export default App;
