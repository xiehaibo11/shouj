import React, { useState } from 'react';
import { View, FlatList, StyleSheet } from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Chip,
  Searchbar,
  useTheme,
  SegmentedButtons,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

import { useRules } from '../hooks/useRules';

export default function RulesScreen() {
  const theme = useTheme();
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState('all');

  const { rules } = useRules();

  const filteredRules = rules.filter(rule => {
    if (filterType !== 'all' && rule.type !== filterType) {
      return false;
    }
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    return (
      rule.payload?.toLowerCase().includes(query) ||
      rule.proxy?.toLowerCase().includes(query)
    );
  });

  const renderRuleItem = ({ item }: { item: any }) => {
    return (
      <Card style={styles.ruleCard}>
        <Card.Content>
          <View style={styles.ruleHeader}>
            <Chip
              mode="outlined"
              compact
              style={styles.typeChip}
              icon={getRuleIcon(item.type)}
            >
              {item.type}
            </Chip>
            <Chip
              mode="flat"
              compact
              style={[styles.proxyChip, { backgroundColor: theme.colors.primary }]}
              textStyle={{ color: 'white' }}
            >
              {item.proxy}
            </Chip>
          </View>
          <Paragraph style={styles.rulePayload}>{item.payload}</Paragraph>
        </Card.Content>
      </Card>
    );
  };

  return (
    <View style={styles.container}>
      <Searchbar
        placeholder="搜索规则..."
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={styles.searchBar}
      />

      <View style={styles.filterContainer}>
        <SegmentedButtons
          value={filterType}
          onValueChange={setFilterType}
          buttons={[
            { value: 'all', label: '全部' },
            { value: 'DOMAIN', label: '域名' },
            { value: 'IP-CIDR', label: 'IP' },
            { value: 'GEOIP', label: 'GeoIP' },
          ]}
        />
      </View>

      <FlatList
        data={filteredRules}
        renderItem={renderRuleItem}
        keyExtractor={(item, index) => `${item.type}-${index}`}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Icon name="file-question" size={64} color="gray" />
            <Paragraph style={styles.emptyText}>暂无规则</Paragraph>
          </View>
        }
      />
    </View>
  );
}

function getRuleIcon(type: string): string {
  switch (type) {
    case 'DOMAIN':
    case 'DOMAIN-SUFFIX':
    case 'DOMAIN-KEYWORD':
      return 'web';
    case 'IP-CIDR':
    case 'IP-CIDR6':
      return 'ip';
    case 'GEOIP':
      return 'earth';
    case 'MATCH':
      return 'check-all';
    default:
      return 'help-circle';
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  searchBar: {
    margin: 16,
    elevation: 2,
  },
  filterContainer: {
    paddingHorizontal: 16,
    marginBottom: 16,
  },
  listContent: {
    padding: 16,
  },
  ruleCard: {
    marginBottom: 12,
    elevation: 2,
  },
  ruleHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  typeChip: {
    height: 28,
  },
  proxyChip: {
    height: 28,
  },
  rulePayload: {
    fontSize: 14,
    color: 'gray',
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

