import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  RefreshControl,
} from 'react-native';
import { ReplayButton } from '../components/ReplayButton';
import type { SavedReplay } from '../modules/ReplayBuffer/types';

interface RecordingsScreenProps {
  savedReplays: SavedReplay[];
  recordingsCount: number;
  isLoading: boolean;
  error: string | null;
  refreshSavedReplays: () => Promise<void>;
}

const formatDate = (timestamp: number) => {
  const date = new Date(timestamp);
  return date.toLocaleString();
};

export const RecordingsScreen: React.FC<RecordingsScreenProps> = ({
  savedReplays,
  recordingsCount,
  isLoading,
  error,
  refreshSavedReplays,
}) => {
  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Saved Recordings</Text>
        <Text style={styles.subtitle}>{recordingsCount} saved</Text>
      </View>

      <View style={styles.actions}>
        <ReplayButton
          title="Refresh List"
          onPress={refreshSavedReplays}
          isLoading={isLoading}
          variant="secondary"
        />
      </View>

      {error ? (
        <View style={styles.errorBox}>
          <Text style={styles.errorText}>{error}</Text>
        </View>
      ) : null}

      <FlatList
        data={savedReplays}
        keyExtractor={(item) => item.path}
        refreshControl={
          <RefreshControl refreshing={isLoading} onRefresh={refreshSavedReplays} />
        }
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={styles.emptyText}>
              No saved recordings yet. Start recording and save your replay.
            </Text>
          </View>
        }
        renderItem={({ item }) => (
          <View style={styles.recordingCard}>
            <Text style={styles.recordingName}>{item.name}</Text>
            <Text style={styles.recordingMeta} numberOfLines={1}>
              {formatDate(item.modifiedAt)} · {(item.sizeBytes / 1024).toFixed(1)} KB
            </Text>
            <Text style={styles.recordingPath} numberOfLines={1}>
              {item.path}
            </Text>
          </View>
        )}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    paddingHorizontal: 16,
    paddingVertical: 20,
    backgroundColor: '#fff',
    borderBottomColor: '#e6e6e6',
    borderBottomWidth: 1,
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    color: '#000',
  },
  subtitle: {
    marginTop: 4,
    color: '#666',
    fontSize: 14,
  },
  actions: {
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  listContent: {
    paddingHorizontal: 16,
    paddingBottom: 24,
  },
  recordingCard: {
    backgroundColor: '#fff',
    borderRadius: 14,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 1,
  },
  recordingName: {
    fontSize: 16,
    fontWeight: '700',
    color: '#000',
  },
  recordingMeta: {
    marginTop: 4,
    color: '#666',
    fontSize: 13,
  },
  recordingPath: {
    marginTop: 8,
    color: '#007AFF',
    fontSize: 12,
  },
  emptyState: {
    marginTop: 40,
    alignItems: 'center',
  },
  emptyText: {
    color: '#666',
    fontSize: 15,
    textAlign: 'center',
  },
  errorBox: {
    backgroundColor: '#FFE5E5',
    borderLeftWidth: 4,
    borderLeftColor: '#FF3B30',
    borderRadius: 8,
    padding: 12,
    marginHorizontal: 16,
    marginBottom: 12,
  },
  errorText: {
    color: '#C41E3A',
    fontSize: 14,
    fontWeight: '500',
  },
});
