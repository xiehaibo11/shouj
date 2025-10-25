import React, { useState, useMemo } from 'react';
import {
  View,
  FlatList,
  StyleSheet,
  TouchableOpacity,
  TextInput,
} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Chip,
  RadioButton,
  Searchbar,
  SegmentedButtons,
  useTheme,
  ActivityIndicator,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

import { useProxies } from '../hooks/useProxies';
import { testDelay, selectProxy } from '../services/api';

type SortType = 'default' | 'delay' | 'name';
type FilterType = 'all' | 'available' | 'unavailable';

export default function ProxiesScreen() {
  const theme = useTheme();
  const [searchQuery, setSearchQuery] = useState('');
  const [sortType, setSortType] = useState<SortType>('default');
  const [filterType, setFilterType] = useState<FilterType>('all');
  const [testingIds, setTestingIds] = useState<Set<string>>(new Set());

  const { groups, proxies, currentGroup, selectGroup, mutate } = useProxies();

  // 过滤和排序代理
  const filteredProxies = useMemo(() => {
    let result = [...proxies];

    // 搜索过滤
    if (searchQuery) {
      result = result.filter(p =>
        p.name.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    // 状态过滤
    if (filterType === 'available') {
      result = result.filter(p => p.delay && p.delay > 0);
    } else if (filterType === 'unavailable') {
      result = result.filter(p => !p.delay || p.delay === 0);
    }

    // 排序
    if (sortType === 'delay') {
      result.sort((a, b) => {
        if (!a.delay) return 1;
        if (!b.delay) return -1;
        return a.delay - b.delay;
      });
    } else if (sortType === 'name') {
      result.sort((a, b) => a.name.localeCompare(b.name));
    }

    return result;
  }, [proxies, searchQuery, sortType, filterType]);

  const handleTestDelay = async (proxyName: string) => {
    setTestingIds(prev => new Set(prev).add(proxyName));
    try {
      await testDelay(proxyName);
      await mutate();
    } catch (error) {
      console.error('测速失败:', error);
    } finally {
      setTestingIds(prev => {
        const next = new Set(prev);
        next.delete(proxyName);
        return next;
      });
    }
  };

  const handleSelectProxy = async (proxyName: string) => {
    try {
      await selectProxy(currentGroup, proxyName);
      await mutate();
    } catch (error) {
      console.error('选择节点失败:', error);
    }
  };

  const renderProxyItem = ({ item }: { item: any }) => {
    const isTesting = testingIds.has(item.name);
    const isSelected = item.now === item.name;

    return (
      <TouchableOpacity
        onPress={() => handleSelectProxy(item.name)}
        onLongPress={() => handleTestDelay(item.name)}
      >
        <Card
          style={[
            styles.proxyCard,
            isSelected && { borderColor: theme.colors.primary, borderWidth: 2 },
          ]}
        >
          <Card.Content style={styles.proxyContent}>
            <View style={styles.proxyInfo}>
              <RadioButton
                value={item.name}
                status={isSelected ? 'checked' : 'unchecked'}
                onPress={() => handleSelectProxy(item.name)}
              />
              <View style={styles.proxyDetails}>
                <Title style={styles.proxyName}>{item.name}</Title>
                <View style={styles.proxyMeta}>
                  <Chip
                    mode="outlined"
                    compact
                    style={styles.chip}
                    icon="dns"
                  >
                    {item.type}
                  </Chip>
                  {item.delay ? (
                    <Chip
                      mode="outlined"
                      compact
                      style={[
                        styles.chip,
                        {
                          borderColor:
                            item.delay < 200
                              ? theme.colors.primary
                              : item.delay < 500
                              ? '#FF9800'
                              : theme.colors.error,
                        },
                      ]}
                      icon="speedometer"
                    >
                      {item.delay}ms
                    </Chip>
                  ) : (
                    <Chip
                      mode="outlined"
                      compact
                      style={styles.chip}
                      icon="help-circle"
                    >
                      未测速
                    </Chip>
                  )}
                </View>
              </View>
            </View>
            <TouchableOpacity
              onPress={() => handleTestDelay(item.name)}
              disabled={isTesting}
            >
              {isTesting ? (
                <ActivityIndicator size="small" />
              ) : (
                <Icon
                  name="speedometer"
                  size={24}
                  color={theme.colors.primary}
                />
              )}
            </TouchableOpacity>
          </Card.Content>
        </Card>
      </TouchableOpacity>
    );
  };

  return (
    <View style={styles.container}>
      {/* 搜索栏 */}
      <Searchbar
        placeholder="搜索节点..."
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={styles.searchBar}
      />

      {/* 代理组选择 */}
      <View style={styles.groupContainer}>
        <FlatList
          horizontal
          data={groups}
          keyExtractor={item => item.name}
          renderItem={({ item }) => (
            <Chip
              selected={currentGroup === item.name}
              onPress={() => selectGroup(item.name)}
              style={styles.groupChip}
            >
              {item.name}
            </Chip>
          )}
          showsHorizontalScrollIndicator={false}
        />
      </View>

      {/* 排序和过滤 */}
      <View style={styles.controlsContainer}>
        <SegmentedButtons
          value={sortType}
          onValueChange={(value) => setSortType(value as SortType)}
          buttons={[
            { value: 'default', label: '默认' },
            { value: 'delay', label: '延迟' },
            { value: 'name', label: '名称' },
          ]}
          style={styles.segmentedButtons}
        />
        <SegmentedButtons
          value={filterType}
          onValueChange={(value) => setFilterType(value as FilterType)}
          buttons={[
            { value: 'all', label: '全部' },
            { value: 'available', label: '可用' },
            { value: 'unavailable', label: '不可用' },
          ]}
          style={styles.segmentedButtons}
        />
      </View>

      {/* 代理列表 */}
      <FlatList
        data={filteredProxies}
        renderItem={renderProxyItem}
        keyExtractor={item => item.name}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Icon name="wifi-off" size={64} color="gray" />
            <Paragraph style={styles.emptyText}>暂无代理节点</Paragraph>
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
  groupContainer: {
    paddingHorizontal: 16,
    marginBottom: 12,
  },
  groupChip: {
    marginRight: 8,
  },
  controlsContainer: {
    paddingHorizontal: 16,
    marginBottom: 12,
    gap: 8,
  },
  segmentedButtons: {
    marginBottom: 8,
  },
  listContent: {
    padding: 16,
  },
  proxyCard: {
    marginBottom: 12,
    elevation: 2,
  },
  proxyContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  proxyInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  proxyDetails: {
    flex: 1,
    marginLeft: 8,
  },
  proxyName: {
    fontSize: 16,
    marginBottom: 4,
  },
  proxyMeta: {
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

