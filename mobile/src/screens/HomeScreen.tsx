import React, { useState } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  RefreshControl,
  TouchableOpacity,
} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Switch,
  Button,
  Chip,
  Divider,
  ActivityIndicator,
  useTheme,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import useSWR from 'swr';

import { useVPN } from '../hooks/useVPN';
import { useTraffic } from '../hooks/useTraffic';
import { useProxies } from '../hooks/useProxies';
import { formatTraffic } from '../utils/format';
import { testDelay } from '../services/api';

export default function HomeScreen() {
  const theme = useTheme();
  const [refreshing, setRefreshing] = useState(false);
  const [testing, setTesting] = useState(false);

  const { isConnected, toggle, loading } = useVPN();
  const { upload, download, uploadSpeed, downloadSpeed } = useTraffic();
  const { currentProxy, mutate: refreshProxies } = useProxies();

  const onRefresh = async () => {
    setRefreshing(true);
    await refreshProxies();
    setRefreshing(false);
  };

  const handleTestDelay = async () => {
    if (!currentProxy) return;
    
    setTesting(true);
    try {
      await testDelay(currentProxy.name);
      await refreshProxies();
    } catch (error) {
      console.error('测速失败:', error);
    } finally {
      setTesting(false);
    }
  };

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
    >
      {/* VPN 连接状态卡片 */}
      <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
        <Card.Content>
          <View style={styles.statusHeader}>
            <View style={styles.statusInfo}>
              <Icon
                name={isConnected ? 'shield-check' : 'shield-off'}
                size={32}
                color={isConnected ? theme.colors.primary : theme.colors.error}
              />
              <View style={styles.statusText}>
                <Title>
                  {isConnected ? '已连接' : '未连接'}
                </Title>
                <Paragraph>
                  {isConnected ? '代理已启用' : '点击开关启用代理'}
                </Paragraph>
              </View>
            </View>
            <Switch
              value={isConnected}
              onValueChange={toggle}
              disabled={loading}
            />
          </View>
        </Card.Content>
      </Card>

      {/* 当前节点卡片 */}
      {currentProxy && (
        <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
          <Card.Content>
            <View style={styles.cardHeader}>
              <Title>当前节点</Title>
              <Button
                mode="outlined"
                compact
                onPress={handleTestDelay}
                loading={testing}
                disabled={testing}
              >
                测速
              </Button>
            </View>
            <Divider style={styles.divider} />
            <TouchableOpacity style={styles.proxyInfo}>
              <View>
                <Paragraph style={styles.proxyName}>
                  {currentProxy.name}
                </Paragraph>
                <View style={styles.proxyMeta}>
                  <Chip
                    mode="outlined"
                    compact
                    style={styles.chip}
                    icon="dns"
                  >
                    {currentProxy.type}
                  </Chip>
                  {currentProxy.delay && (
                    <Chip
                      mode="outlined"
                      compact
                      style={[
                        styles.chip,
                        {
                          borderColor:
                            currentProxy.delay < 200
                              ? theme.colors.primary
                              : currentProxy.delay < 500
                              ? '#FF9800'
                              : theme.colors.error,
                        },
                      ]}
                      icon="speedometer"
                    >
                      {currentProxy.delay}ms
                    </Chip>
                  )}
                </View>
              </View>
              <Icon name="chevron-right" size={24} color={theme.colors.primary} />
            </TouchableOpacity>
          </Card.Content>
        </Card>
      )}

      {/* 流量统计卡片 */}
      <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
        <Card.Content>
          <Title>流量统计</Title>
          <Divider style={styles.divider} />
          <View style={styles.trafficContainer}>
            {/* 上传 */}
            <View style={styles.trafficItem}>
              <Icon name="arrow-up" size={24} color={theme.colors.primary} />
              <View style={styles.trafficInfo}>
                <Paragraph style={styles.trafficLabel}>上传</Paragraph>
                <Title style={styles.trafficValue}>
                  {formatTraffic(uploadSpeed)}/s
                </Title>
                <Paragraph style={styles.trafficTotal}>
                  总计: {formatTraffic(upload)}
                </Paragraph>
              </View>
            </View>

            <Divider style={styles.verticalDivider} />

            {/* 下载 */}
            <View style={styles.trafficItem}>
              <Icon name="arrow-down" size={24} color={theme.colors.secondary} />
              <View style={styles.trafficInfo}>
                <Paragraph style={styles.trafficLabel}>下载</Paragraph>
                <Title style={styles.trafficValue}>
                  {formatTraffic(downloadSpeed)}/s
                </Title>
                <Paragraph style={styles.trafficTotal}>
                  总计: {formatTraffic(download)}
                </Paragraph>
              </View>
            </View>
          </View>
        </Card.Content>
      </Card>

      {/* 快捷操作 */}
      <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
        <Card.Content>
          <Title>快捷操作</Title>
          <Divider style={styles.divider} />
          <View style={styles.quickActions}>
            <TouchableOpacity style={styles.actionButton}>
              <Icon name="refresh" size={32} color={theme.colors.primary} />
              <Paragraph style={styles.actionLabel}>刷新配置</Paragraph>
            </TouchableOpacity>
            <TouchableOpacity style={styles.actionButton}>
              <Icon name="speedometer" size={32} color={theme.colors.primary} />
              <Paragraph style={styles.actionLabel}>全部测速</Paragraph>
            </TouchableOpacity>
            <TouchableOpacity style={styles.actionButton}>
              <Icon name="close-network" size={32} color={theme.colors.error} />
              <Paragraph style={styles.actionLabel}>断开连接</Paragraph>
            </TouchableOpacity>
          </View>
        </Card.Content>
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  card: {
    marginBottom: 16,
    elevation: 2,
  },
  statusHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  statusInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  statusText: {
    marginLeft: 16,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  divider: {
    marginVertical: 12,
  },
  proxyInfo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  proxyName: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
  },
  proxyMeta: {
    flexDirection: 'row',
    gap: 8,
  },
  chip: {
    height: 28,
  },
  trafficContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  trafficItem: {
    flex: 1,
    alignItems: 'center',
  },
  trafficInfo: {
    alignItems: 'center',
    marginTop: 8,
  },
  trafficLabel: {
    fontSize: 12,
    color: 'gray',
  },
  trafficValue: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  trafficTotal: {
    fontSize: 12,
    color: 'gray',
    marginTop: 4,
  },
  verticalDivider: {
    width: 1,
    height: '100%',
  },
  quickActions: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  actionButton: {
    alignItems: 'center',
    padding: 8,
  },
  actionLabel: {
    marginTop: 8,
    fontSize: 12,
  },
});

