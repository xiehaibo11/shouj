import React, { useState, useEffect } from 'react';
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
  Text,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import useSWR from 'swr';

import { useVPN } from '../hooks/useVPN';
import { useTraffic } from '../hooks/useTraffic';
import { useProxies } from '../hooks/useProxies';
import { formatTraffic } from '../utils/format';
import { 
  testDelay, 
  getClashMode, 
  setClashMode, 
  getClashInfo,
  getIpInfo,
  closeAllConnections,
} from '../services/api';

export default function HomeScreen() {
  const theme = useTheme();
  const [refreshing, setRefreshing] = useState(false);
  const [testing, setTesting] = useState(false);
  const [clashMode, setClashModeState] = useState('rule');
  const [clashInfo, setClashInfo] = useState<any>(null);
  const [ipInfo, setIpInfo] = useState<any>(null);
  const [showIp, setShowIp] = useState(false);

  const { isConnected, toggle, loading } = useVPN();
  const { upload, download, uploadSpeed, downloadSpeed } = useTraffic();
  const { currentProxy, mutate: refreshProxies } = useProxies();

  useEffect(() => {
    loadClashMode();
    loadClashInfo();
    loadIpInfo();
  }, []);

  const loadClashMode = async () => {
    try {
      const mode = await getClashMode();
      setClashModeState(mode);
    } catch (error) {
      console.error('获取 Clash 模式失败:', error);
    }
  };

  const loadClashInfo = async () => {
    try {
      const info = await getClashInfo();
      setClashInfo(info);
    } catch (error) {
      console.error('获取 Clash 信息失败:', error);
    }
  };

  const loadIpInfo = async () => {
    try {
      const info = await getIpInfo();
      setIpInfo(info);
    } catch (error) {
      console.error('获取 IP 信息失败:', error);
    }
  };

  const handleModeChange = async (mode: string) => {
    try {
      await setClashMode(mode);
      setClashModeState(mode);
    } catch (error) {
      console.error('切换模式失败:', error);
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await Promise.all([
      refreshProxies(),
      loadClashInfo(),
      loadIpInfo(),
    ]);
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

  const handleCloseAllConnections = async () => {
    try {
      await closeAllConnections();
    } catch (error) {
      console.error('关闭所有连接失败:', error);
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

      {/* Clash 模式切换卡片 */}
      <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
        <Card.Content>
          <Title>Clash 模式</Title>
          <Divider style={styles.divider} />
          <View style={styles.modeContainer}>
            <TouchableOpacity
              style={[
                styles.modeButton,
                clashMode === 'rule' && { backgroundColor: theme.colors.primary },
              ]}
              onPress={() => handleModeChange('rule')}
            >
              <Icon
                name="filter-variant"
                size={24}
                color={clashMode === 'rule' ? 'white' : theme.colors.primary}
              />
              <Paragraph
                style={[
                  styles.modeText,
                  { color: clashMode === 'rule' ? 'white' : theme.colors.primary },
                ]}
              >
                规则
              </Paragraph>
            </TouchableOpacity>
            <TouchableOpacity
              style={[
                styles.modeButton,
                clashMode === 'global' && { backgroundColor: theme.colors.primary },
              ]}
              onPress={() => handleModeChange('global')}
            >
              <Icon
                name="earth"
                size={24}
                color={clashMode === 'global' ? 'white' : theme.colors.primary}
              />
              <Paragraph
                style={[
                  styles.modeText,
                  { color: clashMode === 'global' ? 'white' : theme.colors.primary },
                ]}
              >
                全局
              </Paragraph>
            </TouchableOpacity>
            <TouchableOpacity
              style={[
                styles.modeButton,
                clashMode === 'direct' && { backgroundColor: theme.colors.primary },
              ]}
              onPress={() => handleModeChange('direct')}
            >
              <Icon
                name="arrow-right-bold"
                size={24}
                color={clashMode === 'direct' ? 'white' : theme.colors.primary}
              />
              <Paragraph
                style={[
                  styles.modeText,
                  { color: clashMode === 'direct' ? 'white' : theme.colors.primary },
                ]}
              >
                直连
              </Paragraph>
            </TouchableOpacity>
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

      {/* Clash 核心信息 */}
      {clashInfo && (
        <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
          <Card.Content>
            <View style={styles.cardHeader}>
              <Title>Clash 核心信息</Title>
              <Icon name="information" size={24} color={theme.colors.primary} />
            </View>
            <Divider style={styles.divider} />
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>核心版本</Text>
              <Text style={styles.infoValue}>{clashInfo.version || '-'}</Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>混合端口</Text>
              <Text style={styles.infoValue}>{clashInfo.mixedPort || '-'}</Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>运行时间</Text>
              <Text style={styles.infoValue}>{clashInfo.uptime || '-'}</Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>规则数量</Text>
              <Text style={styles.infoValue}>{clashInfo.rulesCount || 0}</Text>
            </View>
          </Card.Content>
        </Card>
      )}

      {/* IP 信息 */}
      {ipInfo && (
        <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
          <Card.Content>
            <View style={styles.cardHeader}>
              <Title>IP 信息</Title>
              <TouchableOpacity onPress={() => setShowIp(!showIp)}>
                <Icon 
                  name={showIp ? 'eye-off' : 'eye'} 
                  size={24} 
                  color={theme.colors.primary} 
                />
              </TouchableOpacity>
            </View>
            <Divider style={styles.divider} />
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>IP 地址</Text>
              <Text style={styles.infoValue}>
                {showIp ? ipInfo.ip : '••••••••••'}
              </Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>国家</Text>
              <Text style={styles.infoValue}>{ipInfo.country || '-'}</Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>地区</Text>
              <Text style={styles.infoValue}>
                {[ipInfo.city, ipInfo.region].filter(Boolean).join(', ') || '-'}
              </Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>ISP</Text>
              <Text style={styles.infoValue}>{ipInfo.isp || '-'}</Text>
            </View>
          </Card.Content>
        </Card>
      )}

      {/* 快捷操作 */}
      <Card style={[styles.card, { backgroundColor: theme.colors.surface }]}>
        <Card.Content>
          <Title>快捷操作</Title>
          <Divider style={styles.divider} />
          <View style={styles.quickActions}>
            <TouchableOpacity style={styles.actionButton} onPress={onRefresh}>
              <Icon name="refresh" size={32} color={theme.colors.primary} />
              <Paragraph style={styles.actionLabel}>刷新配置</Paragraph>
            </TouchableOpacity>
            <TouchableOpacity style={styles.actionButton} onPress={handleTestDelay}>
              <Icon name="speedometer" size={32} color={theme.colors.primary} />
              <Paragraph style={styles.actionLabel}>测速</Paragraph>
            </TouchableOpacity>
            <TouchableOpacity 
              style={styles.actionButton} 
              onPress={handleCloseAllConnections}
            >
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
  modeContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingVertical: 8,
  },
  modeButton: {
    flex: 1,
    alignItems: 'center',
    padding: 12,
    marginHorizontal: 4,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#2196F3',
  },
  modeText: {
    marginTop: 4,
    fontSize: 12,
    fontWeight: '600',
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  infoLabel: {
    fontSize: 14,
    color: 'gray',
  },
  infoValue: {
    fontSize: 14,
    fontWeight: '600',
  },
});

