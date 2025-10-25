import React, { useState } from 'react';
import { View, FlatList, StyleSheet } from 'react-native';
import {
  Card,
  Paragraph,
  Chip,
  SegmentedButtons,
  IconButton,
  useTheme,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

import { useLogs } from '../hooks/useLogs';
import { formatTime } from '../utils/format';

export default function LogsScreen() {
  const theme = useTheme();
  const [logLevel, setLogLevel] = useState('all');

  const { logs, clearLogs } = useLogs();

  const filteredLogs = logs.filter(log => {
    if (logLevel === 'all') return true;
    return log.type === logLevel;
  });

  const renderLogItem = ({ item }: { item: any }) => {
    const color = getLogColor(item.type, theme);

    return (
      <Card style={[styles.logCard, { borderLeftColor: color, borderLeftWidth: 4 }]}>
        <Card.Content>
          <View style={styles.logHeader}>
            <Chip
              mode="outlined"
              compact
              style={[styles.levelChip, { borderColor: color }]}
              textStyle={{ color }}
            >
              {item.type}
            </Chip>
            <Paragraph style={styles.logTime}>{formatTime(item.time)}</Paragraph>
          </View>
          <Paragraph style={styles.logPayload}>{item.payload}</Paragraph>
        </Card.Content>
      </Card>
    );
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <SegmentedButtons
          value={logLevel}
          onValueChange={setLogLevel}
          buttons={[
            { value: 'all', label: '全部' },
            { value: 'info', label: 'Info' },
            { value: 'warning', label: 'Warn' },
            { value: 'error', label: 'Error' },
          ]}
          style={styles.segmentedButtons}
        />
        <IconButton
          icon="delete"
          iconColor={theme.colors.error}
          onPress={clearLogs}
        />
      </View>

      <FlatList
        data={filteredLogs}
        renderItem={renderLogItem}
        keyExtractor={(item, index) => `${item.time}-${index}`}
        contentContainerStyle={styles.listContent}
        inverted
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Icon name="text-box-remove" size={64} color="gray" />
            <Paragraph style={styles.emptyText}>暂无日志</Paragraph>
          </View>
        }
      />
    </View>
  );
}

function getLogColor(type: string, theme: any): string {
  switch (type) {
    case 'error':
      return theme.colors.error;
    case 'warning':
      return '#FF9800';
    case 'info':
      return theme.colors.primary;
    default:
      return 'gray';
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    gap: 8,
  },
  segmentedButtons: {
    flex: 1,
  },
  listContent: {
    padding: 16,
  },
  logCard: {
    marginBottom: 12,
    elevation: 2,
  },
  logHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  levelChip: {
    height: 28,
  },
  logTime: {
    fontSize: 12,
    color: 'gray',
  },
  logPayload: {
    fontSize: 14,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 64,
  },
  emptyText: {
    marginTop: 16,
    color: 'gray',
  },
});

