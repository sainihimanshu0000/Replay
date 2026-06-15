import React from 'react';
import {
  FlatList,
  RefreshControl,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
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
  return date.toLocaleString([], {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const formatSize = (sizeBytes: number) => {
  if (sizeBytes >= 1024 * 1024) {
    return `${(sizeBytes / 1024 / 1024).toFixed(1)} MB`;
  }

  return `${(sizeBytes / 1024).toFixed(1)} KB`;
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
        <View>
          <Text style={styles.title}>Saved Replays</Text>
          <Text style={styles.subtitle}>{recordingsCount} saved clips</Text>
        </View>
        <TouchableOpacity
          style={styles.refreshButton}
          onPress={refreshSavedReplays}
          activeOpacity={0.8}
        >
          <Text style={styles.refreshText}>Refresh</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.searchMock}>
        <Text style={styles.searchText}>Search recordings...</Text>
      </View>

      {error ? (
        <View style={styles.errorBox}>
          <Text style={styles.errorText}>{error}</Text>
        </View>
      ) : null}

      <FlatList
        data={savedReplays}
        keyExtractor={item => item.path}
        refreshControl={
          <RefreshControl refreshing={isLoading} onRefresh={refreshSavedReplays} />
        }
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <View style={styles.emptyIcon}>
              <Text style={styles.emptyIconText}>RB</Text>
            </View>
            <Text style={styles.emptyTitle}>No replays yet</Text>
            <Text style={styles.emptyText}>Start dashcam and save clips</Text>
          </View>
        }
        renderItem={({ item }) => (
          <View style={styles.recordingCard}>
            <View style={styles.thumbnail}>
              <Text style={styles.thumbnailText}>PLAY</Text>
            </View>
            <View style={styles.recordingInfo}>
              <Text style={styles.recordingName} numberOfLines={1}>
                {item.name}
              </Text>
              <Text style={styles.recordingMeta} numberOfLines={1}>
                {formatDate(item.modifiedAt)} - {formatSize(item.sizeBytes)}
              </Text>
              <Text style={styles.recordingPath} numberOfLines={1}>
                {item.path}
              </Text>
            </View>
          </View>
        )}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  header: {
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 12,
    borderBottomColor: '#DBEAFE',
    borderBottomWidth: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    fontSize: 20,
    fontWeight: '900',
    color: '#1E293B',
    letterSpacing: -0.2,
  },
  subtitle: {
    marginTop: 3,
    color: '#94A3B8',
    fontSize: 10,
    fontWeight: '800',
  },
  refreshButton: {
    borderRadius: 99,
    backgroundColor: '#EFF6FF',
    borderWidth: 1,
    borderColor: '#BFDBFE',
    paddingHorizontal: 12,
    paddingVertical: 7,
  },
  refreshText: {
    color: '#2563EB',
    fontSize: 11,
    fontWeight: '900',
  },
  searchMock: {
    marginHorizontal: 16,
    marginTop: 12,
    borderRadius: 14,
    backgroundColor: '#F8FAFC',
    borderWidth: 1,
    borderColor: '#DBEAFE',
    paddingHorizontal: 14,
    paddingVertical: 11,
  },
  searchText: {
    color: '#94A3B8',
    fontSize: 12,
    fontWeight: '700',
  },
  errorBox: {
    backgroundColor: '#FEF2F2',
    borderLeftWidth: 4,
    borderLeftColor: '#EF4444',
    borderRadius: 12,
    padding: 12,
    marginHorizontal: 16,
    marginTop: 12,
  },
  errorText: {
    color: '#B91C1C',
    fontSize: 12,
    fontWeight: '700',
  },
  listContent: {
    paddingHorizontal: 16,
    paddingTop: 14,
    paddingBottom: 24,
  },
  recordingCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 18,
    borderWidth: 1,
    borderColor: '#DBEAFE',
    padding: 12,
    marginBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#93C5FD',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.14,
    shadowRadius: 10,
    elevation: 2,
  },
  thumbnail: {
    width: 52,
    height: 52,
    borderRadius: 16,
    backgroundColor: '#EFF6FF',
    borderWidth: 1,
    borderColor: '#BFDBFE',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  thumbnailText: {
    color: '#2563EB',
    fontSize: 9,
    fontWeight: '900',
  },
  recordingInfo: {
    flex: 1,
  },
  recordingName: {
    fontSize: 14,
    fontWeight: '900',
    color: '#1E293B',
  },
  recordingMeta: {
    marginTop: 4,
    color: '#94A3B8',
    fontSize: 10,
    fontWeight: '800',
  },
  recordingPath: {
    marginTop: 6,
    color: '#2563EB',
    fontSize: 10,
    fontWeight: '700',
  },
  emptyState: {
    marginTop: 70,
    alignItems: 'center',
  },
  emptyIcon: {
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: '#F1F5F9',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
  },
  emptyIconText: {
    color: '#94A3B8',
    fontSize: 16,
    fontWeight: '900',
  },
  emptyTitle: {
    color: '#64748B',
    fontSize: 13,
    fontWeight: '900',
  },
  emptyText: {
    color: '#CBD5E1',
    fontSize: 10,
    fontWeight: '800',
    marginTop: 4,
  },
});
