import React, { useState } from 'react';
import {
  View,
  FlatList,
  StyleSheet,
  TouchableOpacity,
} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Chip,
  IconButton,
  Searchbar,
  useTheme,
  Button,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

import { useConnections } from '../hooks/useConnections';
import { formatTraffic, formatDuration } from '../utils/format';
import { closeConnection, closeAllConnections } from '../services/api';

export default function ConnectionsScreen() {
  const theme = useTheme();
  const [searchQuery, setSearchQuery] = useState('');

  const { connections, mutate } = useConnections();

  const handleCloseConnection = async (id: string) => {
    try {
      await closeConnection(id);
      await mutate();
    } catch (error) {
      console.error('关闭连接失败:', error);
    }
  };

  const handleCloseAllConnections = async () => {
    try {
      await closeAllConnections();
      await mutate();
    } catch (error) {
      console.error('关闭所有连接失败:', error);
    }
  };

  const filteredConnections = connections.filter(conn => {
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    return (
      conn.metadata.host?.toLowerCase().includes(query) ||
      conn.metadata.destinationIP?.toLowerCase().includes(query) ||
      conn.chains?.join(',').toLowerCase().includes(query)
    );
  });

  const renderConnectionItem = ({ item }: { item: any }) => {
    const { metadata, chains, upload, download, start } = item;
    const duration = Date.now() - new Date(start).getTime();

    return (
      <Card style={styles.connectionCard}>
        <Card.Content>
          <View style={styles.connectionHeader}>
            <View style={styles.connectionInfo}>
              <Icon
                name={metadata.type === 'HTTP' ? 'web' : 'shield-check'}
                size={20}
                color={theme.colors.primary}
              />
              <View style={styles.connectionDetails}>
                <Title style={styles.connectionHost} numberOfLines={1}>
                  {metadata.host || metadata.destinationIP}
                </Title>
                <Paragraph style={styles.connectionMeta}>
                  {metadata.type} → {chains?.join(' → ')}
                </Paragraph>
              </View>
            </View>
            <IconButton
              icon="close"
              size={20}
              onPress={() => handleCloseConnection(item.id)}
            />
          </View>
          <View style={styles.connectionStats}>
            <Chip
              mode="outlined"
              compact
              style={styles.chip}
              icon="arrow-up"
            >
              ↑ {formatTraffic(upload)}
            </Chip>
            <Chip
              mode="outlined"
              compact
              style={styles.chip}
              icon="arrow-down"
            >
              ↓ {formatTraffic(download)}
            </Chip>
            <Chip
              mode="outlined"
              compact
              style={styles.chip}
              icon="clock"
            >
              {formatDuration(duration)}
            </Chip>
          </View>
        </Card.Content>
      </Card>
    );
  };

  return (
    <View style={styles.container}>
      {/* 搜索栏 */}
      <Searchbar
        placeholder="搜索连接..."
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={styles.searchBar}
      />

      {/* 统计信息 */}
      <Card style={styles.statsCard}>
        <Card.Content>
          <View style={styles.statsContainer}>
            <View style={styles.statItem}>
              <Title>{connections.length}</Title>
              <Paragraph>活动连接</Paragraph>
            </View>
            <View style={styles.statItem}>
              <Button
                mode="contained"
                onPress={handleCloseAllConnections}
                compact
                disabled={connections.length === 0}
              >
                关闭全部
              </Button>
            </View>
          </View>
        </Card.Content>
      </Card>

      {/* 连接列表 */}
      <FlatList
        data={filteredConnections}
        renderItem={renderConnectionItem}
        keyExtractor={item => item.id}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Icon name="lan-disconnect" size={64} color="gray" />
            <Paragraph style={styles.emptyText}>暂无活动连接</Paragraph>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  searchBar: {
    margin: 16,
    elevation: 2,
  },
  statsCard: {
    marginHorizontal: 16,
    marginBottom: 16,
    elevation: 2,
  },
  statsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  statItem: {
    alignItems: 'center',
  },
  listContent: {
    padding: 16,
  },
  connectionCard: {
    marginBottom: 12,
    elevation: 2,
  },
  connectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  connectionInfo: {
    flexDirection: 'row',
    flex: 1,
  },
  connectionDetails: {
    flex: 1,
    marginLeft: 8,
  },
  connectionHost: {
    fontSize: 14,
    marginBottom: 2,
  },
  connectionMeta: {
    fontSize: 12,
    color: 'gray',
  },
  connectionStats: {
    flexDirection: 'row',
    gap: 8,
  },
  chip: {
    height: 28,
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

